package com.sep.vox.infrastructure.service;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sep.vox.application.port.output.PaymentProcessPort;
import com.sep.vox.application.response.output.PaymentLinkResult;
import com.sep.vox.application.response.output.PaymentLinkStatusResult;
import com.sep.vox.domain.model.subscription.PaymentMethod;
import com.sep.vox.infrastructure.properties.SePayPaymentProperties;

@Service
public class SePayPaymentProcessService implements PaymentProcessPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(SePayPaymentProcessService.class);

    // SePay xác thực IPN bằng secret key gửi kèm ở header, không ký HMAC trên body — đây chính là
    // lý do verifyCallback phải nhận headers chứ không chỉ nhận body.
    private static final String SECRET_KEY_HEADER = "X-Secret-Key";

    private static final List<String> SIGNED_FIELDS = List.of(
        "order_amount",
        "merchant",
        "currency",
        "operation",
        "order_description",
        "order_invoice_number",
        "customer_id",
        "payment_method",
        "success_url",
        "error_url",
        "cancel_url"
    );

    private static final String HMAC = "HmacSHA256";

    private final SePayPaymentProperties properties;

    public SePayPaymentProcessService(SePayPaymentProperties properties) {
        this.properties = properties;
    }

    @Override
    public PaymentMethod provider() {
        return PaymentMethod.SEPAY;
    }

    @Override
    public PaymentLinkResult createPaymentLink(CreatePaymentLinkCommand command) {
        var fields = new LinkedHashMap<String, String>();
        fields.put("order_amount", command.amount().toPlainString());
        fields.put("merchant", properties.merchantId());
        fields.put("currency", "VND");
        fields.put("operation", "PURCHASE");
        fields.put("order_description", command.description());
        fields.put("order_invoice_number", command.orderRef());
        fields.put("success_url", properties.returnBaseUrl() + "/payment/success");
        fields.put("error_url", properties.returnBaseUrl() + "/payment/error");
        fields.put("cancel_url", properties.returnBaseUrl() + "/payment/cancel");
        fields.put("signature", sign(fields));
        // TODO(Phase 5): POST fields tới properties.checkoutUrl() bằng RestClient rồi lấy
        // checkoutUrl + id đơn từ response, thay vì trả thẳng URL init như hiện tại.
        return new PaymentLinkResult(null, properties.checkoutUrl());
    }

    @Override
    public PaymentLinkStatusResult getPaymentLinkStatus(String providerOrderRef) {
        // TODO(Phase 5): GET trạng thái đơn từ properties.apiBaseUrl().
        throw new UnsupportedOperationException("Chưa hỗ trợ tra trạng thái đơn SePay");
    }

    @Override
    public CallbackVerificationResult verifyCallback(byte[] rawBody, Map<String, String> headers) {
        if (!hasValidSecretKey(headers)) {
            return CallbackVerificationResult.invalid();
        }
        // TODO(Phase 5): parse rawBody thành SePayIpnPayload, lấy order.orderId làm providerOrderRef
        // và transaction.transactionAmount làm amount. Phần còn phải chốt là ánh xạ
        // order.orderStatus / transaction.transactionStatus sang PaymentLinkRemoteStatus — cần đúng
        // chuỗi trạng thái SePay gửi, vì đoán sai ở đây là chốt nhầm tiền.
        throw new UnsupportedOperationException("Chưa hỗ trợ xác thực callback SePay");
    }

    // So sánh constant-time để không lộ secret qua thời gian phản hồi.
    private boolean hasValidSecretKey(Map<String, String> headers) {
        var provided = headers.get(SECRET_KEY_HEADER);
        if (provided == null) {
            LOGGER.warn("IPN SePay thiếu header {}", SECRET_KEY_HEADER);
            return false;
        }
        return MessageDigest.isEqual(
            provided.getBytes(StandardCharsets.UTF_8),
            properties.secretKey().getBytes(StandardCharsets.UTF_8)
        );
    }

    private String sign(Map<String, String> fields) {
        var data = SIGNED_FIELDS.stream()
            .filter(fields::containsKey)
            .map(f -> f + "=" + fields.get(f))
            .collect(Collectors.joining(","));
        try {
            var mac = Mac.getInstance(HMAC);
            mac.init(getSecretKey());
            return Base64.getEncoder().encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            LOGGER.error("Error when signing SePay: {}", e);
            throw new IllegalStateException("Lỗi ký chữ ký cho SePay");
        }
    }

    private SecretKey getSecretKey() {
        return new SecretKeySpec(properties.secretKey().getBytes(StandardCharsets.UTF_8), HMAC);
    }

    private record SePayIpnPayload(
        Integer timestamp,
        String notificationType,
        SePayIpnOrderPayload order,
        SePayIpnTransactionPayload transaction,
        SePayIpnCustomerPayload customer
    ) {
        public record SePayIpnOrderPayload(
            UUID id,
            String orderId,
            String orderStatus,
            String orderCurrency,
            String orderAmount,
            String[] customData,
            String userAgent,
            String ipAddress,
            String orderDescription
        ){}

        public record SePayIpnTransactionPayload(
            UUID id,
            String paymentMethod,
            String transactionId,
            String transactionType,
            String transactionDate,
            String transactionStatus,
            String transactionAmount,
            String transactionCurrency
        ){}

        public record SePayIpnCustomerPayload(
            UUID id,
            String customerId
        ){}

    }
}
