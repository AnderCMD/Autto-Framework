package io.github.andercmd.autto.core.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BrowserTagsTest {

    @Test
    void parsesViewportTags() {
        assertThat(BrowserTags.viewport("@viewport:390x844")).contains(new BrowserTags.Viewport(390, 844));
    }

    @Test
    void ignoresOtherTags() {
        assertThat(BrowserTags.viewport("@smoke")).isEmpty();
        assertThat(BrowserTags.viewport("@viewport:wide")).isEmpty();
    }
}
