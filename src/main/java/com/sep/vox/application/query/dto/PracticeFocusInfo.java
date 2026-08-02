package com.sep.vox.application.query.dto;

public record PracticeFocusInfo(
    String primaryCriterion,
    String secondaryCriterion,
    String primarySubAttribute,
    String secondarySubAttribute
) {
}
