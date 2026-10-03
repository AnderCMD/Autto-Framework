package io.github.andercmd.autto.core.observability;

import io.github.andercmd.autto.core.config.AuttoProperties;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Sends the run summary to a Slack, Microsoft Teams or generic JSON webhook. Never fails the run. */
public final class Notifier {

    private static final Logger LOG = LoggerFactory.getLogger(Notifier.class);

    private Notifier() {
    }

    /** Sends the summary when notifications are configured (and, if requested, only when something failed). */
    public static void send(AuttoProperties.Notifications config, RunMetrics metrics) {
        if (config.webhookUrl() == null || metrics.total() == 0) {
            return;
        }
        if (config.onlyOnFailure() && metrics.failed() == 0) {
            return;
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create(config.webhookUrl()))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload(config, metrics)))
                .build();
        try {
            HttpResponse<String> response = HttpClient.newHttpClient().send(request,
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                LOG.warn("Notification webhook answered {}", response.statusCode());
            } else {
                LOG.info("Run summary sent to the {} webhook", config.type());
            }
        } catch (IOException e) {
            LOG.warn("Notification could not be sent: {}", e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** The JSON body for the configured channel. */
    static String payload(AuttoProperties.Notifications config, RunMetrics metrics) {
        boolean ok = metrics.failed() == 0;
        String headline = (ok ? "✅ Autto run passed" : "❌ Autto run failed");
        String details = "%d scenarios · %d passed · %d failed · %d skipped".formatted(metrics.total(),
                metrics.passed(), metrics.failed(), metrics.skipped())
                + (metrics.recovered() > 0 ? " · " + metrics.recovered() + " flaky (passed on rerun)" : "");
        String link = config.reportUrl() == null ? "" : "\n" + config.reportUrl();
        return switch (config.type()) {
            case "teams" -> """
                    {"@type":"MessageCard","@context":"http://schema.org/extensions","summary":%s,\
                    "themeColor":"%s","title":%s,"text":%s}""".formatted(json(headline), ok ? "2EB67D" : "E01E5A",
                    json(headline), json(details + link));
            case "generic" -> metrics.toJson();
            default -> "{\"text\":" + json(headline + "\n" + details + link) + "}";
        };
    }

    static String json(String value) {
        StringBuilder out = new StringBuilder("\"");
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.append('"').toString();
    }
}
