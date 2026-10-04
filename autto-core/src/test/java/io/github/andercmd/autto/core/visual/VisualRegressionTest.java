package io.github.andercmd.autto.core.visual;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.andercmd.autto.core.config.AuttoProperties;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class VisualRegressionTest {

    @TempDir
    Path dir;

    private static BufferedImage image(Color background, Color box) {
        BufferedImage image = new BufferedImage(100, 50, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(background);
        g.fillRect(0, 0, 100, 50);
        if (box != null) {
            g.setColor(box);
            g.fillRect(10, 10, 20, 20);
        }
        g.dispose();
        return image;
    }

    private static byte[] png(BufferedImage image) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    private AuttoProperties.Visual config(boolean update) {
        return new AuttoProperties.Visual(dir.resolve("baseline").toString(), 0.001, 10, update,
                dir.resolve("diff").toString());
    }

    @Test
    void identicalImagesMatch() {
        assertThat(VisualRegression.compare(image(Color.WHITE, Color.BLUE), image(Color.WHITE, Color.BLUE), 10,
                List.of()).ratio()).isZero();
    }

    @Test
    void differencesAreMeasuredAndIgnoredAreasSkipped() {
        BufferedImage expected = image(Color.WHITE, null);
        BufferedImage actual = image(Color.WHITE, Color.RED);

        assertThat(VisualRegression.compare(expected, actual, 10, List.of()).ratio()).isEqualTo(400.0 / 5000);
        assertThat(VisualRegression.compare(expected, actual, 10, List.of(new Rectangle(0, 0, 40, 40))).ratio())
                .isZero();
    }

    @Test
    void sizeMismatchIsAFullDifference() {
        BufferedImage small = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);

        assertThat(VisualRegression.compare(image(Color.WHITE, null), small, 10, List.of()).ratio()).isEqualTo(1.0);
    }

    @Test
    void baselineWorkflow() throws Exception {
        byte[] approved = png(image(Color.WHITE, Color.BLUE));

        assertThatThrownBy(() -> VisualRegression.assertMatches("home", approved, config(false)))
                .hasMessageContaining("autto.visual.update=true");

        VisualRegression.assertMatches("home", approved, config(true));
        assertThat(dir.resolve("baseline/home.png")).exists();

        VisualRegression.assertMatches("home", approved, config(false));

        byte[] changed = png(image(Color.WHITE, Color.RED));
        assertThatThrownBy(() -> VisualRegression.assertMatches("home", changed, config(false)))
                .isInstanceOf(AssertionError.class).hasMessageContaining("Visual regression in 'home'");
        assertThat(Files.exists(dir.resolve("diff/home-diff.png"))).isTrue();
    }
}
