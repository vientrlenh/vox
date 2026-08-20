package com.sep.vox.interfaces.rest.dto.request;

import java.util.Map;

import jakarta.validation.constraints.NotEmpty;

/**
 * Thân request xác nhận ghép cột, dùng chung cho các luồng import mới.
 *
 * <p>Khoá là tiêu đề cột trong file, giá trị là tên trường hệ thống.
 * {@code AcceptQuestionImportRequest} có nội dung y hệt nhưng giữ nguyên — không đổi kiểu tham số
 * của một endpoint đang chạy chỉ để gộp tên.
 */
public record AcceptImportRequest(
    @NotEmpty(message = "Mapping import không được để trống")
    Map<String, String> confirmedMapping
) {
}
