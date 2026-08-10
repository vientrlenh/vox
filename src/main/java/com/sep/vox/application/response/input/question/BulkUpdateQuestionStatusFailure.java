package com.sep.vox.application.response.input.question;

import java.util.UUID;

/**
 * Một câu hỏi bị bỏ qua trong lần cập nhật hàng loạt.
 *
 * <p>Ngoài {@code reason} (tiếng Việt, hiển thị thẳng cho người dùng) còn có {@code reasonCode} để
 * client gom nhóm các câu cùng lý do, và {@code questionCode}/{@code currentStatus} để client hiển
 * thị được danh sách câu bị bỏ qua mà không phải tra ngược sang danh sách đang xem — trước đây nó
 * phải tự join theo id nên câu nào không nằm trên trang hiện tại sẽ biến mất khỏi thông báo.
 *
 * @param questionCode {@code null} khi không tìm thấy câu hỏi
 * @param currentStatus tên enum {@code QuestionStatus}, {@code null} khi không tìm thấy câu hỏi
 * @param reasonCode tên enum {@code QuestionStatusTransition.RejectionCode}
 */
public record BulkUpdateQuestionStatusFailure(
    UUID questionId,
    String questionCode,
    String currentStatus,
    String reasonCode,
    String reason
) {
}
