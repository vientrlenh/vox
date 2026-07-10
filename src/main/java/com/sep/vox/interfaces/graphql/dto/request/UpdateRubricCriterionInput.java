package com.sep.vox.interfaces.graphql.dto.request;

import java.math.BigDecimal;

public record UpdateRubricCriterionInput(
        String name,
        String description,
        String examplesJson,
        BigDecimal weight,
        BigDecimal minScore,
        BigDecimal maxScore,
        Integer order,
        Boolean isRequired    // Dùng class Wrapper để hứng null
) {}