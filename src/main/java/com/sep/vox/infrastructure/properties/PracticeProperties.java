package com.sep.vox.infrastructure.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.sep.vox.application.port.output.PracticeFrameworkConfigPort;

/**
 * Practice settings bound from {@code app.practice.*}. A missing {@code framework-version-code} gives an
 * empty string, the same default the former {@code @Value("${app.practice.framework-version-code:}")}
 * produced. Other keys under the prefix ({@code heartbeat-cleanup-ms}) are read elsewhere and ignored here.
 */
@ConfigurationProperties(prefix = "app.practice")
public record PracticeProperties(
    String frameworkVersionCode
) implements PracticeFrameworkConfigPort {

    public PracticeProperties {
        frameworkVersionCode = frameworkVersionCode == null ? "" : frameworkVersionCode;
    }
}
