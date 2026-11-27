package com.sep.vox.interfaces.rest.controller;

import com.sep.vox.application.response.input.payment.PaymentCallbackResponse;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sep.vox.application.port.input.command.payment.ProcessPaymentCallbackCommand;
import com.sep.vox.application.port.input.usecase.payment.ProcessPaymentCallbackUseCase;
import com.sep.vox.application.response.input.payment.PaymentCallbackResponse.CallbackOutcome;
import com.sep.vox.domain.model.payment.PaymentProvider;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;

// Passes the raw bytes straight down to the use case and does not parse them here: each gateway has
// its own payload format and its own authentication (PayOS signs the data part of the body with
// HMAC, SePay compares a secret key in a header), so reading the payload belongs to that gateway's
// adapter. Parsing in this layer would also break the signature, which is computed over the raw body.
//
// There is NO @PreAuthorize: the caller is a payment gateway, not a logged-in user. Authentication
// happens in the adapter's verifyCallback (the gateway's signature/secret) -- see SecurityConfig for
// where /api/v1/callback/** is opened up.
@RestController
@RequestMapping("/api/v1/callback")
@RequiredArgsConstructor
public class RestCallbackController {

    private final ProcessPaymentCallbackUseCase processPaymentCallbackUseCase;

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

    // Every authenticated outcome returns 200: the gateway uses the HTTP status to decide whether to call back, and
    // calling back with the same payload yields the same outcome anyway. Only a callback that CANNOT be authenticated
    // escapes as 401 through UnauthorizedException.
    //
    // It must be exactly 200, not just any 2xx: the SePay Payment Gateway IPN documentation states
    // "endpoint must return HTTP status code 200". PayOS also accepts 200 (the sample SDK returns 200 "OK").
    // That is why ResponseEntity.ok is used rather than 202/204 for the "nothing settled" branches.
    private ResponseEntity<ApiResponse<Void>> respond(
            PaymentProvider provider, byte[] rawBody, HttpHeaders headers) {
        PaymentCallbackResponse result = processPaymentCallbackUseCase.execute(
            new ProcessPaymentCallbackCommand(provider, rawBody, toSingleValueMap(headers)));
        return ResponseEntity.ok(ApiResponse.success(messageOf(result.outcome())));
    }

    private String messageOf(CallbackOutcome outcome) {
        return switch (outcome) {
            case SETTLED -> "OK";
            case ALREADY_SETTLED -> "The payment attempt was already settled earlier";
            // Still 200 like every other authenticated branch: calling back yields the same outcome, and making the gateway
            // retry a case that needs manual handling only adds noise. The warning is in the ERROR log of
            // ProcessPaymentCallbackUseCase, not in the HTTP status.
            case PAID_AFTER_WRITE_OFF -> "Credit recorded for an already closed payment attempt, reconciliation needed";
            case UNKNOWN_PAYMENT -> "No matching payment attempt found";
            case AMOUNT_MISMATCH -> "Amount does not match the payment attempt";
            case NOT_FINAL -> "Transaction is not in a final status";
        };
    }

    // HTTP header names are case-insensitive, so the adapter must be able to look up "X-Secret-Key" even
    // when the client sends "x-secret-key".
    private Map<String, String> toSingleValueMap(HttpHeaders headers) {
        Map<String, String> singleValue = new LinkedCaseInsensitiveMap<>();
        singleValue.putAll(headers.toSingleValueMap());
        return singleValue;
    }
}
