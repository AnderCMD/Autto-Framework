package io.github.andercmd.autto.core.mail;

import io.github.andercmd.autto.core.config.AuttoProperties;
import io.github.andercmd.autto.core.config.AuttoSettings;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Client of a test mail server (Mailpit, see {@code docker-compose.yml}, profile {@code mail}) to verify the e-mails
 * of the application under test: registration links, password resets, one-time codes.
 *
 * <pre>{@code
 * Mail mail = mailbox.waitFor(email, "Reset your password");
 * String code = mail.find("\\b(\\d{6})\\b").orElseThrow();
 * }</pre>
 *
 * <p>Point the application under test to the SMTP port of the server (1025 by default) and set
 * {@code autto.mail.url} to its HTTP API (8025 by default).
 */
public class Mailbox {

    private static final Duration POLL = Duration.ofMillis(500);

    private final AuttoProperties.Mail config;

    public Mailbox(AuttoSettings settings) {
        this.config = settings.properties().mail();
    }

    /** Waits for a message to {@code recipient} whose subject contains {@code subject}. */
    public Mail waitFor(String recipient, String subject) {
        Instant deadline = Instant.now().plus(config.timeout());
        do {
            Optional<Mail> mail = search(recipient, subject);
            if (mail.isPresent()) {
                return mail.get();
            }
            pause();
        } while (Instant.now().isBefore(deadline));
        throw new AssertionError("No e-mail to '" + recipient + "' with subject containing '" + subject
                + "' arrived within " + config.timeout());
    }

    /** The newest message to {@code recipient} with a subject containing {@code subject}, if any. */
    public Optional<Mail> search(String recipient, String subject) {
        JsonPath list = RestAssured.given().get(config.url() + "/api/v1/search?query=" + encode(
                "to:\"" + recipient + "\"")).jsonPath();
        List<String> ids = list.getList("messages.findAll { it.Subject.contains('" + escape(subject) + "') }.ID");
        if (ids == null || ids.isEmpty()) {
            return Optional.empty();
        }
        JsonPath message = RestAssured.given().get(config.url() + "/api/v1/message/" + ids.getFirst()).jsonPath();
        return Optional.of(new Mail(message.getString("Subject"), message.getString("Text"),
                message.getString("HTML")));
    }

    /** Deletes every message (call it before the scenario to avoid reading a previous run's mail). */
    public void clear() {
        RestAssured.given().delete(config.url() + "/api/v1/messages");
    }

    private static void pause() {
        try {
            TimeUnit.MILLISECONDS.sleep(POLL.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for an e-mail", e);
        }
    }

    private static String encode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("'", "\\'");
    }

    /** A received message. */
    public record Mail(String subject, String text, String html) {

        /** First capture group (or the whole match) of {@code regex} in the plain text body. */
        public Optional<String> find(String regex) {
            Matcher matcher = Pattern.compile(regex).matcher(text == null ? "" : text);
            if (!matcher.find()) {
                return Optional.empty();
            }
            return Optional.of(matcher.groupCount() > 0 ? matcher.group(1) : matcher.group());
        }
    }
}
