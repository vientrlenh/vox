package com.sep.vox.application.response.input.payment;

public record PaymentCallbackResponse(
    CallbackOutcome outcome
) {
        /** Kết quả để tầng REST chọn thông điệp trả về. */
    public enum CallbackOutcome {
        /** Đã chốt lần thử (thành công hoặc thất bại). */
        SETTLED,
        /** Chữ ký hợp lệ nhưng không khớp lần thử nào -- vd payload test cố định của cổng. */
        UNKNOWN_PAYMENT,
        /** Lần thử này đã được chốt từ trước -- callback lặp, không làm gì thêm. */
        ALREADY_SETTLED,
        /** Số tiền báo về lệch với lần thử: dừng lại, cần người xem. */
        AMOUNT_MISMATCH,
        /** Trạng thái chưa phải trạng thái cuối (hoặc chưa ánh xạ được) -- chưa chốt gì. */
        NOT_FINAL
    }

    public static PaymentCallbackResponse toResponse(CallbackOutcome outcome) {
        return new PaymentCallbackResponse(outcome);
    }
}
