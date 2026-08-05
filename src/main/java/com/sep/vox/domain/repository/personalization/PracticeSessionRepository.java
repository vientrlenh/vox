package com.sep.vox.domain.repository.personalization;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.personalization.PracticeSession;

public interface PracticeSessionRepository {

    Optional<PracticeSession> findById(UUID id);

    /** Khoá row (SELECT ... FOR UPDATE) -- dùng khi transaction sẽ ghi thêm dữ liệu phụ thuộc
     * vào trạng thái hiện tại của session và phải serialize với các lời gọi đồng thời khác cho
     * cùng session (ví dụ ResolveNextPracticeQuestionUseCase). */
    Optional<PracticeSession> findByIdForUpdate(UUID id);

    Optional<PracticeSession> findByIdAndStudentId(UUID id, UUID studentId);

    boolean existsByIdAndStudentIdAndStatus(UUID id, UUID studentId, String status);

    PracticeSession save(PracticeSession session);

    List<PracticeSession> findStaleInProgress(Instant staleBefore);

    /** Tính lại điểm phiên bằng một câu UPDATE -- KHÔNG nạp entity rồi save lại, vì save() ghi
     * đè cả dòng và có thể xoá mất graded_seconds do lượt nộp song song vừa cộng vào. */
    void refreshOverallScore(UUID sessionId);
}
