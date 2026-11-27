package com.sep.vox.infrastructure.service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.Set;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sep.vox.application.port.output.PaymentProcessPort;
import com.sep.vox.application.response.output.CallbackVerificationResult;
import com.sep.vox.application.response.output.CreatePaymentLinkCommand;
import com.sep.vox.application.response.output.PaymentCheckoutResult;
import com.sep.vox.application.response.output.PaymentLinkRemoteStatus;
import com.sep.vox.application.response.output.PaymentLinkStatusResult;
import com.sep.vox.domain.model.payment.PaymentProvider;
import com.sep.vox.infrastructure.properties.SePayPaymentProperties;

import tools.jackson.databind.json.JsonMapper;

/**
 * Adapter for the SePay Payment Gateway.
 *
 * <p>A completely different flow from PayOS: SePay does not create a dedicated link for each order through an API. We build a
 * set of fields ourselves with an HMAC signature and let the browser POST to a fixed checkout URL -- so
 * {@code createPaymentLink} here makes no network call at all, and has no paymentLinkId to return.
 */
@Service
@Slf4j
public class SePayPaymentProcessService implements PaymentProcessPort {

    // SePay authenticates the IPN with a secret key sent in a header, not an HMAC over the body -- this is exactly the
    // reason verifyCallback must receive headers and not only the body.
    private static final String SECRET_KEY_HEADER = "X-Secret-Key";

    private static final String HMAC = "HmacSHA256";
    private static final String CURRENCY = "VND";
    private static final String OPERATION_PURCHASE = "PURCHASE";

    // SePay signs a string of "key=value" pairs joined by commas, so a comma inside a
    // value would make the signing string ambiguous and the two sides' signatures diverge.
    private static final char SIGNATURE_SEPARATOR = ',';

    // Only a SET used for filtering, not the signing order: the order comes from the insertion order into the LinkedHashMap
    // in buildCheckoutFields(). Keeping two parallel ordered lists is a sure way for someone one day
    // to fix one place and forget the other, and then every transaction fails the signature.
    private static final Set<String> SIGNED_FIELDS = Set.of(
        "merchant",
        "env",
        "operation",
        "payment_method",
        "order_amount",
        "currency",
        "order_invoice_number",
        "order_description",
        "customer_id",
        "agreement_id",
        "agreement_name",
        "agreement_type",
        "agreement_payment_frequency",
        "agreement_amount_per_payment",
        "success_url",
        "error_url",
        "cancel_url",
        "order_id"
    );

    private static final String NOTIFICATION_ORDER_PAID = "ORDER_PAID";
    private static final String NOTIFICATION_TRANSACTION_VOID = "TRANSACTION_VOID";

    // The reconciliation job runs on the scheduler thread and calls invoices one by one, so one hanging request
    // hangs the whole sweep -- the timeout here is not a precaution but a condition for the job to run on schedule.
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(15);

    private final SePayPaymentProperties properties;
    private final JsonMapper jsonMapper;
    private final RestClient restClient;

    public SePayPaymentProcessService(SePayPaymentProperties properties, JsonMapper jsonMapper) {
        this.properties = properties;
        this.jsonMapper = jsonMapper;
        this.restClient = RestClient.builder()
            .baseUrl(properties.apiBaseUrl())
            .requestFactory(requestFactory())
            .defaultHeader(HttpHeaders.AUTHORIZATION, basicAuth(properties))
            .build();
    }

