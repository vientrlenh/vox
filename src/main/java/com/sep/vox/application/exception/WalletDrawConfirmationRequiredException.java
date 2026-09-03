package com.sep.vox.application.exception;

import java.math.BigDecimal;

/**
 * Phân bổ hạn mức cá nhân yêu cầu vượt phần pool còn chia được, ăn vào ví tự nạp của trường -- ném ra
 * khi chưa có xác nhận rõ ràng ({@code confirmWalletDraw=false}), vì ví đó dùng chung cho cả EXAM lẫn
 * PRACTICE (xem {@code SchoolBalance} javadoc). KHÁC {@code IllegalArgumentException}: đây không phải
 * yêu cầu sai, chỉ là cần người dùng xác nhận lại rồi gửi lại với cờ bật.
 */
public class WalletDrawConfirmationRequiredException extends RuntimeException {

    private final BigDecimal walletDrawVnd;
    private final BigDecimal walletBalanceVnd;

    public WalletDrawConfirmationRequiredException(BigDecimal walletDrawVnd, BigDecimal walletBalanceVnd) {
        super(String.format(
            "Phân bổ này vượt phần chia từ gói, sẽ trích thêm %s từ ví tự nạp của trường (đang còn %s). "
                + "Xác nhận để tiếp tục.",
            formatVnd(walletDrawVnd), formatVnd(walletBalanceVnd)));
        this.walletDrawVnd = walletDrawVnd;
        this.walletBalanceVnd = walletBalanceVnd;
    }

    public BigDecimal getWalletDrawVnd() {
        return walletDrawVnd;
    }

    public BigDecimal getWalletBalanceVnd() {
        return walletBalanceVnd;
    }

    private static String formatVnd(BigDecimal amountVnd) {
        return amountVnd.toPlainString() + "đ";
    }
}
