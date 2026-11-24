package com.sep.vox.interfaces.graphql.input.framework;

public record SignalInput(
    String code,
    String description,
    String importance,
    String evidenceHint
) {}
