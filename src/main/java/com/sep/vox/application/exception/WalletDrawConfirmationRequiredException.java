package com.sep.vox.application.exception;

import java.math.BigDecimal;

/**
 * A personal quota allocation asked for more than the part of the pool that can still be shared out, so it eats into the school's
 * self-topped-up wallet -- thrown when there is no explicit confirmation ({@code confirmWalletDraw=false}), because that wallet is shared by both ASSESSMENT and
 * PRACTICE (see the {@code SchoolBalance} javadoc). DIFFERENT from {@code IllegalArgumentException}: this is not a wrong
 * request, it only needs the user to confirm again and resend with the flag turned on.
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
