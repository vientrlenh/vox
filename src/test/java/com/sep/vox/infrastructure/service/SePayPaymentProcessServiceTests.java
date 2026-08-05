package com.sep.vox.infrastructure.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.response.output.CallbackVerificationResult;
import com.sep.vox.application.response.output.CheckoutAction;
import com.sep.vox.application.response.output.CreatePaymentLinkCommand;
import com.sep.vox.application.response.output.PaymentLinkRemoteStatus;
import com.sep.vox.domain.model.subscription.PaymentMethod;
import com.sep.vox.infrastructure.properties.SePayPaymentProperties;
import com.sep.vox.infrastructure.properties.SePayPaymentProperties.SePayCheckoutMethod;
import com.sep.vox.infrastructure.properties.SePayPaymentProperties.SePayEnvironment;

import tools.jackson.databind.json.JsonMapper;

/**
 * Chữ ký form là thứ quyết định SePay có nhận đơn hay không, còn header X-Secret-Key là ranh giới
 * xác thực duy nhất của endpoint IPN — nó được cổng gọi chứ không phải client đã đăng nhập, nên
 * không có JWT nào đứng sau.
 *
 * <p>{@link #signsFieldsInTheOrderTheOfficialSdkUses()} dùng vector tính độc lập hai lần: bằng
 * {@code openssl dgst -sha256 -hmac} và bằng chính {@code signFields} của SDK Node chính thức
 * (sepay-pg-node). Nhờ vậy test không chỉ khẳng định lại thuật toán mà nó đang kiểm tra — và quan
 * trọng hơn, nó chốt luôn THỨ TỰ field, thứ mà cả hai SDK đều lấy theo thứ tự phần tử của mảng chứ
 * không sort, nên đảo hai dòng trong buildCheckoutFields là hỏng toàn bộ giao dịch.
 */
class SePayPaymentProcessServiceTests {

    private static final String MERCHANT_ID = "TEST_MERCHANT";
    private static final String SECRET_KEY = "test-secret-key";
    private static final String IPN_SECRET_KEY = "test-ipn-secret";
    private static final String RETURN_BASE_URL = "https://vox.test";

    private static final String ORDER_REF = "INV-2026-ABCD1234";

    /** HMAC-SHA256/Base64 của chuỗi ký dựng từ đúng bộ field mà createPaymentLink sinh ra. */
    private static final String EXPECTED_SIGNATURE = "XTtR737b25JhXlZNveXz2ZmxoKUCD83oZCdUGVhwHOM=";

    private SePayPaymentProcessService service;

    @BeforeEach
    void setUp() {
        service = newService(IPN_SECRET_KEY);
    }

    private static SePayPaymentProcessService newService(String ipnSecretKey) {
        var properties = new SePayPaymentProperties(
            SePayEnvironment.SANDBOX, MERCHANT_ID, SECRET_KEY, ipnSecretKey,
            SePayCheckoutMethod.BANK_TRANSFER, RETURN_BASE_URL);
        // RestClient chỉ dùng ở getPaymentLinkStatus, không đụng tới trong các test dưới đây.
        return new SePayPaymentProcessService(properties, JsonMapper.builder().build());
    }

    private CallbackVerificationResult verify(String body, Map<String, String> headers) {
        return service.verifyCallback(body.getBytes(StandardCharsets.UTF_8), headers);
    }

    private static Map<String, String> validHeaders() {
        return Map.of("X-Secret-Key", IPN_SECRET_KEY);
    }

    private static String ipn(String notificationType, String orderStatus, String orderRef, String amount) {
        return """
            {
              "timestamp": 1767225600,
              "notification_type": "%s",
              "order": {
                "id": "9f1c",
                "order_status": "%s",
                "order_currency": "VND",
                "order_amount": "%s",
                "order_invoice_number": "%s",
                "order_description": "VOX-%s"
              },
              "transaction": {
                "id": "tx-1",
                "payment_method": "BANK_TRANSFER",
                "transaction_status": "APPROVED"
              }
            }
            """.formatted(notificationType, orderStatus, amount, orderRef, orderRef);
    }

    @Test
    void reportsItselfAsTheSepayAdapter() {
        assertThat(service.provider()).isEqualTo(PaymentMethod.SEPAY);
    }

