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

import com.sep.vox.application.port.input.command.ProcessPaymentCallbackCommand;
import com.sep.vox.application.port.input.usecase.payment.ProcessPaymentCallbackUseCase;
import com.sep.vox.application.response.input.payment.PaymentCallbackResponse.CallbackOutcome;
import com.sep.vox.domain.model.payment.PaymentProvider;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;

// Chuyển thẳng raw bytes xuống use case, không parse ở đây: mỗi cổng có định dạng payload và cách
// xác thực riêng (PayOS ký HMAC trên phần data trong body, SePay so secret key ở header), nên việc
// đọc payload thuộc về adapter của cổng đó. Parse ở tầng này còn làm hỏng chữ ký tính trên raw body.
//
// KHÔNG có @PreAuthorize: người gọi là cổng thanh toán, không phải người dùng đã đăng nhập. Việc
// xác thực nằm ở verifyCallback của adapter (chữ ký/secret của cổng) -- xem SecurityConfig cho
// phần mở đường /api/v1/callback/**.
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
        return respond(PaymentProvider.PAYOS, rawBody, headers);
    }

    @PostMapping("/sepay/ipn")
    public ResponseEntity<ApiResponse<Void>> handleSePayCallback(
            @RequestBody byte[] rawBody,
            @RequestHeader HttpHeaders headers) {
        return respond(PaymentProvider.SEPAY, rawBody, headers);
    }

    // Mọi kết quả đã xác thực đều trả 200: cổng dùng mã HTTP để quyết định có gọi lại hay không, mà
    // gọi lại cùng một payload cũng ra đúng kết quả đó. Chỉ callback KHÔNG xác thực được mới thoát
    // ra thành 401 qua UnauthorizedException.
    //
    // Phải đúng 200, không phải một mã 2xx bất kỳ: tài liệu IPN của SePay Payment Gateway ghi rõ
    // "endpoint must return HTTP status code 200". PayOS cũng nhận 200 (SDK mẫu trả 200 "OK").
    // Đây là lý do dùng ResponseEntity.ok chứ không phải 202/204 cho các nhánh "chưa chốt gì".
    private ResponseEntity<ApiResponse<Void>> respond(
            PaymentProvider provider, byte[] rawBody, HttpHeaders headers) {
        var result = processPaymentCallbackUseCase.execute(
            new ProcessPaymentCallbackCommand(provider, rawBody, toSingleValueMap(headers)));
        return ResponseEntity.ok(ApiResponse.success(messageOf(result.outcome())));
    }

    private String messageOf(CallbackOutcome outcome) {
        return switch (outcome) {
            case SETTLED -> "OK";
            case ALREADY_SETTLED -> "Lần thanh toán đã được chốt trước đó";
            case UNKNOWN_PAYMENT -> "Không tìm thấy lần thanh toán tương ứng";
            case AMOUNT_MISMATCH -> "Số tiền không khớp lần thanh toán";
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
