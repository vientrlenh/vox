package com.sep.vox.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.exam.ExamItemEvaluation;

public interface ExamItemEvaluationRepository {
    ExamItemEvaluation save(ExamItemEvaluation evaluation);

    /**
     * Authoritative evaluation for a response: the most recent one that is
     * AUTO_GRADED or FINALIZED. Appeal reviewer reports (UNDER_REVIEW) and
     * evaluations replaced by an appeal (SUPERSEDED) are never returned.
     */
    Optional<ExamItemEvaluation> findLatestByResponseId(UUID responseId);

    /**
     * Bản AI mới nhất của một câu trả lời, CỐ Ý không lọc theo status: sau khi giáo viên
     * chấm lại, bản AI bị đẩy về SUPERSEDED nhưng nó vẫn là nguồn DUY NHẤT của lượt nói
     * (audio/transcript/word feedback) và của mọi bằng chứng AI — bản chấm tay không sinh
     * turn và không có signals/validity/confidence.
     *
     * <p>Cùng khuôn với {@code JpaExamGradingQueryRepository#aiEvaluationsByResponseIds}
     * vốn phục vụ màn chấm của giáo viên. UNDER_REVIEW bị loại tường minh — đó là báo cáo
     * phúc khảo CHƯA công bố, không được lọt tới học sinh.
     */
    Optional<ExamItemEvaluation> findLatestAiByResponseId(UUID responseId);
    List<ExamItemEvaluation> findLatestByResponseIdIn(Collection<UUID> responseIds);
    List<ExamItemEvaluation> findByResponseIdIn(Collection<UUID> responseIds);
    void deleteByResponseIdIn(Collection<UUID> responseIds);
}
