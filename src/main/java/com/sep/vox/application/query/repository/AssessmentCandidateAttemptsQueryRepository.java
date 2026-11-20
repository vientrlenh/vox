package com.sep.vox.application.query.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.sep.vox.application.query.dto.AssessmentAttemptSummary;

/**
 * Lượt thi của thí sinh.
 *
 * <p>Hai phương thức tách đôi theo NGƯỜI ĐỌC, không theo dữ liệu: phiên đã xoá mềm là thứ quản trị
 * trường và chủ tịch hội đồng phải thấy (để trả lời "điểm của em đi đâu"), nhưng học sinh thì không.
 * Tách thành hai tên gọi rõ ràng thay vì một cờ boolean để mỗi call-site phải nói ra mình phục vụ ai.
 */
public interface AssessmentCandidateAttemptsQueryRepository {

    /** Mặc định an toàn: BỎ lượt đã xoá mềm. Dùng cho mọi đường học sinh nhìn thấy. */
    List<AssessmentAttemptSummary> findByCandidateIds(Collection<UUID> candidateIds);

    /**
     * Includes soft-deleted attempts too (with {@code status = DELETED} and {@code deletedReason}).
     *
     * <p>ONLY for read paths already restricted to the school administrator / council chair -- currently
     * {@code ResolveAssessmentCandidateAttemptsUseCase}, serving the assessment results screen.
     */
    List<AssessmentAttemptSummary> findByCandidateIdsIncludingDeleted(Collection<UUID> candidateIds);
}
