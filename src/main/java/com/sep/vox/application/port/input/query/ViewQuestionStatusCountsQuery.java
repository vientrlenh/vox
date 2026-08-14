package com.sep.vox.application.port.input.query;

import java.util.UUID;

import com.sep.vox.domain.model.question.QuestionSharing;
import com.sep.vox.domain.model.question.QuestionType;

/**
 * Giống {@link ViewQuestionsQuery} nhưng bỏ {@code status} (chiều đang nhóm) cùng
 * {@code page}/{@code size} (kết quả là một dòng tổng hợp).
 *
 * <p>Các tham số còn lại phải giữ NGUYÊN danh sách của ViewQuestionsQuery: màn hình dùng
 * chung một bộ lọc cho cả biểu đồ đếm lẫn danh sách, thiếu một tham số ở đây là hai bên
 * cho ra hai con số khác nhau.
 */
public record ViewQuestionStatusCountsQuery(
    UUID questionBankId,
    UUID questionTopicId,
    String topicName,
    QuestionType type,
    QuestionSharing sharing,
    String scope,
    String keyword
) {
}
