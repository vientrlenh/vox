package com.sep.vox.interfaces.graphql.dto.request;

import java.util.List;

public record UpdateFrameworkCriterionBandInput(
    String descriptor,
    List<SignalInput> positiveSignals,
    List<SignalInput> negativeSignals
) {}
