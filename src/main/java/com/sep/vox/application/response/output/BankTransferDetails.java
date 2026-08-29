package com.sep.vox.application.response.output;

import java.math.BigDecimal;

/**
 * Thông tin để trường TỰ chuyển khoản khi app ngân hàng không quét được mã.
 *
 * <p>Đi kèm chứ không thay thế chuỗi QR: một mã không quét được là chuyện thường (camera kém, app
 * cũ, ảnh chụp màn hình), và lúc đó thứ duy nhất cứu được giao dịch là bộ số này.
 *
 * <p>{@code transferContent} là trường NGUY HIỂM nhất ở đây. Nó là thứ cổng dùng để khớp khoản tiền
 * với đơn; người dùng gõ sai thì tiền vẫn về tài khoản nhưng không đơn nào nhận, và hệ thống không
 * có luồng hoàn tiền nào — phải đối soát tay. Giao diện phải làm nổi bật và cho copy, đừng bắt gõ.
 */
public record BankTransferDetails(
    String bankBin,
    String accountNumber,
    String accountName,
    BigDecimal amountVnd,
    String transferContent
) {
}
