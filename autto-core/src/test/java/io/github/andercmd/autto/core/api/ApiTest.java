package io.github.andercmd.autto.core.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import io.github.andercmd.autto.core.config.AuttoSettings;
import io.restassured.response.Response;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ApiTest {

    private HttpServer server;
    private final AtomicReference<String> receivedApiKey = new AtomicReference<>();

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/orders/42", exchange -> {
            receivedApiKey.set(exchange.getRequestHeaders().getFirst("X-Api-Key"));
            byte[] body = "{\"id\":42,\"status\":\"PAID\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private Api api() {
        Properties system = new Properties();
        system.setProperty("user.dir", System.getProperty("java.io.tmpdir"));
        system.setProperty("autto.api.base-url", "http://127.0.0.1:" + server.getAddress().getPort());
        return new Api(AuttoSettings.load(Map.of(), system));
    }

    @Test
    void appliesBaseUrlAndDefaultHeaders() {
        Response response = api().request().get("/orders/42");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getString("status")).isEqualTo("PAID");
        assertThat(receivedApiKey).hasValue("unit-test-api-key");
    }

    @Test
    void sensitiveHeadersAreMaskedInTheReport() {
        assertThat(ApiReportFilter.isSensitive("Authorization")).isTrue();
        assertThat(ApiReportFilter.isSensitive("x-api-key")).isTrue();
        assertThat(ApiReportFilter.isSensitive("X-Session-Token")).isTrue();
        assertThat(ApiReportFilter.isSensitive("Content-Type")).isFalse();
    }
}