    private static ClientHttpRequestFactory requestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT);
        factory.setReadTimeout(READ_TIMEOUT);
        return factory;
    }

    @Override
    public boolean isConfigured() {
        return properties.isConfigured();
    }

    @Override
    public PaymentProvider provider() {
        return PaymentProvider.SEPAY;
    }

    /**
     * SePay identifies an order by the string {@code order_invoice_number} and only requires it to be unique.
     * It used to use the invoice number directly, but an invoice is now only issued AFTER the money arrives, so when the link is issued
     * there is no number yet -- generated in place, with the VOX prefix so the SePay dashboard shows at a glance that it is our order.
     */
    @Override
    public String newOrderRef() {
        return "VOX-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24).toUpperCase();
    }

    @Override
    public PaymentCheckoutResult createPaymentLink(CreatePaymentLinkCommand command) {
        requireConfigured();
        Map<String, String> fields = buildCheckoutFields(command);
        fields.put("signature", sign(fields));
        return PaymentCheckoutResult.formPost(properties.checkoutUrl(), fields);
    }

    /**
     * The insertion order here IS the signing order, and it follows exactly the order in which the official SDKs
     * (sepay-pg-php {@code prepareFormFields}, sepay-pg-node {@code signFields}) build the field set:
     * merchant, currency, order_amount, operation, order_description, and only then the optional group.
     * Both SDKs sign by the element order of the array and do not sort, so this is a real constraint, not
     * an aesthetic one — swap two lines and every transaction is rejected by SePay for a wrong signature.
     */
    private LinkedHashMap<String, String> buildCheckoutFields(CreatePaymentLinkCommand command) {
        LinkedHashMap<String, String> fields = new LinkedHashMap<String, String>();
        fields.put("merchant", properties.merchantId());
        fields.put("currency", CURRENCY);
        fields.put("order_amount", formatAmount(command.amount()));
        fields.put("operation", OPERATION_PURCHASE);
        fields.put("order_description", sanitizeForSignature(command.description()));
        fields.put("payment_method", properties.paymentMethod().name());
        fields.put("order_invoice_number", command.orderRef());
        fields.put("success_url", properties.returnBaseUrl() + "/payment/success");
        fields.put("error_url", properties.returnBaseUrl() + "/payment/error");
        fields.put("cancel_url", properties.returnBaseUrl() + "/payment/cancel");
        return fields;
    }

    /**
     * VND has no fractional part and the official SDK forces the amount to an integer string. A BigDecimal read from a
     * numeric(_,2) column gives {@code toPlainString()} = "10000.00" -- which both mismatches the signature and risks
     * SePay interpreting it as a different amount.
     */
    private String formatAmount(BigDecimal amount) {
        return String.valueOf(amount.longValueExact());
    }

    /**
     * A comma is the separator of the signing string, so letting it into a value breaks our own signature.
     * Replaced with a space instead of throwing: an order description with one character changed is still better than blocking
     * a valid transaction outright.
     */
    private String sanitizeForSignature(String value) {
        if (value == null) {
            return "";
        }
        if (value.indexOf(SIGNATURE_SEPARATOR) < 0) {
            return value;
        }
        log.warn("SePay order description contains a comma — replaced with a space so the signature is not broken");
        return value.replace(SIGNATURE_SEPARATOR, ' ');
    }

    @Override
    public PaymentLinkStatusResult getPaymentLinkStatus(String providerOrderRef) {
        requireConfigured();
        // SePay's rate limit is only a few requests/second (429 with the X-SePay-UserApi-Retry-After header),
        // so the caller — PendingOrderReconciler — must pace itself, see the throttle there.
        Map<?, ?> response;
        try {
            response = restClient.get()
                .uri("/order/detail/{orderInvoiceNumber}", providerOrderRef)
                .retrieve()
                .body(Map.class);
        } catch (HttpClientErrorException.NotFound e) {
            // Catch ONLY 404. The other errors -- 429 too fast, 5xx, network lost -- are still thrown: the
            // caller must tell "SePay says no order carries this code" from "could not ask SePay",
            // because mapping the latter to NOT_FOUND would allow failing an attempt that is still alive.
            log.warn("SePay has no order with code {} -- treating the attempt as dead", providerOrderRef, e);
            return new PaymentLinkStatusResult(PaymentLinkRemoteStatus.NOT_FOUND);
        }

        return toStatusResult(providerOrderRef, response);
    }

    private PaymentLinkStatusResult toStatusResult(String providerOrderRef, Map<?, ?> response) {
        String orderStatus = extractOrderStatus(response);
        PaymentLinkRemoteStatus status = toRemoteStatus(orderStatus);
        if (status == null) {
            // Do not guess: mapping an unknown status to FAILED could mark an already-paid invoice as
            // failed, and mapping it to PAID would grant quota for free. Keep PENDING so the next sweep asks again.
            log.warn("SePay order status is not mapped yet, keeping PENDING: orderRef={} order_status={}",
                providerOrderRef, orderStatus);
            return new PaymentLinkStatusResult(PaymentLinkRemoteStatus.PENDING);
        }
        return new PaymentLinkStatusResult(status);
    }

    /**
     * SePay has NO API to cancel a session that has not been paid, so this always returns false.
     *
     * <p>The closest is {@code POST /v1/order/voidTransaction}, but it is an entirely DIFFERENT operation:
     * it only accepts CARD transactions ({@code payment_method = CARD}) that are already {@code CAPTURED} and
     * not yet at reconciliation time -- i.e. refunding money ALREADY COLLECTED, not closing a session that has not been
     * paid. Using it here either fails (not CAPTURED yet) or, worse: refunds a transaction that the system
     * has not yet recorded as paid.
     *
     * <p>Consequence: a SePay order can only be closed after the session expires by itself. See CancelOrderUseCase.
     */
    @Override
    public boolean cancelPaymentLink(String providerOrderRef, String reason) {
        log.info("SePay does not support cancelling an unpaid payment session, ignoring the cancel command for orderRef={}",
            providerOrderRef);
        return false;
    }

    // The response wraps data in "data", but the flat form is accepted too so a small change on
    // SePay's side does not kill the reconciliation job.
    private String extractOrderStatus(Map<?, ?> response) {
        if (response == null) {
            return null;
        }
        if (response.get("data") instanceof Map<?, ?> data && data.get("order_status") instanceof String nested) {
            return nested;
        }
        return response.get("order_status") instanceof String flat ? flat : null;
    }

    @Override
    public CallbackVerificationResult verifyCallback(byte[] rawBody, Map<String, String> headers) {
        if (!hasValidSecretKey(headers)) {
            return CallbackVerificationResult.invalid();
        }

        SePayIpnPayload payload = parseBody(rawBody);
        if (payload == null || payload.order() == null) {
            log.warn("SePay IPN has a valid secret key but the payload is missing the order part");
            return CallbackVerificationResult.invalid();
        }

        String orderRef = payload.order().orderInvoiceNumber();
        if (orderRef == null || orderRef.isBlank()) {
            log.warn("SePay IPN is missing order_invoice_number, no invoice can be looked up");
            return CallbackVerificationResult.invalid();
        }

        return new CallbackVerificationResult(
            true,
            orderRef,
            resolveStatus(payload),
            parseAmount(payload.order().orderAmount())
        );
    }

    /**
     * {@code notification_type} is what SePay commits to in the IPN documentation so it takes priority; only when
     * an unknown notification type shows up does it fall back to reading {@code order_status}. Returning null means "cannot
     * conclude yet" — the upper layer will skip it instead of settling the invoice.
     */
    private PaymentLinkRemoteStatus resolveStatus(SePayIpnPayload payload) {
        String notificationType = payload.notificationType();
        if (NOTIFICATION_ORDER_PAID.equalsIgnoreCase(notificationType)) {
            return PaymentLinkRemoteStatus.PAID;
        }
        if (NOTIFICATION_TRANSACTION_VOID.equalsIgnoreCase(notificationType)) {
            return PaymentLinkRemoteStatus.CANCELLED;
        }
        PaymentLinkRemoteStatus status = toRemoteStatus(payload.order().orderStatus());
        if (status == null) {
            log.warn("SePay IPN has an unknown notification_type ({}) and an unmapped order_status ({}) — ignoring",
                notificationType, payload.order().orderStatus());
        }
        return status;
    }

    /**
     * SePay's public documentation only states a few sample values (CAPTURED, APPROVED) and does not list
     * them all, so this is a broad recognition list, and every unknown value returns null (= no
     * conclusion yet) instead of being lumped into failure. This list needs to be pinned down after running the sandbox.
     */
    private PaymentLinkRemoteStatus toRemoteStatus(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank()) {
            return null;
        }
        return switch (rawStatus.toUpperCase(Locale.ROOT)) {
            case "CAPTURED", "PAID", "COMPLETED", "SUCCESS", "SUCCEEDED" -> PaymentLinkRemoteStatus.PAID;
            case "NEW", "CREATED", "PENDING", "WAITING" -> PaymentLinkRemoteStatus.PENDING;
            case "PROCESSING", "AUTHORIZED", "APPROVED" -> PaymentLinkRemoteStatus.PROCESSING;
            case "PARTIALLY", "PARTIALLY_PAID", "UNDERPAID" -> PaymentLinkRemoteStatus.UNDERPAID;
            case "CANCELLED", "CANCELED", "VOID", "VOIDED", "REVERSED" -> PaymentLinkRemoteStatus.CANCELLED;
            case "EXPIRED", "TIMEOUT" -> PaymentLinkRemoteStatus.EXPIRED;
            case "FAILED", "DECLINED", "REJECTED", "ERROR" -> PaymentLinkRemoteStatus.FAILED;
            default -> null;
        };
    }

    // null means the IPN carries no amount — the upper layer skips the matching step instead of treating it as a mismatch.
    private BigDecimal parseAmount(String rawAmount) {
        if (rawAmount == null || rawAmount.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(rawAmount.trim());
        } catch (NumberFormatException e) {
            log.warn("SePay IPN has an unreadable order_amount: {}", rawAmount);
            return null;
        }
    }

    // Constant-time comparison so the secret does not leak through response time.
    private boolean hasValidSecretKey(Map<String, String> headers) {
        String provided = headers.get(SECRET_KEY_HEADER);
        if (provided == null) {
            log.warn("SePay IPN is missing header {}", SECRET_KEY_HEADER);
            return false;
        }
        String expected = properties.ipnSecretKey();
        // If it is not configured and still accepted, the IPN endpoint becomes an open door for anyone to settle an invoice.
        if (expected == null || expected.isBlank()) {
            log.error("sepay.pg.ipn-secret-key is not configured — rejecting every SePay IPN");
            return false;
        }
        return MessageDigest.isEqual(
            provided.getBytes(StandardCharsets.UTF_8),
            expected.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String sign(Map<String, String> fields) {
        StringBuilder data = new StringBuilder();
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            if (!SIGNED_FIELDS.contains(entry.getKey()) || entry.getValue() == null) {
                continue;
            }
            if (!data.isEmpty()) {
                data.append(SIGNATURE_SEPARATOR);
            }
            data.append(entry.getKey()).append('=').append(entry.getValue());
        }
        try {
            Mac mac = Mac.getInstance(HMAC);
            mac.init(getSecretKey());
            return Base64.getEncoder()
                .encodeToString(mac.doFinal(data.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            log.error("Error while signing the SePay payment form", e);
            throw new IllegalStateException("Error computing the signature for SePay");
        }
    }

    private SecretKey getSecretKey() {
        return new SecretKeySpec(properties.secretKey().getBytes(StandardCharsets.UTF_8), HMAC);
    }

    private static String basicAuth(SePayPaymentProperties properties) {
        String credentials = properties.merchantId() + ":" + properties.secretKey();
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    // Better to die here with a clear message than an NPE in the middle of signing, or worse a POST to SePay
    // of a form carrying an empty merchant. Not validated at startup because dev/test environments run
    // without the SePay environment variables.
    private void requireConfigured() {
        if (!properties.isConfigured()) {
            throw new IllegalStateException(
                "SePay is not configured: sepay.pg.merchant-id and sepay.pg.secret-key are required (SEPAY_MERCHANT_ID / SEPAY_SECRET_KEY)");
        }
    }

    private SePayIpnPayload parseBody(byte[] rawBody) {
        try {
            return jsonMapper.readValue(rawBody, SePayIpnPayload.class);
        } catch (Exception e) {
            log.warn("Could not read the SePay IPN payload", e);
            return null;
        }
    }

    /**
     * The IPN payload uses snake_case, while the application's JsonMapper is an autoconfigured bean with the default
     * naming strategy -- so @JsonProperty must be declared on every field. Without it every field binds to null
     * and every IPN is treated as missing the order.
     *
     * <p>The String type for id/amount is intentional: SePay does not promise that id is a UUID or amount an integer,
     * and an exception while parsing here means losing the paid notification altogether.
     */
    private record SePayIpnPayload(
        @JsonProperty("timestamp") Long timestamp,
        @JsonProperty("notification_type") String notificationType,
        @JsonProperty("order") SePayIpnOrderPayload order,
        @JsonProperty("transaction") SePayIpnTransactionPayload transaction,
        @JsonProperty("customer") SePayIpnCustomerPayload customer
    ) {
        private record SePayIpnOrderPayload(
            @JsonProperty("id") String id,
            @JsonProperty("order_id") String orderId,
            @JsonProperty("order_status") String orderStatus,
            @JsonProperty("order_currency") String orderCurrency,
            @JsonProperty("order_amount") String orderAmount,
            @JsonProperty("order_invoice_number") String orderInvoiceNumber,
            // Object and not String: SePay sends custom_data as an array (empty, or an array of
            // name/value objects). Declared as String it makes Jackson throw MismatchedInputException, parseBody
            // returns null, and EVERY IPN is treated as unauthenticatable — the school paid but the
            // invoice is never settled. We do not read this field, so any type will do.
            @JsonProperty("custom_data") Object customData,
            @JsonProperty("user_agent") String userAgent,
            @JsonProperty("ip_address") String ipAddress,
            @JsonProperty("order_description") String orderDescription
        ) {
        }

        private record SePayIpnTransactionPayload(
            @JsonProperty("id") String id,
            @JsonProperty("payment_method") String paymentMethod,
            @JsonProperty("transaction_id") String transactionId,
            @JsonProperty("transaction_type") String transactionType,
            @JsonProperty("transaction_date") String transactionDate,
            @JsonProperty("transaction_status") String transactionStatus,
            @JsonProperty("transaction_amount") String transactionAmount,
            @JsonProperty("transaction_currency") String transactionCurrency
        ) {
        }

        private record SePayIpnCustomerPayload(
            @JsonProperty("id") String id,
            @JsonProperty("customer_id") String customerId
        ) {
        }
    }
}
