package io.github.andercmd.autto.core.report;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReportPathsTest {

    @TempDir
    Path temp;

    @Test
    void storesEvidenceWithUniqueRelativePaths() throws Exception {
        ReportPaths paths = new ReportPaths(temp);
        String first = paths.store(ReportPaths.Kind.SCREENSHOT, "Login falló: ¡error!", "png", new byte[] {1});
        String second = paths.store(ReportPaths.Kind.SCREENSHOT, "Login falló: ¡error!", "png", new byte[] {2});

        assertThat(first).isEqualTo("screenshots/0001-login-fallo-error.png");
        assertThat(second).isEqualTo("screenshots/0002-login-fallo-error.png");
        assertThat(Files.readAllBytes(temp.resolve(first))).containsExactly(1);
    }

    @Test
    void cleanEvidenceRemovesPreviousRunFiles() {
        ReportPaths paths = new ReportPaths(temp);
        String stale = paths.store(ReportPaths.Kind.VIDEO, "old", "mp4", new byte[] {1});
        paths.cleanEvidence();
        assertThat(temp.resolve(stale)).doesNotExist();
        assertThat(temp).exists();
    }

    @Test
    void slugNeverReturnsEmptyNames() {
        assertThat(ReportPaths.slug("!!!")).isEqualTo("evidence");
        assertThat(ReportPaths.slug(null)).isEqualTo("evidence");
        assertThat(ReportPaths.slug("x".repeat(100))).hasSize(60);
    }

    @Test
    void htmlIsEscaped() {
        assertThat(Html.escape("<a href=\"x\">'&'</a>"))
                .isEqualTo("&lt;a href=&quot;x&quot;&gt;&#39;&amp;&#39;&lt;/a&gt;");
        assertThat(Html.collapsible("t", "<b>", false)).contains("&lt;b&gt;").doesNotContain(" open");
        assertThat(new String(Html.video("videos/a.mp4", "v").getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8))
                .contains("<video controls").contains("src='videos/a.mp4'");
    }
}
