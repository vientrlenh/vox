package com.sep.vox.domain.repository.assessment;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentItemAnswer;
import com.sep.vox.domain.repository.SessionDurationAggregate;

public interface AssessmentItemAnswerRepository {
    Optional<AssessmentItemAnswer> findById(UUID id);
    boolean existsById(UUID id);
    AssessmentItemAnswer save(AssessmentItemAnswer response);
    List<AssessmentItemAnswer> findBySessionId(UUID sessionId);
    void deleteBySessionId(UUID sessionId);
        int sumDurationSecondsBySessionId(UUID sessionId);

    /**
     * Tổng duration_seconds (giây trả lời thật) theo TỪNG session trong sessionIds -- dùng cho
     * QuotaPricingCalibrationService. Chỉ aggregate 1 bảng (không join ai_usage_record) nên an
     * toàn, không bị cartesian product -- join với SessionCostDto (query side) làm ở tầng Java.
     */
    List<SessionDurationAggregate> sumDurationSecondsGroupedBySessionIds(Collection<UUID> sessionIds);
}
