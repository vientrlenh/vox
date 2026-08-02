package com.sep.vox.domain.model.personalization;

import java.util.UUID;

public record WeaknessFrequency(
    UUID studentId,
    UUID frameworkCriterionId,
    String subAttribute,
    int frequency,
    int recentFrequency
) {
}
