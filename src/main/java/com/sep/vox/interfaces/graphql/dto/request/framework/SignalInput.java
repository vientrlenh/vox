package com.sep.vox.interfaces.graphql.dto.request.framework;

public record SignalInput(
    String code,
    String description,
    String importance,
    String evidenceHint
) {}
