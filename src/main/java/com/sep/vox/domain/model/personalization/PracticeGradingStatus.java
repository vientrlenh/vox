package com.sep.vox.domain.model.personalization;

/**
 * Vòng đời chấm của MỘT câu trả lời luyện tập ({@code practice_item_responses.grading_status}).
 *
 * <p>Khác hẳn tầng phiên: {@code practice_sessions.status} (IN_PROGRESS / COMPLETED / ABANDONED)
 * tả HÀNH VI NGƯỜI HỌC -- "em có nói không" -- chứ không tả tiến độ chấm. Hai thứ cố ý tách rời,
 * xem chú thích dài ở {@code EndPracticeSessionUseCase}.
 *
 * <p>Đặt tên bám {@code ExamSessionStatus} bên thi để cả repo dùng chung một bộ từ vựng. Riêng
 * {@link #GRADING_FAILED} phải giữ đủ tiền tố: {@code ExamCandidateResultStatus.FAILED} trong
 * repo này nghĩa là HỌC SINH TRƯỢT, nên đặt {@code FAILED} cho lỗi kỹ thuật là mời người đọc sau
 * hiểu sai theo hướng tệ nhất.
 */
public enum PracticeGradingStatus {

    PENDING,

    GRADING,

    GRADED,

    GRADING_FAILED;

    /** Đã chốt, không còn gì để chờ nữa. */
    public boolean isTerminal() {
        return this == GRADED;
    }
}
