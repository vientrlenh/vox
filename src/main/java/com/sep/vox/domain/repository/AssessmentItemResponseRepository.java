package com.sep.vox.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentItemResponse;

public interface AssessmentItemResponseRepository {
    Optional<AssessmentItemResponse> findById(UUID id);
    boolean existsById(UUID id);
    AssessmentItemResponse save(AssessmentItemResponse response);
    List<AssessmentItemResponse> findBySessionId(UUID sessionId);
    void deleteBySessionId(UUID sessionId);
        int sumDurationSecondsBySessionId(UUID sessionId);

    /**
     * Total duration_seconds (real answering seconds) per EACH session in sessionIds -- used by
     * QuotaPricingCalibrationService. Aggregates only 1 table (no join with ai_usage_record) so it is
     * safe, with no cartesian product -- the join with SessionCostDto (query side) is done in the Java layer.
     */
    List<SessionDurationAggregate> sumDurationSecondsGroupedBySessionIds(Collection<UUID> sessionIds);
}