    /**
     * SePay định danh đơn bằng order_invoice_number dạng chuỗi và chỉ đòi nó duy nhất — sinh thêm
     * một mã thứ hai chỉ tạo ra một lớp phải đối chiếu tay với dashboard SePay.
     */
    @Test
    void usesTheInvoiceNumberItselfAsOrderRef() {
        assertThat(service.newOrderRef(ORDER_REF)).isEqualTo(ORDER_REF);
    }

    @Test
    void signsFieldsInTheOrderTheOfficialSdkUses() {
        var result = service.createPaymentLink(
            new CreatePaymentLinkCommand(ORDER_REF, new BigDecimal("10000"), "VOX-" + ORDER_REF));

        assertThat(result.fields()).containsEntry("signature", EXPECTED_SIGNATURE);
    }

    /**
     * Thứ tự chèn của LinkedHashMap CHÍNH LÀ thứ tự ký, nên nó phải bám đúng thứ tự
     * {@code prepareFormFields} của SDK PHP. Khoá thứ tự lại bằng test vì đây là ràng buộc không
     * nhìn ra được khi đọc riêng đoạn sinh chữ ký.
     */
    @Test
    void emitsFieldsInTheCanonicalSdkOrder() {
        var result = service.createPaymentLink(
            new CreatePaymentLinkCommand(ORDER_REF, new BigDecimal("10000"), "VOX-" + ORDER_REF));

        assertThat(List.copyOf(result.fields().keySet())).containsExactly(
            "merchant", "currency", "order_amount", "operation", "order_description",
            "payment_method", "order_invoice_number", "success_url", "error_url", "cancel_url",
            "signature");
    }

    /** SePay là form POST chứ không phải redirect — FE phải phân biệt được, xem CheckoutAction. */
    @Test
    void returnsFormPostCheckoutAgainstTheFixedSandboxUrl() {
        var result = service.createPaymentLink(
            new CreatePaymentLinkCommand(ORDER_REF, new BigDecimal("10000"), "d"));

        assertThat(result.action()).isEqualTo(CheckoutAction.FORM_POST);
        assertThat(result.actionUrl()).isEqualTo("https://pay-sandbox.sepay.vn/v1/checkout/init");
        assertThat(result.paymentLinkId()).isNull();
    }

    /**
     * BigDecimal đọc từ cột numeric(_,2) ra "10000.00" với toPlainString, trong khi SDK chính thức
     * ép về chuỗi số nguyên — lệch một ký tự là lệch chữ ký.
     */
    @Test
    void formatsAmountAsIntegerStringRegardlessOfScale() {
        var result = service.createPaymentLink(
            new CreatePaymentLinkCommand(ORDER_REF, new BigDecimal("10000.00"), "VOX-" + ORDER_REF));

        assertThat(result.fields()).containsEntry("order_amount", "10000");
        assertThat(result.fields()).containsEntry("signature", EXPECTED_SIGNATURE);
    }

    /** Dấu phẩy là ký tự phân tách của chuỗi ký, để lọt vào giá trị là tự phá chữ ký của mình. */
    @Test
    void stripsCommasFromDescriptionBecauseTheyAreTheSignatureSeparator() {
        var result = service.createPaymentLink(
            new CreatePaymentLinkCommand(ORDER_REF, new BigDecimal("10000"), "Goi A, thang 8"));

        assertThat(result.fields().get("order_description")).doesNotContain(",");
    }

