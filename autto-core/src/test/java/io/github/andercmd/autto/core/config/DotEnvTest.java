package io.github.andercmd.autto.core.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DotEnvTest {

    @TempDir
    Path temp;

    @Test
    void parsesCommonSyntax() {
        Map<String, String> values = DotEnv.parse(List.of(
                "# comment",
                "",
                "PLAIN=value # inline comment",
                "export EXPORTED=yes",
                "SINGLE='literal $HOME #not-a-comment'",
                "DOUBLE=\"tab\\there \\\"quoted\\\"\"",
                "MULTI=\"first",
                "second\"",
                "EMPTY=",
                "spaced = trimmed "), "test");

        assertThat(values)
                .containsEntry("PLAIN", "value")
                .containsEntry("EXPORTED", "yes")
                .containsEntry("SINGLE", "literal $HOME #not-a-comment")
                .containsEntry("DOUBLE", "tab\there \"quoted\"")
                .containsEntry("MULTI", "first\nsecond")
                .containsEntry("EMPTY", "")
                .containsEntry("spaced", "trimmed");
    }

    @Test
    void malformedLinesNeverLeakTheirContent() {
        assertThatThrownBy(() -> DotEnv.parse(List.of("this is not valid secret-value"), ".env"))
                .hasMessageContaining("line 1")
                .hasMessageNotContaining("secret-value");
    }

    @Test
    void locatesTheFileInParentFolders() throws Exception {
        Path root = temp.resolve("repo");
        Path module = Files.createDirectories(root.resolve("module/sub"));
        Files.writeString(root.resolve(".env"), "A=1");

        assertThat(DotEnv.locate(Map.of(), Map.of("user.dir", module.toString())))
                .contains(root.resolve(".env").toAbsolutePath());
        assertThat(DotEnv.locate(Map.of(), Map.of("user.dir", temp.toString()))).isEmpty();
    }
}
