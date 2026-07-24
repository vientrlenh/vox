package com.sep.vox.interfaces.graphql.dto.request;

public record SignalInput(
    String code,
    String description,
    String importance,
    String evidenceHint
) {}
