package com.sep.vox.application.port.input.command;

import java.math.BigDecimal;

/**
 * creditAmountVnd là SỐ DƯ TRƯỜNG MUỐN NHẬN, chưa gồm phí dịch vụ -- số tiền thật phải trả nằm ở
 * {@code orders.total_amount_vnd} = creditAmountVnd + charged_fee_vnd. Nhận theo hướng này thay vì
 * nhận tổng tiền rồi trừ ngược ra, vì trường luôn nghĩ theo kiểu "tôi muốn có thêm 1 triệu để tiêu",
 * và vì làm ngược lại thì phí thành phần dư của phép chia, lệch vài đồng do làm tròn.
 *
 * <p>Không mang schoolId: lấy từ token của school admin đang đăng nhập -- xem
 * {@link CreateSubscriptionOrderCommand}.
 *
 * <p>Không còn subscriptionId và không còn danh sách theo QuotaType như BuyTokensCommand cũ: số dư
 * giờ thuộc về TRƯỜNG (school_balances) chứ không thuộc về một lần đăng ký, và là MỘT con số duy
 * nhất không chia theo loại hạn mức. Mang subscriptionId vào đây là nối lại đúng thứ refactor này
 * vừa cắt -- tiền tự nạp không được chết theo gói.
 */
public record CreateTopUpOrderCommand(
    BigDecimal creditAmountVnd
) {
}
