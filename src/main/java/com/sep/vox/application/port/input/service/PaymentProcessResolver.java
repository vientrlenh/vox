package com.sep.vox.application.port.input.service;

import java.util.Map;
import org.springframework.stereotype.Service;

import com.sep.vox.application.port.output.PaymentProcessPort;

// Chọn đúng PaymentPort (PayOS, hoặc provider mới sau này) theo PaymentMethod, thay vì use case
// inject thẳng 1 implementation cụ thể. Spring tự gom mọi bean implement PaymentPort vào List —
// thêm provider mới chỉ cần thêm 1 class @Service implements PaymentPort, không phải sửa gì ở đây
// hay ở các use case đang gọi resolve(...).
@Service
public class PaymentProcessResolver {

    private final Map<String, PaymentProcessPort> paymentProcessers;

    public PaymentProcessResolver(Map<String, PaymentProcessPort> paymentProcessers) {
        this.paymentProcessers = paymentProcessers;
    }

    public PaymentProcessPort resolve(String selectedMethod) {
        var port = paymentProcessers.get(selectedMethod);
        if (port == null) {
            throw new IllegalArgumentException("Chưa hỗ trợ cổng thanh toán: " + selectedMethod);
        }
        return port;
    }
}
