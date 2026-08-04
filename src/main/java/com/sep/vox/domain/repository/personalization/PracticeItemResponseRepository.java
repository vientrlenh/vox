package com.sep.vox.domain.repository.personalization;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PracticeItemResponseRepository {

    UUID findRubricVersionIdByResponseId(UUID practiceResponseId);

    /** Học sinh sở hữu response này -- quan sát điểm yếu gắn theo học sinh, không theo phiên. */
    UUID findStudentIdByResponseId(UUID practiceResponseId);

    /** Phiên chứa response này -- để tính lại điểm phiên khi một bản chấm vừa về. */
    UUID findSessionIdByResponseId(UUID practiceResponseId);

    /** Tìm response theo (session, câu hỏi); nếu chưa có thì tạo mới, nếu đã có thì nối thêm
     * transcript và cập nhật audioUrl -- trả về id response.
     *
     * @param questionComplete lượt này có phải lượt cuối của câu không. Chỉ đi một chiều
     *                         false -> true; xem phần cài đặt để biết vì sao. */
    UUID upsertResponse(
        UUID sessionId,
        UUID questionId,
        String audioUrl,
        String transcript,
        boolean questionComplete
    );

    /** Số câu đã có người trả lời nhưng chưa có bản chấm -- màn tổng kết dựa vào đây để biết
     * còn phải đợi hay không. */
    int countAwaitingEvaluation(UUID sessionId);

    /** Trung bình độ khó các câu đã trả lời trong phiên; null khi chưa trả lời câu nào. */
    Double findAverageDifficultyRank(UUID sessionId);

    /** Các câu đã trả lời nhưng CHƯA XONG và chưa từng được chấm -- diện cần xả chấm lúc
     * đóng phiên. */
    List<PendingEvaluationResponse> findResponsesAwaitingFlush(UUID sessionId);

    /** Phiên đã đóng (từ {@code since} tới nay) mà vẫn còn lượt chưa chấm -- lượt mồ côi. */
    List<UUID> findEndedSessionsWithUngradedResponses(Instant since);

    /** Đã có ít nhất 1 turn được nộp cho (session, câu hỏi) này chưa -- dùng để phân biệt
     * "câu vừa resolve nhưng client chưa từng nhận được" (idempotency retry) với "câu đã
     * thực sự bắt đầu trả lời". */
    boolean existsResponse(UUID sessionId, UUID questionId);
}
