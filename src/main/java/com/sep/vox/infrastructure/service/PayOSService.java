package com.sep.vox.infrastructure.service;

import java.time.Instant;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sep.vox.application.port.output.PaymentProcessPort;
import com.sep.vox.application.response.output.PaymentLinkRemoteStatus;
import com.sep.vox.application.response.output.CallbackVerificationResult;
import com.sep.vox.application.response.output.CreatePaymentLinkCommand;
import com.sep.vox.application.response.output.PaymentCheckoutResult;
import com.sep.vox.application.response.output.PaymentLinkStatusResult;
import com.sep.vox.domain.model.payment.PaymentProvider;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import vn.payos.PayOS;
import vn.payos.exception.WebhookException;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.PaymentLinkStatus;

// Tự tính chữ ký thay vì gọi payOSClient.webhooks().verify(...) vì SDK payos-java 2.0.1 bind Map
// sang WebhookData (có field Instant) bằng ObjectMapper nội bộ không có JavaTimeModule,
// luôn ném InvalidDefinitionException với mọi webhook thật (có transactionDateTime).
@Service
public class PayOSService implements PaymentProcessPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(PayOSService.class);

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String SUCCESS_CODE = "00";

    // Đủ ngắn để đưa hóa đơn PENDING treo mãi về trạng thái cuối trong một khoảng thời gian xác định,
    // và ngắn hơn chu kỳ quét của PendingOrderReconciler (5 phút) để lần quét kế tiếp bắt kịp ngay.
    private static final long PAYMENT_LINK_EXPIRATION_MINUTES = 3;

    private final PayOS payOSClient;
    private final JsonMapper jsonMapper;
    private final String checksumKey;
    private final String returnUrl;
    private final String cancelUrl;

    public PayOSService(
            PayOS payOSClient,
            JsonMapper jsonMapper,
            @Value("${payos.checksum-key}") String checksumKey,
            @Value("${payos.return-url}") String returnUrl,
            @Value("${payos.cancel-url}") String cancelUrl) {
        this.payOSClient = payOSClient;
        this.jsonMapper = jsonMapper;
        this.checksumKey = checksumKey;
        this.returnUrl = returnUrl;
        this.cancelUrl = cancelUrl;
    }

    @Override
    public PaymentProvider provider() {
        return PaymentProvider.PAYOS;
    }

    /**
     * PayOS bắt buộc orderCode là số nên không dùng lại được invoiceNumber dạng chuỗi. Sinh từ mốc
     * thời gian mili giây nhân 1000 cộng phần ngẫu nhiên: đủ tăng dần để dễ tra theo thời gian, và
     * phần ngẫu nhiên chặn hai hóa đơn tạo trong cùng một mili giây trùng mã.
     */
    @Override
    public String newOrderRef() {
        return String.valueOf(System.currentTimeMillis() * 1000 + ThreadLocalRandom.current().nextInt(1000));
    }

    @Override
    public PaymentCheckoutResult createPaymentLink(CreatePaymentLinkCommand command) {
        var paymentLinkRequest = CreatePaymentLinkRequest.builder()
            .orderCode(Long.parseLong(command.orderRef()))
            .amount(command.amount().longValueExact())
            .description(command.description())
            .returnUrl(returnUrl)
            .cancelUrl(cancelUrl)
            // Hạn lấy từ ĐƠN chứ không phải hằng số của adapter -- xem CreatePaymentLinkCommand.expiresAt.
            .expiredAt(command.expiresAt().getEpochSecond())
            .build();
        var response = payOSClient.paymentRequests().create(paymentLinkRequest);
        return PaymentCheckoutResult.redirect(response.getCheckoutUrl(), response.getPaymentLinkId());
    }

    @Override
    public PaymentLinkStatusResult getPaymentLinkStatus(String providerOrderRef) {
        var paymentLink = payOSClient.paymentRequests().get(Long.parseLong(providerOrderRef));
        return new PaymentLinkStatusResult(toRemoteStatus(paymentLink.getStatus()));
    }

    @Override
    public CallbackVerificationResult verifyCallback(byte[] rawBody, Map<String, String> headers) {
        var body = parseBody(rawBody);
        if (body == null) {
            return CallbackVerificationResult.invalid();
        }

        // PayOS đặt chữ ký trong body (không phải header), tính trên phần "data".
        if (!(body.get("data") instanceof Map<?, ?> rawData) || !(body.get("signature") instanceof String signature)) {
            return CallbackVerificationResult.invalid();
        }

        var data = new HashMap<String, Object>(rawData.size());
        for (var entry : rawData.entrySet()) {
            if (entry.getKey() instanceof String key) {
                data.put(key, entry.getValue());
            }
        }

        if (!verifySignature(data, signature)) {
            return CallbackVerificationResult.invalid();
        }

        if (!(data.get("orderCode") instanceof Number orderCode)) {
            LOGGER.warn("Webhook PayOS có chữ ký hợp lệ nhưng thiếu orderCode");
            return CallbackVerificationResult.invalid();
        }

        // Webhook PayOS chỉ báo thành công/thất bại qua code, không phân biệt lý do thất bại cụ thể.
        var status = SUCCESS_CODE.equals(data.get("code"))
            ? PaymentLinkRemoteStatus.PAID
            : PaymentLinkRemoteStatus.FAILED;

        return new CallbackVerificationResult(
            true,
            String.valueOf(orderCode.longValue()),
            status,
            data.get("amount") instanceof Number amount ? BigDecimal.valueOf(amount.longValue()) : null
        );
    }

    // Decode UTF-8 kiểu lenient (thay ký tự lỗi thay vì crash) vì payload test/webhook của PayOS
    // đôi khi chứa byte UTF-8 không hợp lệ ở phần text tiếng Việt (desc), làm Jackson mặc định
    // ném StreamReadException. Trả null nếu payload không dùng được.
    private Map<String, Object> parseBody(byte[] rawBody) {
        var decoder = StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPLACE)
            .onUnmappableCharacter(CodingErrorAction.REPLACE);
        try {
            var json = decoder.decode(ByteBuffer.wrap(rawBody)).toString();
            return jsonMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (CharacterCodingException | RuntimeException e) {
            LOGGER.warn("Không đọc được payload webhook PayOS", e);
            return null;
        }
    }

    // Switch tường minh thay vì valueOf(status.name()): nếu SDK PayOS thêm trạng thái mới, ta muốn
    // biết bằng lỗi compile ở đây chứ không phải IllegalArgumentException giữa lúc đối soát tiền.
    private PaymentLinkRemoteStatus toRemoteStatus(PaymentLinkStatus status) {
        return switch (status) {
            case PENDING -> PaymentLinkRemoteStatus.PENDING;
            case PROCESSING -> PaymentLinkRemoteStatus.PROCESSING;
            case UNDERPAID -> PaymentLinkRemoteStatus.UNDERPAID;
            case PAID -> PaymentLinkRemoteStatus.PAID;
            case CANCELLED -> PaymentLinkRemoteStatus.CANCELLED;
            case EXPIRED -> PaymentLinkRemoteStatus.EXPIRED;
            case FAILED -> PaymentLinkRemoteStatus.FAILED;
        };
    }

    // So sánh constant-time: String.equals thoát sớm ở byte lệch đầu tiên, để lộ thông tin cho
    // kiểu tấn công dò chữ ký theo thời gian phản hồi.
    boolean verifySignature(Map<String, Object> data, String signature) {
        return MessageDigest.isEqual(
            signature.getBytes(StandardCharsets.UTF_8),
            computeSignature(data).getBytes(StandardCharsets.UTF_8)
        );
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
