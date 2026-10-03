package io.github.andercmd.autto.core.report;

import io.github.andercmd.autto.core.security.Secrets;

/** Minimal HTML helpers for report content. Every text goes through {@link Secrets#mask(String)}. */
final class Html {

    private Html() {
    }

    static String escape(String text) {
        if (text == null) {
            return "";
        }
        String masked = Secrets.mask(text);
        StringBuilder out = new StringBuilder(masked.length() + 16);
        for (char c : masked.toCharArray()) {
            switch (c) {
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '&' -> out.append("&amp;");
                case '"' -> out.append("&quot;");
                case '\'' -> out.append("&#39;");
                default -> out.append(c);
            }
        }
        return out.toString();
    }

    /** Collapsible pre-formatted block, used for long text such as page sources or logs. */
    static String collapsible(String title, String content, boolean open) {
        return "<details class='autto-details'" + (open ? " open" : "") + "><summary>" + escape(title)
                + "</summary><pre class='autto-pre'>" + escape(content) + "</pre></details>";
    }

    static String video(String relativePath, String title) {
        String src = escape(relativePath);
        return "<div class='autto-video'><div class='autto-video-title'>&#127909; " + escape(title) + "</div>"
                + "<video controls preload='metadata' src='" + src + "'></video>"
                + "<a href='" + src + "' target='_blank' rel='noopener'>Download video</a></div>";
    }

    static String link(String relativePath, String title) {
        return "&#128206; <a href='" + escape(relativePath) + "' target='_blank' rel='noopener'>" + escape(title)
                + "</a>";
    }
}
