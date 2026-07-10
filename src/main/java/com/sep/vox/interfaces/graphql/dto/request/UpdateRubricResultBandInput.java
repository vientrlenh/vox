package com.sep.vox.interfaces.graphql.dto.request;

import java.math.BigDecimal;

public record UpdateRubricResultBandInput(
        String name,
        String description,
        BigDecimal scoreMin,
        BigDecimal scoreMax,
        Integer order
) {}