package com.sep.vox.interfaces.graphql.dto.request;

import java.math.BigDecimal;

public record UpdateRubricCriterionBandInput(
        BigDecimal scoreMin,
        BigDecimal scoreMax
) {}