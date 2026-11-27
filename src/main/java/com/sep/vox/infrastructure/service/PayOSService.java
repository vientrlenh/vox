package com.sep.vox.infrastructure.service;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sep.vox.application.port.output.PaymentProcessPort;
import com.sep.vox.application.response.output.PaymentLinkRemoteStatus;
import com.sep.vox.application.response.output.BankTransferDetails;
import com.sep.vox.application.response.output.CallbackVerificationResult;
import com.sep.vox.application.response.output.CreatePaymentLinkCommand;
import com.sep.vox.application.response.output.PaymentCheckoutResult;
import com.sep.vox.application.response.output.PaymentLinkStatusResult;
import com.sep.vox.domain.model.payment.PaymentProvider;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import vn.payos.PayOS;
import vn.payos.exception.NotFoundException;
import vn.payos.exception.WebhookException;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLink;
import vn.payos.model.v2.paymentRequests.PaymentLinkStatus;

// Tự tính chữ ký thay vì gọi payOSClient.webhooks().verify(...) vì SDK payos-java 2.0.1 bind Map
// sang WebhookData (có field Instant) bằng ObjectMapper nội bộ không có JavaTimeModule,
// luôn ném InvalidDefinitionException với mọi webhook thật (có transactionDateTime).
@Service
@Slf4j
public class PayOSService implements PaymentProcessPort {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String SUCCESS_CODE = "00";

    private final PayOS payOSClient;
    private final JsonMapper jsonMapper;
    private final String clientId;
    private final String apiKey;
    private final String checksumKey;
    private final String returnUrl;
    private final String cancelUrl;

    // clientId và apiKey KHÔNG được dùng ở đâu khác trong class này -- chúng đi vào PayOS client ở
    // PayOSConfig. Nhận thêm vào đây chỉ để isConfigured() trả lời được đầy đủ: cả năm giá trị đều
    // mặc định rỗng trong application.yaml, và new PayOS("", "", "") dựng bean thành công, nên
    // thiếu credential không lộ ra lúc khởi động mà chỉ lộ ra giữa một lần thanh toán thật.
    public PayOSService(
            PayOS payOSClient,
            JsonMapper jsonMapper,
            @Value("${payos.client-id}") String clientId,
            @Value("${payos.api-key}") String apiKey,
            @Value("${payos.checksum-key}") String checksumKey,
            @Value("${payos.return-url}") String returnUrl,
            @Value("${payos.cancel-url}") String cancelUrl) {
        this.payOSClient = payOSClient;
        this.jsonMapper = jsonMapper;
        this.clientId = clientId;
        this.apiKey = apiKey;
        this.checksumKey = checksumKey;
        this.returnUrl = returnUrl;
        this.cancelUrl = cancelUrl;
    }

    @Override
    public boolean isConfigured() {
        return hasText(clientId)
            && hasText(apiKey)
            && hasText(checksumKey)
            && hasText(returnUrl)
            && hasText(cancelUrl);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
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
        CreatePaymentLinkResponse response = payOSClient.paymentRequests().create(toPaymentLinkRequest(command));

        // PayOS returns BOTH the VietQR string and the bank transfer details, not only the checkoutUrl.
        // Keep them to show the code right inside the application: the user does not have to leave the page, and we do not lose
        // track of them midway. checkoutUrl still comes along as an exit for anyone who wants to use PayOS's page.
        //
        // Without qrCode we fall back to REDIRECT instead of building an empty QR page -- the FE does not have to guess, and
        // an SDK version without this field does not break the payment flow either.
        if (!hasText(response.getQrCode())) {
            log.warn("PayOS không trả về qrCode cho orderCode={} -- lùi về điều hướng sang trang checkout",
                command.orderRef());
            return PaymentCheckoutResult.redirect(response.getCheckoutUrl(), response.getPaymentLinkId());
        }

        return toQrCheckoutResult(command, response);
    }

    private CreatePaymentLinkRequest toPaymentLinkRequest(CreatePaymentLinkCommand command) {
        return CreatePaymentLinkRequest.builder()
            .orderCode(Long.parseLong(command.orderRef()))
            .amount(command.amount().longValueExact())
            .description(command.description())
            .returnUrl(returnUrl)
            .cancelUrl(cancelUrl)
            // The expiry comes from the ORDER, not from an adapter constant -- see CreatePaymentLinkCommand.expiresAt.
            .expiredAt(command.expiresAt().getEpochSecond())
            .build();
    }

    private PaymentCheckoutResult toQrCheckoutResult(
            CreatePaymentLinkCommand command, CreatePaymentLinkResponse response) {
        BankTransferDetails transfer = new BankTransferDetails(
            response.getBin(),
            response.getAccountNumber(),
            response.getAccountName(),
            command.amount(),
            // The transfer description must be what PayOS is CURRENTLY waiting for, not a description we set ourselves: it is
            // the key that matches the money to the order, one character off and the money arrives while the order stays pending.
            response.getDescription());

        return PaymentCheckoutResult.qr(
            response.getQrCode(), transfer, response.getCheckoutUrl(), response.getPaymentLinkId());
    }

