package com.sep.vox.domain.repository.assessment;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentAppealStatus;
import com.sep.vox.domain.model.assessment.AssessmentResultAppeal;

public interface AssessmentResultAppealRepository {
    Optional<AssessmentResultAppeal> findById(UUID id);
    AssessmentResultAppeal save(AssessmentResultAppeal appeal);
    boolean existsOpenByCandidateResultId(UUID candidateResultId);
    /** Số vòng phúc khảo đã thực sự chấm lại và công bố — đơn bị từ chối không tính. */
    long countPublishedByCandidateResultId(UUID candidateResultId);
    List<AssessmentResultAppeal> findByCandidateResultId(UUID candidateResultId);
    void deleteByIdIn(Collection<UUID> ids);
    long countBySchoolIdAndStatusIn(UUID schoolId, Collection<AssessmentAppealStatus> statuses);

    /** Mốc nộp của đơn chờ xử lý lâu nhất của trường; null khi hàng đợi sạch — null KHÁC 0 ngày. */
    Instant findOldestPendingRequestedAt(UUID schoolId);
}
