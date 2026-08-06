package com.sep.vox.interfaces.rest.controller;

import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sep.vox.application.port.input.usecase.subscription.ProcessPaymentCallbackUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ProcessPaymentCallbackUseCase.CallbackOutcome;
import com.sep.vox.domain.model.subscription.PaymentMethod;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;

// Chuyển thẳng raw bytes xuống use case, không parse ở đây: mỗi cổng có định dạng payload và cách
// xác thực riêng (PayOS ký HMAC trên phần data trong body, SePay so secret key ở header), nên việc
// đọc payload thuộc về adapter của cổng đó. Parse ở tầng này còn làm hỏng chữ ký tính trên raw body.
@RestController
@RequestMapping("/api/v1/callback")
public class CallbackController {

    private final ProcessPaymentCallbackUseCase processPaymentCallbackUseCase;

    public CallbackController(ProcessPaymentCallbackUseCase processPaymentCallbackUseCase) {
        this.processPaymentCallbackUseCase = processPaymentCallbackUseCase;
    }

    @PostMapping("/payos/webhook")
    public ResponseEntity<ApiResponse<Void>> handlePayOsCallback(
            @RequestBody byte[] rawBody,
            @RequestHeader HttpHeaders headers) {
        return respond(PaymentMethod.PAYOS, rawBody, headers);
    }

    @PostMapping("/sepay/ipn")
    public ResponseEntity<ApiResponse<Void>> handleSePayCallback(
            @RequestBody byte[] rawBody,
            @RequestHeader HttpHeaders headers) {
        return respond(PaymentMethod.SEPAY, rawBody, headers);
    }

    // Mọi kết quả đã xác thực đều trả 200: cổng thanh toán dùng mã HTTP để quyết định có gọi lại
    // hay không (SePay retry tới 7 lần trong ~5 giờ), mà gọi lại cùng một payload cũng ra đúng kết
    // quả đó. Chỉ callback KHÔNG xác thực được mới thoát ra thành 401 qua UnauthorizedException.
    private ResponseEntity<ApiResponse<Void>> respond(
            PaymentMethod provider, byte[] rawBody, HttpHeaders headers) {
        var outcome = processPaymentCallbackUseCase.execute(provider, rawBody, toSingleValueMap(headers));
        return ResponseEntity.ok(ApiResponse.success(messageOf(outcome)));
    }

    private String messageOf(CallbackOutcome outcome) {
        return switch (outcome) {
            case SETTLED -> "OK";
            case UNKNOWN_ORDER -> "Không tìm thấy hóa đơn tương ứng";
            case AMOUNT_MISMATCH -> "Số tiền không khớp hóa đơn";
            case NOT_FINAL -> "Giao dịch chưa ở trạng thái cuối";
        };
    }

    // Tên header HTTP không phân biệt hoa thường, nên adapter phải tra được "X-Secret-Key" kể cả
    // khi client gửi "x-secret-key".
    private Map<String, String> toSingleValueMap(HttpHeaders headers) {
        Map<String, String> singleValue = new LinkedCaseInsensitiveMap<>();
        singleValue.putAll(headers.toSingleValueMap());
        return singleValue;
    }
}
