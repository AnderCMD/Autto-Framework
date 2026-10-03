package io.github.andercmd.autto.core.media;

/** When a piece of evidence (screenshot, video) is captured and kept. */
public enum EvidenceMode {
    /** Never capture. */
    OFF,
    /** Capture only for failed steps / scenarios. */
    ON_FAILURE,
    /** Capture for every step (screenshots) or every scenario (videos). */
    ALWAYS;

    public boolean shouldKeep(boolean failed) {
        return this == ALWAYS || (this == ON_FAILURE && failed);
    }
}
