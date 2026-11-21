package com.sep.vox.application.port.output;

/**
 * Practice settings read from configuration ({@code app.practice.*}), bound by PracticeProperties.
 */
public interface PracticeFrameworkConfigPort {

    /**
     * Code of the framework version to pin for practice; empty (or blank) means the active framework is
     * chosen automatically. The value is returned as configured, without trimming.
     */
    String frameworkVersionCode();
}
