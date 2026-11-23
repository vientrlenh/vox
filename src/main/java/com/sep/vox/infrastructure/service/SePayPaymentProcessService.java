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
 * Adapter cho SePay Payment Gateway.
 *
 * <p>Luồng khác hẳn PayOS: SePay không tạo sẵn một link riêng cho từng đơn qua API. Ta tự dựng một
 * bộ field kèm chữ ký HMAC rồi để trình duyệt POST sang một URL checkout cố định — nên
 * {@code createPaymentLink} ở đây không hề gọi mạng, và cũng không có paymentLinkId để trả về.
 */
@Service
@Slf4j
public class SePayPaymentProcessService implements PaymentProcessPort {

    // SePay xác thực IPN bằng secret key gửi kèm ở header, không ký HMAC trên body — đây chính là
    // lý do verifyCallback phải nhận headers chứ không chỉ nhận body.
    private static final String SECRET_KEY_HEADER = "X-Secret-Key";

    private static final String HMAC = "HmacSHA256";
    private static final String CURRENCY = "VND";
    private static final String OPERATION_PURCHASE = "PURCHASE";

    // SePay ký trên chuỗi các cặp "key=value" nối bằng dấu phẩy, nên bản thân dấu phẩy trong giá
    // trị sẽ làm chuỗi ký nhập nhằng và chữ ký hai bên lệch nhau.
    private static final char SIGNATURE_SEPARATOR = ',';

    // Chỉ là TẬP HỢP để lọc, không phải thứ tự ký: thứ tự lấy từ chính thứ tự chèn vào LinkedHashMap
    // ở buildCheckoutFields(). Giữ hai danh sách có thứ tự song song là cách chắc chắn để một hôm
    // nào đó sửa chỗ này quên chỗ kia rồi mọi giao dịch trượt chữ ký.
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

    // Job đối soát chạy trên luồng scheduler và gọi lần lượt từng hóa đơn, nên một request treo là
    // treo cả lượt quét — timeout ở đây không phải phòng xa mà là điều kiện để job chạy đúng nhịp.
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
     * SePay định danh đơn bằng {@code order_invoice_number} dạng chuỗi và chỉ đòi nó duy nhất.
     * Trước đây dùng thẳng số hóa đơn, nhưng hóa đơn giờ chỉ phát SAU khi tiền về nên lúc phát link
     * chưa có số nào -- sinh tại chỗ, tiền tố VOX để nhìn dashboard SePay là biết đơn của mình.
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
     * VND không có phần lẻ và SDK chính thức ép số tiền về chuỗi số nguyên. BigDecimal đọc từ cột
     * numeric(_,2) thì {@code toPlainString()} ra "10000.00" — vừa lệch chữ ký, vừa có nguy cơ
     * SePay hiểu thành một số tiền khác.
     */
    private String formatAmount(BigDecimal amount) {
        return String.valueOf(amount.longValueExact());
    }

    /**
     * Dấu phẩy là ký tự phân tách của chuỗi ký, nên để nó lọt vào giá trị là tự phá chữ ký của mình.
     * Thay bằng khoảng trắng thay vì ném lỗi: mô tả đơn hàng bị đổi một ký tự vẫn tốt hơn là chặn
     * hẳn một giao dịch hợp lệ.
     */
    private String sanitizeForSignature(String value) {
        if (value == null) {
            return "";
        }
        if (value.indexOf(SIGNATURE_SEPARATOR) < 0) {
            return value;
        }
        log.warn("Mô tả đơn SePay chứa dấu phẩy — đã thay bằng khoảng trắng để không phá chữ ký");
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
            log.warn("SePay không có đơn nào mang mã {} -- coi như lần thử đã chết", providerOrderRef, e);
            return new PaymentLinkStatusResult(PaymentLinkRemoteStatus.NOT_FOUND);
        }

