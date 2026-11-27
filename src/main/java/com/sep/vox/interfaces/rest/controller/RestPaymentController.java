package com.sep.vox.interfaces.rest.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sep.vox.application.port.input.command.payment.CreatePaymentCheckoutUrlCommand;
import com.sep.vox.application.port.input.usecase.payment.CreatePaymentCheckoutUrlUseCase;
import com.sep.vox.application.response.input.payment.PaymentCheckoutResponse;
import com.sep.vox.interfaces.rest.dto.request.payment.CreatePaymentCheckoutUrlRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class RestPaymentController {

    private final CreatePaymentCheckoutUrlUseCase createPaymentCheckoutUrlUseCase;

    /**
     * POST rather than GET even though the name sounds like a read: every call WRITES a payment_records
     * row and calls the gateway to open a real payment session. GET must be a safe operation
     * -- browsers and proxies may prefetch, and prefetching an endpoint like this would open
     * a payment attempt nobody asked for.
     *
     * <p>201 because the result is a NEW resource (the payment attempt) -- unless the order already has a pending
     * session, in which case the use case returns that same session.
     */
    @PostMapping("/checkout-url")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<PaymentCheckoutResponse>> createCheckoutUrl(
            @Valid @RequestBody CreatePaymentCheckoutUrlRequest request) {
        PaymentCheckoutResponse data = createPaymentCheckoutUrlUseCase.execute(
            new CreatePaymentCheckoutUrlCommand(request.orderId(), request.provider()));
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Payment link created successfully", data));
    }
}
