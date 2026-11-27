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

// Computes the signature itself instead of calling payOSClient.webhooks().verify(...) because the payos-java 2.0.1 SDK binds Map
// to WebhookData (which has an Instant field) with an internal ObjectMapper that has no JavaTimeModule,
// and always throws InvalidDefinitionException for every real webhook (which carries transactionDateTime).
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

    // clientId and apiKey are NOT used anywhere else in this class -- they go into the PayOS client in
    // PayOSConfig. They are accepted here only so isConfigured() can answer completely: all five values
    // default to empty in application.yaml, and new PayOS("", "", "") builds the bean successfully, so
    // missing credentials do not show up at startup but only in the middle of a real payment.
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
     * PayOS requires orderCode to be a number so the string invoiceNumber cannot be reused. Generated from the millisecond
     * timestamp times 1000 plus a random part: increasing enough to look up by time, and the
     * random part stops two invoices created in the same millisecond from colliding on code.
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
            log.warn("PayOS did not return a qrCode for orderCode={} -- falling back to redirecting to the checkout page",
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
            log.warn("PayOS has no payment session with code {} -- treating the attempt as dead",
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
                log.warn("PayOS accepted the cancel command for link {} but the returned status is {}",
                    providerOrderRef, cancelled.getStatus());
            }
            return confirmed;
        } catch (Exception e) {
            // Do not throw: to the caller "cancel failed" and "the gateway does not support cancel" lead to the same
            // decision -- do not close the order. Throwing here would only turn a case that can be handled
            // into a 500 error for the user.
            log.warn("Could not cancel the PayOS payment link {}", providerOrderRef, e);
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
            log.warn("PayOS webhook has a valid signature but is missing orderCode");
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

    // Lenient UTF-8 decoding (replace bad characters instead of crashing) because PayOS test/webhook payloads
    // sometimes contain invalid UTF-8 bytes in the Vietnamese text part (desc), which makes the default Jackson
    // throw StreamReadException. Returns null if the payload is unusable.
    private Map<String, Object> parseBody(byte[] rawBody) {
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPLACE)
            .onUnmappableCharacter(CodingErrorAction.REPLACE);
        try {
            String json = decoder.decode(ByteBuffer.wrap(rawBody)).toString();
            return jsonMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (CharacterCodingException | RuntimeException e) {
            log.warn("Could not read the PayOS webhook payload", e);
            return null;
        }
    }

    // An explicit switch instead of valueOf(status.name()): if the PayOS SDK adds a new status, we want to
    // find out through a compile error here and not an IllegalArgumentException in the middle of money reconciliation.
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

    // Constant-time comparison: String.equals exits at the first differing byte, leaking information to
    // signature-probing attacks based on response time.
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
            throw new WebhookException("Cannot compute the webhook signature", e);
        }
    }
}