        String orderStatus = extractOrderStatus(response);
        PaymentLinkRemoteStatus status = toRemoteStatus(orderStatus);
        if (status == null) {
            // Do not guess: mapping an unknown status to FAILED could mark an already-paid invoice as
            // failed, and mapping it to PAID would grant quota for free. Keep PENDING so the next sweep asks again.
            log.warn("Trạng thái đơn SePay chưa được ánh xạ, giữ nguyên PENDING: orderRef={} order_status={}",
                providerOrderRef, orderStatus);
            return new PaymentLinkStatusResult(PaymentLinkRemoteStatus.PENDING);
        }
        return new PaymentLinkStatusResult(status);
    }

    /**
     * SePay KHÔNG có API hủy một phiên chưa trả, nên luôn trả false.
     *
     * <p>Thứ gần nhất là {@code POST /v1/order/voidTransaction}, nhưng nó là nghiệp vụ KHÁC hẳn:
     * chỉ nhận giao dịch THẺ ({@code payment_method = CARD}) đã ở trạng thái {@code CAPTURED} và
     * chưa tới giờ đối soát -- tức là hoàn lại một khoản ĐÃ THU, không phải đóng một phiên chưa
     * trả. Dùng nó ở đây thì hoặc lỗi (chưa CAPTURED), hoặc tệ hơn: hoàn một giao dịch mà hệ thống
     * chưa kịp ghi nhận là đã trả.
     *
     * <p>Hệ quả: đơn SePay chỉ đóng được sau khi phiên tự hết hạn. Xem CancelOrderUseCase.
     */
    @Override
    public boolean cancelPaymentLink(String providerOrderRef, String reason) {
        log.info("SePay không hỗ trợ hủy phiên thanh toán chưa trả, bỏ qua lệnh hủy cho orderRef={}",
            providerOrderRef);
        return false;
    }

    // Response bọc dữ liệu trong "data", nhưng chấp nhận cả dạng phẳng để một thay đổi nhỏ bên
    // SePay không làm chết luôn job đối soát.
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
            log.warn("IPN SePay có secret key hợp lệ nhưng payload thiếu phần order");
            return CallbackVerificationResult.invalid();
        }

        String orderRef = payload.order().orderInvoiceNumber();
        if (orderRef == null || orderRef.isBlank()) {
            log.warn("IPN SePay thiếu order_invoice_number, không tra được hóa đơn nào");
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
            log.warn("IPN SePay có notification_type lạ ({}) và order_status chưa ánh xạ ({}) — bỏ qua",
                notificationType, payload.order().orderStatus());
        }
        return status;
    }

    /**
     * Tài liệu công khai của SePay chỉ nêu vài giá trị mẫu (CAPTURED, APPROVED) chứ không liệt kê
     * đủ, nên đây là danh sách nhận diện rộng, và mọi giá trị chưa biết đều trả null (= chưa kết
     * luận) thay vì bị gom thành thất bại. Cần chốt lại danh sách này sau khi chạy sandbox.
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

    // null nghĩa là IPN không kèm số tiền — tầng trên sẽ bỏ qua bước đối chiếu thay vì coi là lệch.
    private BigDecimal parseAmount(String rawAmount) {
        if (rawAmount == null || rawAmount.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(rawAmount.trim());
        } catch (NumberFormatException e) {
            log.warn("IPN SePay có order_amount không đọc được: {}", rawAmount);
            return null;
        }
    }

    // So sánh constant-time để không lộ secret qua thời gian phản hồi.
    private boolean hasValidSecretKey(Map<String, String> headers) {
        String provided = headers.get(SECRET_KEY_HEADER);
        if (provided == null) {
            log.warn("IPN SePay thiếu header {}", SECRET_KEY_HEADER);
            return false;
        }
        String expected = properties.ipnSecretKey();
        // If it is not configured and still accepted, the IPN endpoint becomes an open door for anyone to settle an invoice.
        if (expected == null || expected.isBlank()) {
            log.error("Chưa cấu hình sepay.pg.ipn-secret-key — từ chối mọi IPN SePay");
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
            log.error("Lỗi khi ký form thanh toán SePay", e);
            throw new IllegalStateException("Lỗi ký chữ ký cho SePay");
        }
    }

    private SecretKey getSecretKey() {
        return new SecretKeySpec(properties.secretKey().getBytes(StandardCharsets.UTF_8), HMAC);
    }

    private static String basicAuth(SePayPaymentProperties properties) {
        String credentials = properties.merchantId() + ":" + properties.secretKey();
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    // Thà chết ở đây với thông điệp rõ ràng còn hơn NPE giữa lúc ký, hoặc tệ hơn là POST sang SePay
    // một form mang merchant rỗng. Không validate lúc khởi động vì môi trường dev/test chạy được
    // mà không cần biến môi trường SePay.
    private void requireConfigured() {
        if (!properties.isConfigured()) {
            throw new IllegalStateException(
                "Chưa cấu hình SePay: cần sepay.pg.merchant-id và sepay.pg.secret-key (SEPAY_MERCHANT_ID / SEPAY_SECRET_KEY)");
        }
    }

    private SePayIpnPayload parseBody(byte[] rawBody) {
        try {
            return jsonMapper.readValue(rawBody, SePayIpnPayload.class);
        } catch (Exception e) {
            log.warn("Không đọc được payload IPN SePay", e);
            return null;
        }
    }

    /**
     * Payload IPN dùng snake_case, còn JsonMapper của ứng dụng là bean autoconfig với chiến lược
     * đặt tên mặc định — nên phải khai @JsonProperty từng field. Thiếu nó thì mọi field bind ra null
     * và IPN nào cũng bị coi là thiếu order.
     *
     * <p>Kiểu String cho id/amount là cố ý: SePay không cam kết id là UUID hay amount là số nguyên,
     * mà một exception lúc parse ở đây đồng nghĩa với việc mất luôn thông báo đã thanh toán.
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
            // Object chứ không phải String: SePay gửi custom_data dưới dạng mảng (rỗng, hoặc mảng
            // object name/value). Khai là String làm Jackson ném MismatchedInputException, parseBody
            // trả null, và MỌI IPN đều bị coi là không xác thực được — trường đã trả tiền nhưng hóa
            // đơn không bao giờ được chốt. Ta không đọc field này, nên nhận kiểu gì cũng được.
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
