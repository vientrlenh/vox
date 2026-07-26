package com.sep.vox.interfaces.rest.dto.request;

import jakarta.validation.constraints.Size;

/**
 * Body chung của các hành động không nhập điểm.
 *
 * <p>{@code reason} không đánh {@code @NotBlank} ở đây vì tính bắt buộc phụ thuộc
 * hành động ({@code UPHOLD} thì tuỳ chọn, ba hành động còn lại thì bắt buộc) — luật
 * đó nằm ở {@code GradingRoundPolicy}, không nhân bản xuống DTO.
 */
public record GradingDecisionRequest(
    @Size(max = 1024, message = "Lý do tối đa 1024 ký tự")
    String reason
) {
}