    @Test
    void failsLoudlyWhenMerchantCredentialsAreMissing() {
        var unconfigured = new SePayPaymentProperties(
            SePayEnvironment.SANDBOX, "", "", IPN_SECRET_KEY, SePayCheckoutMethod.BANK_TRANSFER, RETURN_BASE_URL);
        var unconfiguredService = new SePayPaymentProcessService(
            unconfigured, JsonMapper.builder().build());

        assertThatThrownBy(() -> unconfiguredService.createPaymentLink(
                new CreatePaymentLinkCommand(ORDER_REF, BigDecimal.TEN, "d")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("SEPAY_MERCHANT_ID");
    }

    @Test
    void acceptsIpnCarryingTheConfiguredSecretKey() {
        var result = verify(ipn("ORDER_PAID", "CAPTURED", ORDER_REF, "10000"), validHeaders());

        assertThat(result.valid()).isTrue();
        assertThat(result.providerOrderRef()).isEqualTo(ORDER_REF);
        assertThat(result.status()).isEqualTo(PaymentLinkRemoteStatus.PAID);
        assertThat(result.amount()).isEqualByComparingTo("10000");
    }

    @Test
    void rejectsIpnWithWrongOrMissingSecretKey() {
        var body = ipn("ORDER_PAID", "CAPTURED", ORDER_REF, "10000");

        assertThat(verify(body, Map.of("X-Secret-Key", "sai-secret")).valid()).isFalse();
        assertThat(verify(body, Map.of()).valid()).isFalse();
    }

    /**
     * Chưa cấu hình ipn-secret-key mà vẫn chấp nhận thì endpoint IPN thành cửa mở cho bất kỳ ai
     * chốt hóa đơn — kể cả khi họ không gửi header nào.
     */
    @Test
    void rejectsEveryIpnWhileTheIpnSecretIsUnconfigured() {
        var unconfigured = newService("");
        var body = ipn("ORDER_PAID", "CAPTURED", ORDER_REF, "10000").getBytes(StandardCharsets.UTF_8);

        assertThat(unconfigured.verifyCallback(body, Map.of("X-Secret-Key", "")).valid()).isFalse();
        assertThat(unconfigured.verifyCallback(body, Map.of("X-Secret-Key", "bat-ky")).valid()).isFalse();
    }

    /**
     * Payload IPN là snake_case còn record khai camelCase — thiếu @JsonProperty thì mọi field bind
     * ra null và IPN nào cũng bị coi là thiếu order.
     */
    @Test
    void bindsSnakeCasePayloadFields() {
        var result = verify(ipn("ORDER_PAID", "CAPTURED", ORDER_REF, "10000"), validHeaders());

        assertThat(result.providerOrderRef()).isEqualTo(ORDER_REF);
        assertThat(result.amount()).isNotNull();
    }

    @Test
    void mapsTransactionVoidToCancelled() {
        var result = verify(ipn("TRANSACTION_VOID", "VOIDED", ORDER_REF, "10000"), validHeaders());

        assertThat(result.valid()).isTrue();
        assertThat(result.status()).isEqualTo(PaymentLinkRemoteStatus.CANCELLED);
    }

    /**
     * Tài liệu SePay không liệt kê đủ giá trị order_status. Trạng thái lạ phải ra null (= chưa kết
     * luận) để tầng trên bỏ qua — quy về FAILED thì một hóa đơn đã thu tiền bị đánh hỏng, còn quy
     * về PAID thì cấp quota không công.
     */
    @Test
    void leavesUnknownStatusUnresolvedInsteadOfGuessing() {
        var result = verify(ipn("SOMETHING_NEW", "SOME_NEW_STATUS", ORDER_REF, "10000"), validHeaders());

        assertThat(result.valid()).isTrue();
        assertThat(result.status()).isNull();
    }

    /** Không có order_invoice_number thì không tra được hóa đơn nào — vô dụng, phải là invalid. */
    @Test
    void rejectsIpnWithoutOrderInvoiceNumber() {
        var body = """
            {"notification_type":"ORDER_PAID","order":{"order_status":"CAPTURED","order_amount":"10000"}}
            """;

        assertThat(verify(body, validHeaders()).valid()).isFalse();
    }

    /** Body rác phải trả invalid chứ không được ném ra ngoài thành lỗi 500. */
    @Test
    void rejectsMalformedBodyWithoutThrowing() {
        assertThat(verify("khong phai json", validHeaders()).valid()).isFalse();
        assertThat(verify("{\"notification_type\":\"ORDER_PAID\"}", validHeaders()).valid()).isFalse();
    }

    /** IPN không kèm số tiền: vẫn hợp lệ, chỉ là tầng trên sẽ bỏ qua bước đối chiếu tiền. */
    @Test
    void toleratesMissingAmount() {
        var body = """
            {"notification_type":"ORDER_PAID","order":{"order_status":"CAPTURED","order_invoice_number":"%s"}}
            """.formatted(ORDER_REF);

        var result = verify(body, validHeaders());

        assertThat(result.valid()).isTrue();
        assertThat(result.amount()).isNull();
    }
}
