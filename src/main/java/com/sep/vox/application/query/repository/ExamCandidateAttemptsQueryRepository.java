package com.sep.vox.application.query.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.sep.vox.application.query.dto.ExamAttemptSummary;

/**
 * Lượt thi của thí sinh.
 *
 * <p>Hai phương thức tách đôi theo NGƯỜI ĐỌC, không theo dữ liệu: phiên đã xoá mềm là thứ quản trị
 * trường và chủ tịch hội đồng phải thấy (để trả lời "điểm của em đi đâu"), nhưng học sinh thì không.
 * Tách thành hai tên gọi rõ ràng thay vì một cờ boolean để mỗi call-site phải nói ra mình phục vụ ai.
 */
public interface ExamCandidateAttemptsQueryRepository {

    /** Mặc định an toàn: BỎ lượt đã xoá mềm. Dùng cho mọi đường học sinh nhìn thấy. */
    List<ExamAttemptSummary> findByCandidateIds(Collection<UUID> candidateIds);

    /**
     * Kèm cả lượt đã xoá mềm (có {@code status = DELETED} và {@code deletedReason}).
     *
     * <p>CHỈ dùng cho đường đọc đã chặn quyền ở quản trị trường / chủ tịch hội đồng — hiện là
     * {@code ResolveExamCandidateAttemptsUseCase}, phục vụ màn kết quả kỳ thi.
     */
    List<ExamAttemptSummary> findByCandidateIdsIncludingDeleted(Collection<UUID> candidateIds);
}
