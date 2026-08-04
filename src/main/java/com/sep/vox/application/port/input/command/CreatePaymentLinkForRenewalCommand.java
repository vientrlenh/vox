package com.sep.vox.application.port.input.command;

import java.util.UUID;

// acceptedPlanId: bắt buộc phải khớp với gói mà PreviewRenewalUseCase trả về KHI VÀ CHỈ KHI gói
// hiện tại của subscription đã bị archive và có gói thay thế (renewalPlan != gói hiện tại) — tức
// là trường phải xem trước và xác nhận gói mới trước khi hệ thống tạo payment link. Null nếu gói
// không đổi (không cần xác nhận gì thêm).
public record CreatePaymentLinkForRenewalCommand(
    UUID schoolId,
    UUID subscriptionId,
    UUID acceptedPlanId
) {
}