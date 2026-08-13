package com.sep.vox.application.query.dto;

import com.sep.vox.domain.model.question.QuestionStatus;

/**
 * Số câu hỏi của MỘT status, trong phạm vi người gọi được xem.
 *
 * <p>Trả về danh sách các dòng như thế này thay vì một record có sẵn một trường cho mỗi
 * status: thêm hằng số mới vào {@link QuestionStatus} thì không phải sửa DTO, schema
 * GraphQL, hay câu truy vấn nào.
 *
 * <p>Đổi lại, {@code GROUP BY} không sinh ra dòng cho status không có câu nào. Việc bù đủ
 * bảy status (kể cả count = 0) là trách nhiệm của tầng use case -- phía client cần một trục
 * ổn định để vẽ biểu đồ, không phải một danh sách co giãn theo dữ liệu.
 */
public record QuestionStatusCountInfo(
    QuestionStatus status,
    long count
) {
}
