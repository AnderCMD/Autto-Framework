package io.github.andercmd.autto.core.doctor;

import io.github.andercmd.autto.core.config.AuttoSettings;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Environment self-check: tells in seconds why a machine or a CI runner cannot run the suite (JDK, configuration,
 * browsers, Docker, application reachability, secrets).
 *
 * <pre>
 * ./scripts/doctor.sh
 * </pre>
 */
public final class Doctor {

    private Doctor() {
    }

    public static void main(String[] args) {
        PrintStream out = new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8);
        List<Check> results = run();
        for (Check check : results) {
            out.println(check.render());
        }
        long failures = results.stream().filter(c -> c.status() == Status.FAIL).count();
        out.println();
        out.println(failures == 0 ? "Everything needed to run the suite is in place."
                : failures + " problem(s) found: fix the lines marked with ✘.");
        System.exit(failures == 0 ? 0 : 1);
    }

    /** Runs every check. */
    public static List<Check> run() {
        List<Check> checks = new ArrayList<>();
        checks.add(java());
        AuttoSettings settings = null;
        try {
            settings = AuttoSettings.get();
            checks.add(new Check(Status.OK, "Configuration", "profiles " + settings.activeProfiles()));
        } catch (RuntimeException e) {
            checks.add(new Check(Status.FAIL, "Configuration", e.getMessage()));
        }
        if (settings != null) {
            checks.add(dotEnv(settings));
            checks.add(baseUrl(settings));
            checks.add(reportDir(settings));
        }
        checks.add(binary("Google Chrome", "google-chrome", "chrome", "chromium", "chromium-browser"));
        checks.add(binary("Firefox", "firefox"));
        checks.add(binary("Microsoft Edge", "microsoft-edge", "msedge"));
        checks.add(docker());
        return checks;
    }

    static Check java() {
        int feature = Runtime.version().feature();
        return feature >= 21
                ? new Check(Status.OK, "Java", "Java " + feature)
                : new Check(Status.FAIL, "Java", "Java " + feature + " found, Java 21 or newer is required");
    }

    private static Check dotEnv(AuttoSettings settings) {
        return settings.dotEnvFile()
                .map(path -> new Check(Status.OK, ".env file", path.toString()))
                .orElseGet(() -> new Check(Status.WARN, ".env file",
                        "not found: copy .env.example to .env (or define the secrets as environment variables)"));
    }

    private static Check baseUrl(AuttoSettings settings) {
        String url = settings.properties().baseUrl();
        if (url == null) {
            return new Check(Status.WARN, "Application", "autto.base-url is not configured");
        }
        try {
            HttpResponse<Void> response = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
                    .followRedirects(HttpClient.Redirect.NORMAL).build()
                    .send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10)).GET().build(),
                            HttpResponse.BodyHandlers.discarding());
            return new Check(response.statusCode() < 500 ? Status.OK : Status.FAIL, "Application",
                    url + " answered " + response.statusCode());
        } catch (java.io.IOException | IllegalArgumentException e) {
            return new Check(Status.FAIL, "Application", url + " is not reachable: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new Check(Status.FAIL, "Application", "interrupted");
        }
    }

    private static Check reportDir(AuttoSettings settings) {
        Path dir = Path.of(settings.properties().report().dir());
        try {
            Files.createDirectories(dir);
            return Files.isWritable(dir) ? new Check(Status.OK, "Report folder", dir.toAbsolutePath().toString())
                    : new Check(Status.FAIL, "Report folder", dir + " is not writable");
        } catch (java.io.IOException e) {
            return new Check(Status.FAIL, "Report folder", e.getMessage());
        }
    }

    private static Check binary(String name, String... candidates) {
        List<String> common = new ArrayList<>(List.of(candidates));
        if (os().contains("mac")) {
            common.add("/Applications/Google Chrome.app/Contents/MacOS/Google Chrome");
            common.add("/Applications/Firefox.app/Contents/MacOS/firefox");
            common.add("/Applications/Microsoft Edge.app/Contents/MacOS/Microsoft Edge");
        }
        for (String candidate : common) {
            if (matches(name, candidate) && (found(candidate) || Files.isExecutable(Path.of(candidate)))) {
                return new Check(Status.OK, name, candidate);
            }
        }
        return new Check(Status.WARN, name, "not found (autto.driver.docker-fallback can start it in Docker)");
    }

    private static boolean matches(String name, String candidate) {
        String lower = candidate.toLowerCase(Locale.ROOT);
        return switch (name) {
            case "Google Chrome" -> lower.contains("chrom");
            case "Firefox" -> lower.contains("firefox");
            default -> lower.contains("edge");
        };
    }

    private static Check docker() {
        return run(() -> found("docker") && exitCode("docker", "info") == 0)
                ? new Check(Status.OK, "Docker", "daemon reachable")
                : new Check(Status.WARN, "Docker", "not available (only needed by the docker target and fallback)");
    }

    private static boolean run(Supplier<Boolean> action) {
        try {
            return action.get();
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static boolean found(String command) {
        String lookup = os().contains("win") ? "where" : "which";
        return exitCode(lookup, command) == 0;
    }

    private static int exitCode(String... command) {
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
            if (!process.waitFor(10, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return -1;
            }
            return process.exitValue();
        } catch (java.io.IOException e) {
            return -1;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return -1;
        }
    }

    private static String os() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
    }

    /** Result severity. */
    public enum Status {
        OK("✔"), WARN("!"), FAIL("✘");

        private final String symbol;

        Status(String symbol) {
            this.symbol = symbol;
        }
    }

    /** One line of the report. */
    public record Check(Status status, String name, String detail) {

        String render() {
            return " %s %-14s %s".formatted(status.symbol, name, detail);
        }
    }
}
