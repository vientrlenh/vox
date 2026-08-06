package com.sep.vox.domain.repository;

import java.time.Instant;
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

    /**
     * @param decayBase {@code 1 - alpha}; trọng số của quan sát cách đây k lần chấm là
     *     {@code decayBase^k}. Thay cho cửa sổ "gần đây" cứng trước đây.
     */
    List<WeaknessFrequency> findWeaknessFrequencies(
        List<UUID> studentIds,
        Instant windowStart,
        double decayBase
    );
}
