package com.sep.vox.application.response.input.subscription;

import java.math.BigDecimal;

import com.sep.vox.domain.dto.SchoolSubscriptionQuotaRecordDto;

/**
 * Kết quả một lần nạp tiền từ ví tự nạp vào ví hạn mức.
 *
 * <p>Trả về CẢ HAI phía của phép chuyển vì màn hình phải cập nhật cả hai con số cùng lúc, và vì đây
 * là cách duy nhất để người vừa bấm thấy ngay rằng tiền đã đi đúng chỗ -- một thao tác không hoàn lại
 * được thì không nên bắt người ta mở màn khác để kiểm tra.
 *
 * @param pool             ví hạn mức SAU khi nạp -- {@code fundedFromBalanceVnd} trong đó là tổng
 *                         cộng dồn của kỳ, không phải riêng lần nạp này
 * @param fundedAmountVnd  số vừa chuyển, luôn dương
 * @param balanceAfterVnd  số dư ví tự nạp còn lại, khớp đúng với bút toán vừa ghi
 */
public record QuotaFundingResponse(
    SchoolSubscriptionQuotaRecordDto pool,
    BigDecimal fundedAmountVnd,
    BigDecimal balanceAfterVnd
) {
}
