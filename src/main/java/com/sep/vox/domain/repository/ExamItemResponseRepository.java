package com.sep.vox.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.exam.ExamItemResponse;

public interface ExamItemResponseRepository {
    Optional<ExamItemResponse> findById(UUID id);
    boolean existsById(UUID id);
    ExamItemResponse save(ExamItemResponse response);
    List<ExamItemResponse> findBySessionId(UUID sessionId);
    void deleteBySessionId(UUID sessionId);
        int sumDurationSecondsBySessionId(UUID sessionId);

    /**
     * Tổng duration_seconds (giây trả lời thật) theo TỪNG session trong sessionIds -- dùng cho
     * QuotaPricingCalibrationService. Chỉ aggregate 1 bảng (không join ai_usage_record) nên an
     * toàn, không bị cartesian product -- join với SessionCostAggregate làm ở tầng Java.
     */
    List<SessionDurationAggregate> sumDurationSecondsGroupedBySessionIds(Collection<UUID> sessionIds);
}