    @Override
    public PaymentLinkStatusResult getPaymentLinkStatus(String providerOrderRef) {
        try {
            PaymentLink paymentLink = payOSClient.paymentRequests().get(Long.parseLong(providerOrderRef));
            return new PaymentLinkStatusResult(toRemoteStatus(paymentLink.getStatus()));
        } catch (NotFoundException e) {
            // Catch ONLY 404. Every other error (network lost, 5xx, call quota exhausted) is still thrown: the
            // caller must be able to tell "the gateway says there is no session" from "could not ask the gateway" -- mapping
            // the latter to NOT_FOUND would allow us to fail an attempt that may still be alive and about to
            // bring in money.
            log.warn("PayOS không có phiên thanh toán nào mang mã {} -- coi như lần thử đã chết",
                providerOrderRef, e);
            return new PaymentLinkStatusResult(PaymentLinkRemoteStatus.NOT_FOUND);
        }
    }

    @Override
    public boolean cancelPaymentLink(String providerOrderRef, String reason) {
        try {
            PaymentLink cancelled = payOSClient.paymentRequests().cancel(Long.parseLong(providerOrderRef), reason);
            // Trust the status PayOS RETURNS, not the fact that "the call did not throw": if for some reason
            // the link did not actually move to CANCELLED it can still collect money, while the caller
            // is waiting for a definitive answer before daring to close the order.
            boolean confirmed = toRemoteStatus(cancelled.getStatus()) == PaymentLinkRemoteStatus.CANCELLED;
            if (!confirmed) {
                log.warn("PayOS nhận lệnh hủy link {} nhưng trạng thái trả về là {}",
                    providerOrderRef, cancelled.getStatus());
            }
            return confirmed;
        } catch (Exception e) {
            // Do not throw: to the caller "cancel failed" and "the gateway does not support cancel" lead to the same
            // decision -- do not close the order. Throwing here would only turn a case that can be handled
            // into a 500 error for the user.
            log.warn("Không hủy được link thanh toán PayOS {}", providerOrderRef, e);
            return false;
        }
    }

    @Override
    public CallbackVerificationResult verifyCallback(byte[] rawBody, Map<String, String> headers) {
        Map<String, Object> body = parseBody(rawBody);
        if (body == null) {
            return CallbackVerificationResult.invalid();
        }

        Map<String, Object> data = signedDataOf(body);
        if (data == null) {
            return CallbackVerificationResult.invalid();
        }

        if (!(data.get("orderCode") instanceof Number orderCode)) {
            log.warn("Webhook PayOS có chữ ký hợp lệ nhưng thiếu orderCode");
            return CallbackVerificationResult.invalid();
        }

        return new CallbackVerificationResult(
            true,
            String.valueOf(orderCode.longValue()),
            callbackStatusOf(data),
            data.get("amount") instanceof Number amount ? BigDecimal.valueOf(amount.longValue()) : null
        );
    }

    // PayOS puts the signature in the body (not a header), computed over the "data" part. Returns null when the body has
    // no data/signature or the signature does not match.
    private @Nullable Map<String, Object> signedDataOf(Map<String, Object> body) {
        if (!(body.get("data") instanceof Map<?, ?> rawData) || !(body.get("signature") instanceof String signature)) {
            return null;
        }
        Map<String, Object> data = copyStringKeyedEntries(rawData);
        return verifySignature(data, signature) ? data : null;
    }

    private Map<String, Object> copyStringKeyedEntries(Map<?, ?> rawData) {
        Map<String, Object> data = new HashMap<String, Object>(rawData.size());
        for (Map.Entry<?, ?> entry : rawData.entrySet()) {
            if (entry.getKey() instanceof String key) {
                data.put(key, entry.getValue());
            }
        }
        return data;
    }

    // The PayOS webhook only reports success/failure through the code, without distinguishing the specific reason for failure.
    private PaymentLinkRemoteStatus callbackStatusOf(Map<String, Object> data) {
        return SUCCESS_CODE.equals(data.get("code"))
            ? PaymentLinkRemoteStatus.PAID
            : PaymentLinkRemoteStatus.FAILED;
    }

    // Decode UTF-8 kiểu lenient (thay ký tự lỗi thay vì crash) vì payload test/webhook của PayOS
    // đôi khi chứa byte UTF-8 không hợp lệ ở phần text tiếng Việt (desc), làm Jackson mặc định
    // ném StreamReadException. Trả null nếu payload không dùng được.
    private Map<String, Object> parseBody(byte[] rawBody) {
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPLACE)
            .onUnmappableCharacter(CodingErrorAction.REPLACE);
        try {
            String json = decoder.decode(ByteBuffer.wrap(rawBody)).toString();
            return jsonMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (CharacterCodingException | RuntimeException e) {
            log.warn("Không đọc được payload webhook PayOS", e);
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
        TreeMap<String, Object> sortedData = new TreeMap<>(data);
        StringBuilder queryString = new StringBuilder();
        for (Map.Entry<String, Object> entry : sortedData.entrySet()) {
            if (!queryString.isEmpty()) {
                queryString.append('&');
            }
            Object value = entry.getValue();
            queryString.append(entry.getKey()).append('=').append(value == null ? "" : value.toString());
        }

        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(checksumKey.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] hmacBytes = mac.doFinal(queryString.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hmacBytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new WebhookException("Không thể tính chữ ký webhook", e);
        }
    }
}
