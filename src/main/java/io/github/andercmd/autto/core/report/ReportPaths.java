package io.github.andercmd.autto.core.report;

import io.github.andercmd.autto.core.config.AuttoConfig;
import io.github.andercmd.autto.core.config.ConfigKeys;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * Folder layout of a test run report.
 *
 * <pre>
 * target/autto-reports/            (report.dir, optionally with a timestamp sub-folder)
 * ├── index.html                   Extent Spark report (every test)
 * ├── failed.html                  Extent Spark report filtered to failures
 * ├── extent.json                  Raw Extent data, used to merge reports of parallel CI jobs
 * ├── screenshots/                 PNG evidence
 * ├── videos/                      MP4 recordings
 * ├── attachments/                 Any other file attached to a scenario
 * └── logs/autto.log               Execution log
 * </pre>
 */
public final class ReportPaths {

    /** Kinds of evidence, each stored in its own folder. */
    public enum Kind {
        SCREENSHOT("screenshots"),
        VIDEO("videos"),
        ATTACHMENT("attachments");

        private final String folder;

        Kind(String folder) {
            this.folder = folder;
        }

        public String folder() {
            return folder;
        }
    }

    private static volatile ReportPaths instance;

    private final Path root;
    private final AtomicInteger counter = new AtomicInteger();

    ReportPaths(Path root) {
        this.root = root.toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.root);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to create report folder " + this.root, e);
        }
    }

    public static ReportPaths get() {
        ReportPaths local = instance;
        if (local == null) {
            synchronized (ReportPaths.class) {
                local = instance;
                if (local == null) {
                    AuttoConfig config = AuttoConfig.get();
                    Path base = Path.of(config.get(ConfigKeys.REPORT_DIR, "target/autto-reports"));
                    boolean timestamped = config.getBoolean(ConfigKeys.REPORT_TIMESTAMPED, false);
                    if (timestamped) {
                        base = base.resolve(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")));
                    }
                    local = new ReportPaths(base);
                    if (!timestamped) {
                        local.cleanEvidence();
                    }
                    instance = local;
                }
            }
        }
        return local;
    }

    /** Removes evidence left by a previous run in the same folder, so the report never links stale files. */
    void cleanEvidence() {
        for (Kind kind : Kind.values()) {
            Path folder = root.resolve(kind.folder());
            if (!Files.isDirectory(folder)) {
                continue;
            }
            try (Stream<Path> files = Files.walk(folder)) {
                files.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
            } catch (IOException e) {
                throw new UncheckedIOException("Unable to clean " + folder, e);
            }
        }
    }

    public Path root() {
        return root;
    }

    public Path resolve(String relative) {
        return root.resolve(relative);
    }

    /** Reserves a unique, relative (to {@link #root()}) path for a new evidence file. */
    public String newRelativePath(Kind kind, String name, String extension) {
        String fileName = String.format(Locale.ROOT, "%04d-%s.%s", counter.incrementAndGet(), slug(name), extension);
        return kind.folder() + "/" + fileName;
    }

    /** Writes {@code data} into a new evidence file and returns its path relative to {@link #root()}. */
    public String store(Kind kind, String name, String extension, byte[] data) {
        String relative = newRelativePath(kind, name, extension);
        Path file = resolve(relative);
        try {
            Files.createDirectories(file.getParent());
            Files.write(file, data);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to write evidence " + file, e);
        }
        return relative;
    }

    static String slug(String value) {
        String ascii = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String slug = ascii.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        if (slug.isEmpty()) {
            return "evidence";
        }
        return slug.length() > 60 ? slug.substring(0, 60).replaceAll("-$", "") : slug;
    }
}
