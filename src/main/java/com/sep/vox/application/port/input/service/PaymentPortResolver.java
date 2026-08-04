package com.sep.vox.application.port.input.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.output.PaymentPort;
import com.sep.vox.domain.model.subscription.PaymentMethod;

// Chọn đúng PaymentPort (PayOS, hoặc provider mới sau này) theo PaymentMethod, thay vì use case
// inject thẳng 1 implementation cụ thể. Spring tự gom mọi bean implement PaymentPort vào List —
// thêm provider mới chỉ cần thêm 1 class @Service implements PaymentPort, không phải sửa gì ở đây
// hay ở các use case đang gọi resolve(...).
@Service
public class PaymentPortResolver {

    private final Map<PaymentMethod, PaymentPort> portsByMethod;

    public PaymentPortResolver(List<PaymentPort> paymentPorts) {
        this.portsByMethod = paymentPorts.stream()
            .collect(Collectors.toMap(PaymentPort::supports, Function.identity()));
    }

    public PaymentPort resolve(PaymentMethod method) {
        var port = portsByMethod.get(method);
        if (port == null) {
            throw new NotFoundException("Chưa hỗ trợ cổng thanh toán: " + method);
        }
        return port;
    }
}
