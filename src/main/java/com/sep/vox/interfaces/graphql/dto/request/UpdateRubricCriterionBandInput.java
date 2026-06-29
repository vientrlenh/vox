package com.sep.vox.interfaces.graphql.dto.request;

import java.math.BigDecimal;

public record UpdateRubricCriterionBandInput(
        String code,
        BigDecimal scoreMin,
        BigDecimal scoreMax
) {}