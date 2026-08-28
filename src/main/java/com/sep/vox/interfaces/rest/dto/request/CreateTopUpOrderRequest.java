package com.sep.vox.interfaces.rest.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

/**
 * creditAmountVnd là số dư trường MUỐN NHẬN, chưa gồm phí dịch vụ -- số phải trả là
 * creditAmountVnd + phí, do use case tính rồi ghi vào orders.total_amount_vnd.
 */
public record CreateTopUpOrderRequest(
    @NotNull(message = "Số tiền nạp không được để trống")
    @DecimalMin(value = "1", message = "Số tiền nạp phải lớn hơn 0")
    // fraction = 0 vì VND không còn đơn vị lẻ và PayOS/SePay từ chối số thập phân; integer = 15 khớp
    // orders.subtotal_amount_vnd numeric(15,0). Chặn ở đây để ra 400 đọc được thay vì để use case ném
    // IllegalArgumentException hay để Postgres làm tròn im lặng.
    @Digits(integer = 15, fraction = 0, message = "Số tiền nạp phải là số nguyên VND và không vượt quá 15 chữ số")
    BigDecimal creditAmountVnd
) {
}
