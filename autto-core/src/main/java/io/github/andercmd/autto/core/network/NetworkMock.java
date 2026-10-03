package io.github.andercmd.autto.core.network;

import io.github.andercmd.autto.core.driver.DriverManager;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import org.openqa.selenium.devtools.NetworkInterceptor;
import org.openqa.selenium.remote.http.Contents;
import org.openqa.selenium.remote.http.HttpRequest;
import org.openqa.selenium.remote.http.HttpResponse;
import org.openqa.selenium.remote.http.Routable;
import org.openqa.selenium.remote.http.Route;

/**
 * Intercepts the browser's network traffic to simulate backend failures, slow responses or blocked third parties,
 * without touching the application under test. Works on Chromium browsers (Chrome, Edge), also on Selenium Grid
 * (it uses the DevTools protocol). Scenario-scoped: every rule is removed when the scenario ends.
 *
 * <pre>{@code
 * network.stub("/api/cart", 500, "{\"error\":\"boom\"}");   // the UI must show an error banner
 * network.block("googletagmanager.com");                     // third parties never slow a test down
 * network.delay("/api/products", Duration.ofSeconds(3));     // loading indicators
 * }</pre>
 */
public class NetworkMock implements AutoCloseable {

    private final List<Rule> rules = new ArrayList<>();
    private NetworkInterceptor interceptor;

    /** Answers every request whose URL contains {@code urlPart} with the given status and JSON body. */
    public void stub(String urlPart, int status, String jsonBody) {
        stub(urlPart, status, "application/json", jsonBody);
    }

    public void stub(String urlPart, int status, String contentType, String body) {
        add(new Rule(request -> request.getUri().contains(urlPart), () -> new HttpResponse().setStatus(status)
                .addHeader("Content-Type", contentType).setContent(Contents.utf8String(body))));
    }

    /** Fails every request whose URL contains {@code urlPart}, as if the host were unreachable. */
    public void block(String urlPart) {
        stub(urlPart, 503, "text/plain", "blocked by Autto");
    }

    /** Answers with the given status and an empty body after {@code delay}. */
    public void delay(String urlPart, Duration delay) {
        add(new Rule(request -> request.getUri().contains(urlPart), () -> {
            sleep(delay);
            return new HttpResponse().setStatus(200).setContent(Contents.utf8String(""));
        }));
    }

    /** Removes every rule and stops intercepting. */
    @Override
    public void close() {
        rules.clear();
        stopInterceptor();
    }

    private void add(Rule rule) {
        rules.add(rule);
        stopInterceptor();
        List<Routable> routes = rules.stream()
                .<Routable>map(r -> Route.matching(r.matches()).to(() -> request -> r.response().get()))
                .toList();
        interceptor = new NetworkInterceptor(DriverManager.driver(), Route.combine(routes));
    }

    private void stopInterceptor() {
        if (interceptor != null) {
            interceptor.close();
            interceptor = null;
        }
    }

    private static void sleep(Duration delay) {
        try {
            java.util.concurrent.TimeUnit.MILLISECONDS.sleep(delay.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private record Rule(Predicate<HttpRequest> matches, java.util.function.Supplier<HttpResponse> response) {
    }
}
