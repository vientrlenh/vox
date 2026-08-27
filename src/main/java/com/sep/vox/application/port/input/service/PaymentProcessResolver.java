package com.sep.vox.application.port.input.service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.sep.vox.application.port.output.PaymentProcessPort;
import com.sep.vox.domain.model.payment.PaymentProvider;

// Chọn đúng adapter theo PaymentMethod, thay vì use case inject thẳng 1 implementation cụ thể.
// Lập chỉ mục theo PaymentProcessPort.provider() chứ không theo tên bean dạng chuỗi: gõ sai tên
// bean chỉ lộ ra lúc chạy, còn sai enum thì compiler bắt ngay. Thêm cổng mới = thêm 1 class
// implements PaymentProcessPort, không phải sửa gì ở đây.
@Service
public class PaymentProcessResolver {

    private final Map<PaymentProvider, PaymentProcessPort> paymentProcessors;

    public PaymentProcessResolver(List<PaymentProcessPort> paymentProcessors) {
        this.paymentProcessors = new EnumMap<>(PaymentProvider.class);
        for (var processor : paymentProcessors) {
            var existing = this.paymentProcessors.put(processor.provider(), processor);
            if (existing != null) {
                throw new IllegalStateException(
                    "Có hai adapter cùng khai provider() = " + processor.provider() + ": "
                        + existing.getClass().getName() + " và " + processor.getClass().getName());
            }
        }
    }

    public PaymentProcessPort resolve(PaymentProvider provider) {
        var port = paymentProcessors.get(provider);
        if (port == null) {
            throw new IllegalArgumentException("Chưa hỗ trợ cổng thanh toán: " + provider);
        }
        return port;
    }
}
