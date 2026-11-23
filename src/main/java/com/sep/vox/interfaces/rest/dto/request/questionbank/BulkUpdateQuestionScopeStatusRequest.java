package com.sep.vox.interfaces.rest.dto.request.questionbank;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Shared by the bulk status change of question banks and question topics.
 *
 * <p>Accepts only PUBLISH/ARCHIVE -- these two entities have no review cycle like questions, so there is no
 * SUBMIT/APPROVE/REJECT and no note is needed either.
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
