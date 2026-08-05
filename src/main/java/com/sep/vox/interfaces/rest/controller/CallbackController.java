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

import com.sep.vox.application.port.input.service.CallbackHandlerService;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;

// Chuyển thẳng raw bytes xuống handler, không parse ở đây: mỗi cổng có định dạng payload và cách
// xác thực riêng (PayOS ký HMAC trên phần data trong body, SePay so secret key ở header), nên việc
// đọc payload thuộc về adapter của cổng đó. Parse ở tầng này còn làm hỏng chữ ký tính trên raw body.
@RestController
@RequestMapping("/api/v1/callback")
public class CallbackController {

    private final Map<String, CallbackHandlerService> callbackHandlerMap;

    public CallbackController(Map<String, CallbackHandlerService> callbackHandlerMap) {
        this.callbackHandlerMap = callbackHandlerMap;
    }

    @PostMapping("/payos/webhook")
    public ResponseEntity<ApiResponse<Void>> handlePayOsCallback(
            @RequestBody byte[] rawBody,
            @RequestHeader HttpHeaders headers) {
        resolveHandler("payosCallback").handle(rawBody, toSingleValueMap(headers));
        return ResponseEntity.ok(ApiResponse.success("OK"));
    }

    @PostMapping("/sepay/ipn")
    public ResponseEntity<ApiResponse<Void>> handleSePayCallback(
            @RequestBody byte[] rawBody,
            @RequestHeader HttpHeaders headers) {
        resolveHandler("sepayCallback").handle(rawBody, toSingleValueMap(headers));
        return ResponseEntity.ok(ApiResponse.success("OK"));
    }

    private CallbackHandlerService resolveHandler(String beanName) {
        var handler = callbackHandlerMap.get(beanName);
        if (handler == null) {
            throw new IllegalStateException("Callback handler đang không tồn tại: " + beanName);
        }
        return handler;
    }

    // Tên header HTTP không phân biệt hoa thường, nên adapter phải tra được "X-Secret-Key" kể cả
    // khi client gửi "x-secret-key".
    private Map<String, String> toSingleValueMap(HttpHeaders headers) {
        Map<String, String> singleValue = new LinkedCaseInsensitiveMap<>();
        singleValue.putAll(headers.toSingleValueMap());
        return singleValue;
    }
}
