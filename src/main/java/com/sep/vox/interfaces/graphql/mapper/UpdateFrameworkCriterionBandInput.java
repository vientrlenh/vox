package com.sep.vox.interfaces.graphql.mapper;

import java.util.List;

public record UpdateFrameworkCriterionBandInput(
    String descriptor,
    List<SignalInput> positiveSignals,
    List<SignalInput> negativeSignals
) {}
