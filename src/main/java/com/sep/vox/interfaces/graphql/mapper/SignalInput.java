package com.sep.vox.interfaces.graphql.mapper;

public record SignalInput(
    String code,
    String description,
    String importance,
    String evidenceHint
) {}
