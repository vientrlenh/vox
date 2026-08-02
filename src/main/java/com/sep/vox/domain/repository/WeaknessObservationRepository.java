package com.sep.vox.domain.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.personalization.WeaknessFrequency;
import com.sep.vox.domain.model.personalization.WeaknessObservation;

public interface WeaknessObservationRepository {

    boolean existsForKey(
        UUID sourceEvaluationId,
        UUID frameworkCriterionId,
        String subAttribute,
        String evidenceSpan
    );

    void save(WeaknessObservation observation);

    List<WeaknessFrequency> findWeaknessFrequencies(
        List<UUID> studentIds,
        OffsetDateTime windowStart,
        OffsetDateTime recentWindowStart
    );
}
