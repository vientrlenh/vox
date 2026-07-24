package com.sep.vox.infrastructure.service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sep.vox.application.port.output.PayOSPort;
import com.sep.vox.application.response.output.PaymentLinkResult;

import vn.payos.PayOS;
import vn.payos.exception.WebhookException;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;

// Tự tính chữ ký thay vì gọi payOSClient.webhooks().verify(...) vì SDK payos-java 2.0.1 bind Map
// sang WebhookData (có field OffsetDateTime) bằng ObjectMapper nội bộ không có JavaTimeModule,
// luôn ném InvalidDefinitionException với mọi webhook thật (có transactionDateTime).
@Service
public class PayOSService implements PayOSPort {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final PayOS payOSClient;
    private final String checksumKey;
    private final String returnUrl;
    private final String cancelUrl;

    public PayOSService(
            PayOS payOSClient,
            @Value("${payos.checksum-key}") String checksumKey,
            @Value("${payos.return-url}") String returnUrl,
            @Value("${payos.cancel-url}") String cancelUrl) {
        this.payOSClient = payOSClient;
        this.checksumKey = checksumKey;
        this.returnUrl = returnUrl;
        this.cancelUrl = cancelUrl;
    }

    @Override
    public boolean verifyWebhookSignature(Map<String, Object> data, String signature) {
        return signature.equals(computeSignature(data));
    }

    @Override
    public PaymentLinkResult createPaymentLink(long orderCode, BigDecimal amount, String description) {
        var paymentLinkRequest = CreatePaymentLinkRequest.builder()
            .orderCode(orderCode)
            .amount(amount.longValueExact())
            .description(description)
            .returnUrl(returnUrl)
            .cancelUrl(cancelUrl)
            .build();
        var response = payOSClient.paymentRequests().create(paymentLinkRequest);
        return new PaymentLinkResult(response.getPaymentLinkId(), response.getCheckoutUrl());
    }

    private String computeSignature(Map<String, Object> data) {
        var sortedData = new TreeMap<>(data);
        var queryString = new StringBuilder();
        for (var entry : sortedData.entrySet()) {
            if (!queryString.isEmpty()) {
                queryString.append('&');
            }
            var value = entry.getValue();
            queryString.append(entry.getKey()).append('=').append(value == null ? "" : value.toString());
        }

        try {
            var mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(checksumKey.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            var hmacBytes = mac.doFinal(queryString.toString().getBytes(StandardCharsets.UTF_8));
            var hex = new StringBuilder();
            for (byte b : hmacBytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new WebhookException("Không thể tính chữ ký webhook", e);
        }
    }
}