package io.github.andercmd.autto.core.media;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;
import org.jcodec.api.FrameGrab;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.Picture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class VideoRecorderTest {

    @TempDir
    Path temp;

    @Test
    void recordsAPlayableMp4() throws Exception {
        AtomicInteger counter = new AtomicInteger();
        // odd sizes on purpose: H.264 needs even dimensions
        VideoRecorder recorder = new VideoRecorder(() -> Optional.of(frame(counter.incrementAndGet(), 641, 401)), 10,
                Duration.ofSeconds(30), 1280);

        recorder.start();
        Thread.sleep(700);
        recorder.stop();

        Path video = recorder.save(temp.resolve("out/video.mp4")).orElseThrow();
        assertThat(Files.size(video)).isPositive();
        try (var channel = NIOUtils.readableChannel(video.toFile())) {
            Picture picture = FrameGrab.createFrameGrab(channel).getNativeFrame();
            assertThat(picture.getWidth()).isEqualTo(640);
            assertThat(picture.getHeight()).isEqualTo(400);
        }
    }

    @Test
    void discardedRecordingsProduceNothing() {
        VideoRecorder recorder = new VideoRecorder(() -> Optional.of(frame(1, 100, 100)), 5, Duration.ofSeconds(5), 640);
        recorder.start();
        recorder.discard();
        assertThat(recorder.capturedFrames()).isZero();
        assertThat(recorder.save(temp.resolve("none.mp4"))).isEmpty();
    }

    @Test
    void frameSourceFailuresAreIgnored() throws Exception {
        VideoRecorder recorder = new VideoRecorder(() -> {
            throw new IllegalStateException("alert open");
        }, 10, Duration.ofSeconds(5), 640);
        recorder.start();
        Thread.sleep(200);
        recorder.stop();
        assertThat(recorder.save(temp.resolve("none.mp4"))).isEmpty();
    }

    @Test
    void evidenceModes() {
        assertThat(EvidenceMode.OFF.shouldKeep(true)).isFalse();
        assertThat(EvidenceMode.ON_FAILURE.shouldKeep(true)).isTrue();
        assertThat(EvidenceMode.ON_FAILURE.shouldKeep(false)).isFalse();
        assertThat(EvidenceMode.ALWAYS.shouldKeep(false)).isTrue();
    }

    private static byte[] frame(int index, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(new Color((index * 40) % 255, 120, 200));
        g.fillRect(0, 0, width, height);
        g.setColor(Color.WHITE);
        g.drawString("frame " + index, 20, 40);
        g.dispose();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
