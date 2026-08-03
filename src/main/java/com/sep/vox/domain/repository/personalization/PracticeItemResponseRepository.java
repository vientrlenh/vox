package com.sep.vox.domain.repository.personalization;

import java.util.UUID;

public interface PracticeItemResponseRepository {

    UUID findRubricVersionIdByResponseId(UUID practiceResponseId);

    /** Học sinh sở hữu response này -- quan sát điểm yếu gắn theo học sinh, không theo phiên. */
    UUID findStudentIdByResponseId(UUID practiceResponseId);

    /** Tìm response theo (session, câu hỏi); nếu chưa có thì tạo mới, nếu đã có thì nối thêm
     * transcript và cập nhật audioUrl -- trả về id response. */
    UUID upsertResponse(
        UUID sessionId,
        UUID questionId,
        String audioUrl,
        String transcript
    );

    /** Đã có ít nhất 1 turn được nộp cho (session, câu hỏi) này chưa -- dùng để phân biệt
     * "câu vừa resolve nhưng client chưa từng nhận được" (idempotency retry) với "câu đã
     * thực sự bắt đầu trả lời". */
    boolean existsResponse(UUID sessionId, UUID questionId);
}
