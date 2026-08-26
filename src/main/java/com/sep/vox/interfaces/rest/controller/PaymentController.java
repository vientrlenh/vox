package com.sep.vox.interfaces.rest.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sep.vox.application.port.input.command.CreatePaymentCheckoutUrlCommand;
import com.sep.vox.application.port.input.usecase.payment.CreatePaymentCheckoutUrlUseCase;
import com.sep.vox.application.response.input.payment.PaymentCheckoutResponse;
import com.sep.vox.interfaces.rest.dto.request.CreatePaymentCheckoutUrlRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final CreatePaymentCheckoutUrlUseCase createPaymentCheckoutUrlUseCase;

    public PaymentController(CreatePaymentCheckoutUrlUseCase createPaymentCheckoutUrlUseCase) {
        this.createPaymentCheckoutUrlUseCase = createPaymentCheckoutUrlUseCase;
    }

    /**
     * POST chứ không phải GET dù tên nghe như một phép đọc: mỗi lần gọi sẽ GHI một dòng
     * payment_records và gọi sang cổng để mở một phiên thanh toán thật. GET phải là thao tác an toàn
     * -- trình duyệt và proxy được phép prefetch, mà prefetch một endpoint như thế này nghĩa là tự
     * mở ra một lần thử thanh toán không ai yêu cầu.
     *
     * <p>201 vì kết quả là một tài nguyên MỚI (lần thử thanh toán) -- trừ trường hợp đơn đang có
     * phiên treo thì use case trả lại đúng phiên cũ.
     */
    @PostMapping("/checkout-url")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<PaymentCheckoutResponse>> createCheckoutUrl(
            @Valid @RequestBody CreatePaymentCheckoutUrlRequest request) {
        var data = createPaymentCheckoutUrlUseCase.execute(
            new CreatePaymentCheckoutUrlCommand(request.orderId(), request.provider()));
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Tạo liên kết thanh toán thành công", data));
    }
}
