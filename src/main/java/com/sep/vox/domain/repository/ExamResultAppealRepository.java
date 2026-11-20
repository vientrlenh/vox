package com.sep.vox.domain.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.exam.ExamAppealStatus;
import com.sep.vox.domain.model.exam.ExamResultAppeal;

public interface ExamResultAppealRepository {
    Optional<ExamResultAppeal> findById(UUID id);
    ExamResultAppeal save(ExamResultAppeal appeal);
    boolean existsOpenByCandidateResultId(UUID candidateResultId);
    /** Số vòng phúc khảo đã thực sự chấm lại và công bố — đơn bị từ chối không tính. */
    long countPublishedByCandidateResultId(UUID candidateResultId);
    List<ExamResultAppeal> findByCandidateResultId(UUID candidateResultId);
    void deleteByIdIn(Collection<UUID> ids);
    long countBySchoolIdAndStatusIn(UUID schoolId, Collection<ExamAppealStatus> statuses);

    /** Mốc nộp của đơn chờ xử lý lâu nhất của trường; null khi hàng đợi sạch — null KHÁC 0 ngày. */
    Instant findOldestPendingRequestedAt(UUID schoolId);
}
