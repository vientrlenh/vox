package com.sep.vox.domain.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PracticeItemResponseRepository {

    UUID findRubricVersionIdByResponseId(UUID practiceResponseId);

    UUID findSessionIdByResponseId(UUID practiceResponseId);


    UUID upsertResponse(
        UUID sessionId,
        UUID questionId,
        String audioUrl,
        String transcript,
        boolean questionComplete
    );

   
    int countAwaitingEvaluation(UUID sessionId);

    Double findAverageDifficultyRank(UUID sessionId);


    /**
     * Câu cần xả chấm, BỎ QUA những câu vừa gửi yêu cầu gần đây.
     *
     * @param requestedBefore chỉ lấy câu chưa từng gửi, hoặc gửi trước mốc này. Không có tham số
     *                        này thì câu đang chấm dở (mất ~3,5 phút) bị bắn lại mỗi 5 phút.
     */
    List<PendingEvaluationResponse> findResponsesAwaitingFlush(
        UUID sessionId, Instant requestedBefore, int maxAttempts);

    /** Đóng dấu đã gửi yêu cầu chấm (GRADING + tăng số lần thử) -- gọi NGAY SAU khi publish. */
    void markGradingRequested(UUID responseId, Instant requestedAt);

    /** Bản chấm đã về -- gọi CÙNG TRANSACTION với lúc ghi practice_item_evaluations. */
    void markGraded(UUID responseId);

    /** Agents báo chấm hỏng. */
    void markGradingFailed(UUID responseId);

    /** Số câu của phiên đã bỏ cuộc: hỏng và hết lượt thử. */
    int countGradingGaveUp(UUID sessionId, int maxAttempts);

    List<UUID> findEndedSessionsWithUngradedResponses(Instant since);

    boolean existsResponse(UUID sessionId, UUID questionId);
}
