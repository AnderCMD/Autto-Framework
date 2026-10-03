package io.github.andercmd.autto.core.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import io.github.andercmd.autto.core.config.AuttoSettings;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MailboxTest {

    private HttpServer server;

    @BeforeEach
    void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/search", exchange -> respond(exchange, """
                {"messages":[{"ID":"m1","Subject":"Reset your password"},{"ID":"m2","Subject":"Hi"}]}"""));
        server.createContext("/api/v1/message/m1", exchange -> respond(exchange, """
                {"Subject":"Reset your password","Text":"Your code is 482913. It expires soon.","HTML":"<p/>"}"""));
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private static void respond(com.sun.net.httpserver.HttpExchange exchange, String json) throws java.io.IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    private Mailbox mailbox(String timeout) {
        Properties system = new Properties();
        system.setProperty("user.dir", System.getProperty("java.io.tmpdir"));
        system.setProperty("autto.mail.url", "http://127.0.0.1:" + server.getAddress().getPort());
        system.setProperty("autto.mail.timeout", timeout);
        return new Mailbox(AuttoSettings.load(Map.of(), system));
    }

    @Test
    void findsTheMessageAndExtractsTheCode() {
        Mailbox.Mail mail = mailbox("5s").waitFor("user@example.test", "Reset");

        assertThat(mail.subject()).isEqualTo("Reset your password");
        assertThat(mail.find("code is (\\d{6})")).contains("482913");
        assertThat(mail.find("nothing here")).isEmpty();
    }

    @Test
    void timesOutWhenNoMessageMatches() {
        assertThatThrownBy(() -> mailbox("1s").waitFor("user@example.test", "Invoice"))
                .isInstanceOf(AssertionError.class).hasMessageContaining("Invoice");
    }
}
