package io.github.andercmd.autto.core.media;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import javax.imageio.ImageIO;
import org.jcodec.api.awt.AWTSequenceEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Browser-agnostic screen recorder.
 *
 * <p>It periodically grabs screenshots through WebDriver (so it works for local browsers, headless browsers, Selenium
 * Grid, cloud providers and Appium alike) and encodes them into an H.264 MP4 file with JCodec, a pure Java encoder,
 * so no native tool such as ffmpeg is required.
 *
 * <p>The encoded video keeps real-time pacing: when a screenshot takes longer than the frame interval, the previous
 * frame is repeated. Only the last {@code maxDuration} of the scenario is kept in memory.
 */
public final class VideoRecorder {

    private static final Logger LOG = LoggerFactory.getLogger(VideoRecorder.class);
    private static final AtomicInteger THREAD_COUNTER = new AtomicInteger();

    private final Supplier<Optional<byte[]>> frameSource;
    private final int fps;
    private final int maxFrames;
    private final int maxWidth;
    private final Deque<Frame> frames = new ArrayDeque<>();
    private ScheduledExecutorService executor;
    private long stoppedAt;

    record Frame(long nanos, byte[] png) {
    }

    public VideoRecorder(Supplier<Optional<byte[]>> frameSource, int fps, Duration maxDuration, int maxWidth) {
        if (fps < 1 || fps > 30) {
            throw new IllegalArgumentException("Video fps must be between 1 and 30, was " + fps);
        }
        this.frameSource = frameSource;
        this.fps = fps;
        this.maxFrames = (int) Math.max(1, maxDuration.toSeconds() * fps);
        this.maxWidth = Math.max(320, maxWidth);
    }

    public synchronized void start() {
        if (executor != null) {
            return;
        }
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "autto-video-" + THREAD_COUNTER.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
        executor.scheduleWithFixedDelay(this::capture, 0, 1000L / fps, TimeUnit.MILLISECONDS);
    }

    /** Stops capturing. Safe to call several times. Must be called before the driver quits. */
    public synchronized void stop() {
        if (executor == null) {
            return;
        }
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        stoppedAt = System.nanoTime();
    }

    public int capturedFrames() {
        synchronized (frames) {
            return frames.size();
        }
    }

    /** Encodes the captured frames into {@code target}. Returns empty when nothing was captured. */
    public Optional<Path> save(Path target) {
        stop();
        List<Frame> snapshot;
        synchronized (frames) {
            snapshot = new ArrayList<>(frames);
            frames.clear();
        }
        if (snapshot.isEmpty()) {
            return Optional.empty();
        }
        try {
            Files.createDirectories(target.toAbsolutePath().getParent());
            encode(snapshot, stoppedAt == 0 ? System.nanoTime() : stoppedAt, target);
            return Optional.of(target);
        } catch (IOException | RuntimeException e) {
            LOG.warn("Unable to encode video {}: {}", target, e.toString());
            return Optional.empty();
        }
    }

    /** Drops every captured frame without encoding them. */
    public void discard() {
        stop();
        synchronized (frames) {
            frames.clear();
        }
    }

    private void capture() {
        try {
            frameSource.get().ifPresent(png -> {
                synchronized (frames) {
                    frames.addLast(new Frame(System.nanoTime(), png));
                    while (frames.size() > maxFrames) {
                        frames.removeFirst();
                    }
                }
            });
        } catch (RuntimeException e) {
            LOG.debug("Video frame skipped: {}", e.getMessage());
        }
    }

    private void encode(List<Frame> captured, long endNanos, Path target) throws IOException {
        long interval = TimeUnit.SECONDS.toNanos(1) / fps;
        long start = captured.getFirst().nanos();
        long end = Math.max(endNanos, captured.getLast().nanos() + interval);
        int outputFrames = (int) Math.min(maxFrames, Math.max(1, (end - start) / interval));

        BufferedImage first = decode(captured.getFirst().png());
        int width = even(Math.min(maxWidth, first.getWidth()));
        int height = even((int) Math.round(first.getHeight() * (width / (double) first.getWidth())));

        AWTSequenceEncoder encoder = AWTSequenceEncoder.createSequenceEncoder(target.toFile(), fps);
        try {
            int index = 0;
            BufferedImage current = scale(first, width, height);
            for (int i = 0; i < outputFrames; i++) {
                long timestamp = start + i * interval;
                int next = index;
                while (next + 1 < captured.size() && captured.get(next + 1).nanos() <= timestamp) {
                    next++;
                }
                if (next != index) {
                    index = next;
                    current = scale(decode(captured.get(index).png()), width, height);
                }
                encoder.encodeImage(current);
            }
        } finally {
            encoder.finish();
        }
    }

    private static BufferedImage decode(byte[] png) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        if (image == null) {
            throw new IOException("Frame is not a valid image");
        }
        return image;
    }

    private static BufferedImage scale(BufferedImage source, int width, int height) {
        BufferedImage target = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g = target.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
            g.drawImage(source, 0, 0, width, height, null);
        } finally {
            g.dispose();
        }
        return target;
    }

    private static int even(int value) {
        return Math.max(2, value - (value % 2));
    }
}
