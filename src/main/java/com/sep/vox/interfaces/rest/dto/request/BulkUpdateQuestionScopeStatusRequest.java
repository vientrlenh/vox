package com.sep.vox.interfaces.rest.dto.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Dùng chung cho đổi trạng thái hàng loạt ngân hàng và chủ đề câu hỏi.
 *
 * <p>Chỉ nhận PUBLISH/ARCHIVE -- hai thực thể này không có vòng duyệt như câu hỏi, nên không có
 * SUBMIT/APPROVE/REJECT và cũng không cần ghi chú.
 */
public record BulkUpdateQuestionScopeStatusRequest(
    @NotEmpty(message = "Danh sách không được để trống")
    @Size(max = 200, message = "Chỉ được cập nhật tối đa 200 mục mỗi lần")
    List<UUID> ids,

    @NotBlank(message = "Action là bắt buộc")
    @Pattern(regexp = "PUBLISH|ARCHIVE", message = "Action không hợp lệ")
    String action
) {
}
