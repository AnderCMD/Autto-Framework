package io.github.andercmd.autto.core.network;

import io.github.andercmd.autto.core.config.AuttoSettings;
import io.github.andercmd.autto.core.config.ExecutionTarget;
import io.github.andercmd.autto.core.driver.DriverManager;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.bidi.module.Network;
import org.openqa.selenium.bidi.network.AddInterceptParameters;
import org.openqa.selenium.bidi.network.BeforeRequestSent;
import org.openqa.selenium.bidi.network.BytesValue;
import org.openqa.selenium.bidi.network.ContinueRequestParameters;
import org.openqa.selenium.bidi.network.Header;
import org.openqa.selenium.bidi.network.InterceptPhase;
import org.openqa.selenium.bidi.network.ProvideResponseParameters;
import org.openqa.selenium.devtools.NetworkInterceptor;
import org.openqa.selenium.remote.http.Contents;
import org.openqa.selenium.remote.http.HttpResponse;
import org.openqa.selenium.remote.http.Routable;
import org.openqa.selenium.remote.http.Route;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Intercepts the browser's network traffic to simulate backend failures, slow responses or blocked third parties,
 * without touching the application under test.
 *
 * <p>Chromium browsers (Chrome, Edge) use the DevTools protocol through Selenium's {@code NetworkInterceptor}, which
 * needs the DevTools bindings matching the browser version (Selenium ships the latest ones; a very old or very new
 * browser falls back to WebDriver BiDi). Firefox uses WebDriver BiDi, which needs
 * {@code autto.browser.console-logs: true} (the default). Both work locally and on Selenium Grid.
 *
 * <p>Scenario-scoped: every rule is removed when the scenario ends.
 *
 * <pre>{@code
 * network.stub("/api/cart", 500, "{\"error\":\"boom\"}");   // the UI must show an error banner
 * network.block("googletagmanager.com");                     // third parties never slow a test down
 * network.delay("/api/products", Duration.ofSeconds(3));     // loading indicators
 * }</pre>
 */
