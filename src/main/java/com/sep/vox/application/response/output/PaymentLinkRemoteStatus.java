package com.sep.vox.application.response.output;

public enum PaymentLinkRemoteStatus {
    PENDING,
    PROCESSING,
    UNDERPAID,
    PAID,
    CANCELLED,
    EXPIRED,
    FAILED,
    /**
     * Cổng khẳng định KHÔNG có phiên nào mang mã này -- khác hẳn "hỏi không được" (mất mạng, 5xx,
     * quá rate limit), thứ phải ném ra ngoài chứ không được quy về đây.
     *
     * <p>Sinh ra từ khi dòng payment_records được commit TRƯỚC lúc gọi sang cổng
     * (CreatePaymentCheckoutUrlUseCase): nếu lời gọi tạo link hỏng trước khi cổng kịp dựng phiên, dòng
     * PENDING đó vẫn nằm lại và mang một mã mà bên cổng chưa từng thấy. Không phân biệt được ca này
     * thì lần thử đó treo tới lúc đơn hết hạn, còn trường thì không xin được link mới.
     *
     * <p>An toàn để coi là đã chết: cổng không biết mã thì không có đường nào tiền về theo mã đó.
     */
    NOT_FOUND
}