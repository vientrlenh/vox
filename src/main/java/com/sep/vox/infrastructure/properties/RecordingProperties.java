package com.sep.vox.infrastructure.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.sep.vox.application.port.output.RecordingStorageConfigPort;

/**
 * Recording storage settings ({@code app.recording.*}). A missing key gives an empty string, the same
 * default the former {@code @Value("${app.recording.bucket:}")} produced.
 */
@ConfigurationProperties(prefix = "app.recording")
public record RecordingProperties(
    String bucket
) implements RecordingStorageConfigPort {

    public RecordingProperties {
        bucket = bucket == null ? "" : bucket;
    }
}