public class NetworkMock implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(NetworkMock.class);
    private static final ExecutorService WORKERS = Executors.newCachedThreadPool(task -> {
        Thread thread = new Thread(task, "autto-network-mock");
        thread.setDaemon(true);
        return thread;
    });

    private static final java.util.Set<String> RESTRICTED_HEADERS =
            java.util.Set.of("host", "content-length", "connection", "upgrade", "expect", "accept-encoding");

    private final List<Rule> rules = new CopyOnWriteArrayList<>();
    private Network network;
    private String interceptId;
    private NetworkInterceptor cdp;

    /** Answers every request whose URL contains {@code urlPart} with the given status and JSON body. */
    public void stub(String urlPart, int status, String jsonBody) {
        stub(urlPart, status, "application/json", jsonBody);
    }

    public void stub(String urlPart, int status, String contentType, String body) {
        add(new Rule(urlPart, Action.respond(status, contentType, body)));
    }

    /** Fails every request whose URL contains {@code urlPart}, as if the host were unreachable. */
    public void block(String urlPart) {
        add(new Rule(urlPart, Action.fail()));
    }

    /** Lets the matching requests through, {@code delay} later (slow backends, loading indicators). */
    public void delay(String urlPart, Duration delay) {
        add(new Rule(urlPart, Action.delay(delay)));
    }

    /** Removes every rule and stops intercepting. */
    @Override
    public void close() {
        rules.clear();
        stopCdp();
        if (network == null) {
            return;
        }
        try {
            if (interceptId != null) {
                network.removeIntercept(interceptId);
            }
            network.close();
        } catch (RuntimeException e) {
            LOG.debug("Network interception did not stop cleanly: {}", e.getMessage());
        } finally {
            network = null;
            interceptId = null;
        }
    }

    /** What to do with a request: the first rule whose text is contained in the URL wins. */
    static Optional<Action> decide(List<Rule> rules, String url) {
        return rules.stream().filter(rule -> url.contains(rule.urlPart())).map(Rule::action).findFirst();
    }

    private void stopCdp() {
        if (cdp != null) {
            try {
                cdp.close();
            } catch (RuntimeException e) {
                LOG.debug("DevTools interception did not stop cleanly: {}", e.getMessage());
            }
            cdp = null;
        }
    }

    /** DevTools engine: one interceptor answering from the current rules (rebuilt whenever a rule is added). */
    private boolean startCdp(WebDriver driver) {
        try {
            Routable routes = Route.matching(request -> decide(rules, request.getUri()).isPresent())
                    .to(() -> request -> respond(decide(rules, request.getUri()).orElseThrow(), request));
            stopCdp();
            cdp = new NetworkInterceptor(driver, routes);
            return true;
        } catch (RuntimeException e) {
            LOG.debug("DevTools interception unavailable ({}), trying WebDriver BiDi", e.getMessage());
            cdp = null;
            return false;
        }
    }

    private static HttpResponse respond(Action action, org.openqa.selenium.remote.http.HttpRequest request) {
        if (action instanceof Action.Respond respond) {
            return new HttpResponse().setStatus(respond.status()).addHeader("Content-Type", respond.contentType())
                    .setContent(Contents.utf8String(respond.body()));
        }
        if (action instanceof Action.Delay delay) {
            pause(delay.duration());
            return forward(request);
        }
        return new HttpResponse().setStatus(503).addHeader("Content-Type", "text/plain")
                .setContent(Contents.utf8String("blocked by Autto"));
    }

    /** Sends the original request to the network (after a delay) and relays the answer to the browser. */
    private static HttpResponse forward(org.openqa.selenium.remote.http.HttpRequest request) {
        try {
            byte[] body = request.getContent().get().readAllBytes();
            java.net.http.HttpRequest.Builder builder = java.net.http.HttpRequest.newBuilder(
                    java.net.URI.create(request.getUri()));
            request.forEachHeader((name, value) -> {
                if (!RESTRICTED_HEADERS.contains(name.toLowerCase(java.util.Locale.ROOT))) {
                    builder.header(name, value);
                }
            });
            builder.method(request.getMethod().name(), java.net.http.HttpRequest.BodyPublishers.ofByteArray(body));
            java.net.http.HttpResponse<byte[]> answer = java.net.http.HttpClient.newHttpClient().send(builder.build(),
                    java.net.http.HttpResponse.BodyHandlers.ofByteArray());
            HttpResponse response = new HttpResponse().setStatus(answer.statusCode());
            answer.headers().map().forEach((name, values) -> {
                if (!name.startsWith(":") && !"content-encoding".equalsIgnoreCase(name)
                        && !"transfer-encoding".equalsIgnoreCase(name)) {
                    values.forEach(value -> response.addHeader(name, value));
                }
            });
            return response.setContent(Contents.bytes(answer.body()));
        } catch (java.io.IOException | IllegalArgumentException e) {
            return new HttpResponse().setStatus(502).setContent(Contents.utf8String(String.valueOf(e.getMessage())));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new HttpResponse().setStatus(502);
        }
    }

    /** The Docker target publishes only the WebDriver port: DevTools / BiDi sockets are unreachable from the host. */
    static void requireSupportedTarget(ExecutionTarget target) {
        if (target == ExecutionTarget.DOCKER) {
            throw new IllegalStateException("NetworkMock is not supported with autto.execution.target=docker: the "
                    + "browser's DevTools socket is not reachable from the host. Use the local target or a Selenium "
                    + "Grid (SE_NODE_GRID_URL set), and exclude @network scenarios from Docker runs");
        }
    }

    private synchronized void add(Rule rule) {
        requireSupportedTarget(AuttoSettings.get().properties().execution().target());
        rules.add(rule);
        WebDriver driver = DriverManager.driver();
        if (network == null && startCdp(driver)) {
            return;
        }
        if (network != null) {
            return;
        }
        try {
            network = new Network(driver);
        } catch (RuntimeException e) {
            rules.clear();
            throw new IllegalStateException("Network mocking needs a WebDriver BiDi session: keep "
                    + "autto.browser.console-logs enabled and use Chrome, Edge or Firefox", e);
        }
        interceptId = network.addIntercept(new AddInterceptParameters(InterceptPhase.BEFORE_REQUEST_SENT));
        network.onBeforeRequestSent(this::handle);
    }

    /**
     * Events arrive on the WebSocket reader thread, which must stay free to receive the answers to the commands
     * sent while handling them: each request is therefore handled on a worker thread.
     */
    private void handle(BeforeRequestSent event) {
        if (event.isBlocked()) {
            WORKERS.execute(() -> process(event));
        }
    }

    private void process(BeforeRequestSent event) {
        String id = event.getRequest().getRequestId();
        Network module = network;
        if (module == null) {
            return;
        }
        try {
            Optional<Action> action = decide(rules, event.getRequest().getUrl());
            if (action.isEmpty()) {
                module.continueRequest(new ContinueRequestParameters(id));
            } else if (action.get() instanceof Action.Fail) {
                module.failRequest(id);
            } else if (action.get() instanceof Action.Respond respond) {
                module.provideResponse(new ProvideResponseParameters(id).statusCode(respond.status())
                        .headers(List.of(new Header("Content-Type",
                                new BytesValue(BytesValue.Type.STRING, respond.contentType()))))
                        .body(new BytesValue(BytesValue.Type.STRING, respond.body())));
            } else if (action.get() instanceof Action.Delay delay) {
                pause(delay.duration());
                release(module, id);
            }
        } catch (RuntimeException e) {
            LOG.debug("Request {} could not be intercepted: {}", id, e.getMessage());
        }
    }

    private static void release(Network module, String id) {
        try {
            module.continueRequest(new ContinueRequestParameters(id));
        } catch (RuntimeException e) {
            LOG.debug("Delayed request {} could not be released: {}", id, e.getMessage());
        }
    }

    private static void pause(Duration delay) {
        try {
            TimeUnit.MILLISECONDS.sleep(delay.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** A rule: requests whose URL contains the text get the action. */
    record Rule(String urlPart, Action action) {
    }

    /** How a matching request is handled. */
    sealed interface Action {

        static Action respond(int status, String contentType, String body) {
            return new Respond(status, contentType, body);
        }

        static Action fail() {
            return new Fail();
        }

        static Action delay(Duration duration) {
            return new Delay(duration);
        }

        /** Answer with a canned response. */
        record Respond(int status, String contentType, String body) implements Action {
        }

        /** Fail the request. */
        record Fail() implements Action {
        }

        /** Let the request through later. */
        record Delay(Duration duration) implements Action {
        }
    }

    List<Rule> rules() {
        return new ArrayList<>(rules);
    }
}
