package io.github.andercmd.autto.core.observability;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.andercmd.autto.core.config.AuttoProperties;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class RunMetricsTest {

    private static RunMetrics sample() {
        RunMetrics metrics = new RunMetrics();
        metrics.scenarioFinished(RunMetrics.Outcome.PASSED, Duration.ofSeconds(2), false);
        metrics.scenarioFinished(RunMetrics.Outcome.PASSED, Duration.ofSeconds(4), true);
        metrics.scenarioFinished(RunMetrics.Outcome.FAILED, Duration.ofMillis(500), false);
        metrics.scenarioFinished(RunMetrics.Outcome.SKIPPED, Duration.ZERO, false);
        metrics.browserStartRetried();
        return metrics;
    }

    @Test
    void countsScenariosAndFlakyRecoveries() {
        RunMetrics metrics = sample();

        assertThat(metrics.total()).isEqualTo(4);
        assertThat(metrics.passed()).isEqualTo(2);
        assertThat(metrics.failed()).isEqualTo(1);
        assertThat(metrics.recovered()).isEqualTo(1);
        assertThat(metrics.browserStartRetries()).isEqualTo(1);
        assertThat(metrics.totalDuration()).isEqualTo(Duration.ofMillis(6500));
    }

    @Test
    void rendersJsonAndPrometheus() {
        RunMetrics metrics = sample();

        assertThat(metrics.toJson()).contains("\"scenarios\": 4", "\"recovered_on_rerun\": 1",
                "\"scenario_seconds_max\": 4.000");
        assertThat(metrics.toPrometheus()).contains("autto_scenarios_total{result=\"failed\"} 1",
                "autto_scenarios_recovered_total 1");
    }

    @Test
    void slackPayloadReportsFailuresAndLink() {
        AuttoProperties.Notifications config =
                new AuttoProperties.Notifications("https://hooks.test/x", "slack", false, "https://ci.test/run/1");

        String payload = Notifier.payload(config, sample());

        assertThat(payload).startsWith("{\"text\":\"❌ Autto run failed").contains("4 scenarios", "1 flaky",
                "https://ci.test/run/1");
    }

    @Test
    void teamsPayloadIsAMessageCard() {
        AuttoProperties.Notifications config = new AuttoProperties.Notifications("https://x", "teams", null, null);

        assertThat(Notifier.payload(config, sample())).contains("\"@type\":\"MessageCard\"", "E01E5A");
    }

    @Test
    void invalidTypeIsRejected() {
        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> new AuttoProperties.Notifications("https://x", "carrier-pigeon", null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void jsonEscapesControlCharacters() {
        assertThat(Notifier.json("a\"b\n\\")).isEqualTo("\"a\\\"b\\n\\\\\"");
    }

    @Test
    void sendPostsTheSummaryToTheWebhook() throws Exception {
        com.sun.net.httpserver.HttpServer server =
                com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1", 0), 0);
        java.util.concurrent.atomic.AtomicReference<String> body = new java.util.concurrent.atomic.AtomicReference<>();
        server.createContext("/hook", exchange -> {
            body.set(new String(exchange.getRequestBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();
        try {
            String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/hook";

            Notifier.send(new AuttoProperties.Notifications(url, "slack", true, null), new RunMetrics());
            assertThat(body.get()).as("nothing ran: nothing sent").isNull();

            Notifier.send(new AuttoProperties.Notifications(url, "slack", true, null), sample());
            assertThat(body.get()).contains("Autto run failed");

            body.set(null);
            RunMetrics green = new RunMetrics();
            green.scenarioFinished(RunMetrics.Outcome.PASSED, Duration.ofSeconds(1), false);
            Notifier.send(new AuttoProperties.Notifications(url, "slack", true, null), green);
            assertThat(body.get()).as("only-on-failure").isNull();
        } finally {
            server.stop(0);
        }
    }
}
