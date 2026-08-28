package com.sep.vox.application.port.input.command;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Mọi field (trừ id) đều CHO PHÉP null với nghĩa "không đụng tới" -- đây là sửa từng phần, không
 * phải ghi đè toàn bộ gói.
 *
 * <p>Không có periodType: cột period_type là {@code updatable = false} nên chu kỳ đã chọn lúc tạo
 * thì không đổi được. Gói tính theo THÁNG mà sửa thành NĂM sẽ làm mọi ngày hết hạn đã tính trước đó
 * mang một ý nghĩa khác -- muốn đổi thì tạo gói mới rồi trỏ replacedByPlanId.
 *
 * <p>Không có serviceFeeRatio: phí dịch vụ đã thành config TOÀN HỆ THỐNG
 * (vox.quota.selling-price.service-fee-ratio) chứ không còn lưu theo từng gói -- xem
 * {@code QuotaPricingPort#serviceFeeRatio()}.
 *
 * <p>quotas null = giữ nguyên bộ hạn mức hiện có. Nếu có truyền thì danh sách được coi là TOÀN BỘ
 * bộ hạn mức mới (thay thế hết), không phải phần bổ sung.
 */
public record UpdateSubscriptionPlanCommand(
    UUID subscriptionPlanId,
    String name,
    String tagline,
    BigDecimal priceVnd,
    Integer periodCount,
    Integer maxTimePerAttemptMin,
    List<UpdateSubscriptionPlanQuotaCommand> quotas
) {
}
