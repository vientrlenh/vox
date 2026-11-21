package com.sep.vox.application.port.output;

/**
 * Recording storage settings read from configuration ({@code app.recording.*}), bound by RecordingProperties.
 */
public interface RecordingStorageConfigPort {

    /** Bucket stored on a recording once its object key is known; empty when nothing is configured. */
    String bucket();
}
