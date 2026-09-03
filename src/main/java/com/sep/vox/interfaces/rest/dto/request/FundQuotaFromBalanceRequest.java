package com.sep.vox.interfaces.rest.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Nạp tiền từ ví tự nạp của trường vào ví hạn mức của một loại quota.
 *
 * <p>{@code quotaType} nằm ở đường dẫn chứ không ở đây, cùng khuôn với endpoint đặt trần phân phối.
 */
public record FundQuotaFromBalanceRequest(
    // inclusive = false: 0 đồng không phải một lần nạp, và một bút toán 0đ vi phạm thẳng
    // chk_school_balance_entries_quota_funding_traceable (đòi amount_vnd < 0 sau khi đảo dấu).
    @NotNull(message = "Số tiền nạp không được để trống")
    @DecimalMin(value = "0", inclusive = false, message = "Số tiền nạp phải lớn hơn 0")
    BigDecimal amountVnd,

    // Tuỳ chọn: ghi chú của quản trị viên, lưu thẳng vào bút toán. Khớp độ dài cột
    // school_balance_entries.reason varchar(2048) -- cắt ở đây để lỗi ra thành câu đọc được thay vì
    // một lỗi cắt chuỗi từ driver.
    @Size(max = 2048, message = "Ghi chú không được vượt quá 2048 ký tự")
    String reason
) {
}
