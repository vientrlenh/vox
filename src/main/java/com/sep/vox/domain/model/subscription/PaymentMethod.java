package com.sep.vox.domain.model.subscription;

public enum PaymentMethod {

    PAYOS(true),
    SEPAY(true),
    // Hóa đơn đối soát tay: tiền được thu ngoài hệ thống nên không cổng nào xử lý —
    // providerOrderRef luôn null, bù lại FinancialEvent ghi actorId của người nhập.
    MANUAL(false);

    private final boolean onlineGateway;

    PaymentMethod(boolean onlineGateway) {
        this.onlineGateway = onlineGateway;
    }

    /**
     * Có một adapter {@code PaymentProcessPort} đứng sau hay không. Đây là ranh giới phân biệt hai
     * nhóm nghiệp vụ đối lập: chỉ cổng trực tuyến mới tạo được link thanh toán và mới có mã đơn để
     * đối soát ngược; ngược lại chỉ phương thức thu ngoài hệ thống mới được ghi nhận là đã trả
     * ngay mà không qua bước xác nhận nào.
     */
    public boolean isOnlineGateway() {
        return onlineGateway;
    }

    public static PaymentMethod resolve(String method) {
        try {
            return PaymentMethod.valueOf(method);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Phương thức thanh toán " + method + " không được hỗ trợ", e);
        }
    }

    /**
     * Dùng ở các use case tạo link thanh toán. Chặn MANUAL ngay từ đầu thay vì để nó đi tiếp rồi
     * chết ở PaymentProcessResolver với thông điệp khó hiểu — quan trọng hơn, tránh việc dựng sẵn
     * hóa đơn rồi mới phát hiện không tạo link được.
     */
    public static PaymentMethod resolveOnlineGateway(String method) {
        var paymentMethod = resolve(method);
        if (!paymentMethod.isOnlineGateway()) {
            throw new IllegalArgumentException(
                "Phương thức " + paymentMethod + " không tạo được link thanh toán vì không qua cổng nào.");
        }
        return paymentMethod;
    }
}
