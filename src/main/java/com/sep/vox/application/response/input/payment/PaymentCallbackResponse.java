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
        /**
         * Cổng báo ĐÃ TRẢ cho một lần thử mình đã ghi thất bại: tiền về thật nhưng không còn dòng nào
         * nhận nó. Tách khỏi ALREADY_SETTLED vì hai ca này ngược hẳn nhau về mức nghiêm trọng --
         * một bên là callback lặp vô hại, một bên là tiền của trường đang treo lơ lửng.
         */
        PAID_AFTER_WRITE_OFF,
        /** Số tiền báo về lệch với lần thử: dừng lại, cần người xem. */
        AMOUNT_MISMATCH,
        /** Trạng thái chưa phải trạng thái cuối (hoặc chưa ánh xạ được) -- chưa chốt gì. */
        NOT_FINAL
    }

    public static PaymentCallbackResponse toResponse(CallbackOutcome outcome) {
        return new PaymentCallbackResponse(outcome);
    }
}
