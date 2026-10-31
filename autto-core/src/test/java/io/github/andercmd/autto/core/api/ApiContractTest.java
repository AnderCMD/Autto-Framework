package io.github.andercmd.autto.core.api;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ApiContractTest {

    private HttpServer server;

    @BeforeEach
    void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/ok", exchange -> send(exchange, "{\"id\":42,\"status\":\"PAID\"}"));
        server.createContext("/bad", exchange -> send(exchange, "{\"id\":\"forty-two\"}"));
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private static void send(com.sun.net.httpserver.HttpExchange exchange, String json) throws java.io.IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    private Response get(String path) {
        return RestAssured.get("http://127.0.0.1:" + server.getAddress().getPort() + path);
    }

    @Test
    void conformingResponsePasses() {
        assertThatCode(() -> ApiContract.assertMatches(get("/ok"), "order.json")).doesNotThrowAnyException();
    }

    @Test
    void violationsFailTheContract() {
        assertThatThrownBy(() -> ApiContract.assertMatches(get("/bad"), "order.json"))
                .isInstanceOf(AssertionError.class);
    }
}
