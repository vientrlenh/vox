package com.sep.vox.infrastructure.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.response.output.PaymentLinkRemoteStatus;
import com.sep.vox.domain.model.subscription.PaymentMethod;
import com.sep.vox.infrastructure.properties.SePayPaymentProperties;

import tools.jackson.databind.json.JsonMapper;

/**
 * IPN là đường duy nhất SePay báo "đã thu được tiền". Nếu bước đọc payload hỏng thì hóa đơn không
 * bao giờ được chốt: trường đã trả tiền nhưng không được cấp quota, và SePay chỉ retry một số lần
 * hữu hạn rồi bỏ cuộc.
 */
class SePayPaymentProcessServiceTests {

    private static final String IPN_SECRET_KEY = "sepay-test-ipn-secret";

    private SePayPaymentProcessService service;

    @BeforeEach
    void setUp() {
        var properties = new SePayPaymentProperties(
            SePayPaymentProperties.SePayEnvironment.SANDBOX,
            "merchant-1",
            "sepay-test-signing-secret",
            IPN_SECRET_KEY,
            SePayPaymentProperties.SePayCheckoutMethod.BANK_TRANSFER,
            "https://vox.test"
        );
        service = new SePayPaymentProcessService(properties, JsonMapper.builder().build());
    }

    private static Map<String, String> validHeaders() {
        return Map.of("X-Secret-Key", IPN_SECRET_KEY);
    }

    /**
     * Payload thật của SePay gửi {@code custom_data} dưới dạng MẢNG. Khai nó là String khiến Jackson
     * ném MismatchedInputException, parseBody trả null, và toàn bộ IPN bị coi là không xác thực được
     * — tức mọi giao dịch SePay đều thất bại ở bước chốt, dù secret key hoàn toàn đúng.
     */
    private static byte[] ipnWithArrayCustomData(String notificationType, String orderStatus) {
        return ("""
            {
              "timestamp": 1754380800,
              "notification_type": "%s",
              "order": {
                "id": "0195f0a1-1111-2222-3333-444455556666",
                "order_id": "ORD-1",
                "order_status": "%s",
                "order_currency": "VND",
                "order_amount": "5000000",
                "order_invoice_number": "1754380800000",
                "custom_data": [],
                "user_agent": "Mozilla/5.0",
                "ip_address": "1.2.3.4",
                "order_description": "VOX-1754380800000"
              },
              "transaction": {
                "id": "0195f0a1-7777-8888-9999-aaaabbbbcccc",
                "payment_method": "BANK_TRANSFER",
                "transaction_id": "TXN-1",
                "transaction_type": "PURCHASE",
                "transaction_date": "2026-08-05T10:00:00Z",
                "transaction_status": "CAPTURED",
                "transaction_amount": "5000000",
                "transaction_currency": "VND"
              },
              "customer": {
                "id": "0195f0a1-dddd-eeee-ffff-000011112222",
                "customer_id": "school-1"
              }
            }
            """).formatted(notificationType, orderStatus).getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void reportsItselfAsTheSepayAdapter() {
        assertThat(service.provider()).isEqualTo(PaymentMethod.SEPAY);
    }

    @Test
    void acceptsIpnWhoseCustomDataIsAnArray() {
        var result = service.verifyCallback(ipnWithArrayCustomData("ORDER_PAID", "CAPTURED"), validHeaders());

        assertThat(result.valid()).isTrue();
        assertThat(result.providerOrderRef()).isEqualTo("1754380800000");
        assertThat(result.status()).isEqualTo(PaymentLinkRemoteStatus.PAID);
        assertThat(result.amount()).isEqualByComparingTo("5000000");
    }

    /** custom_data cũng có thể là mảng object — không được vỡ vì kiểu của một field không dùng tới. */
    @Test
    void acceptsIpnWhoseCustomDataIsAnArrayOfObjects() {
        var body = new String(ipnWithArrayCustomData("ORDER_PAID", "CAPTURED"), StandardCharsets.UTF_8)
            .replace("\"custom_data\": []", "\"custom_data\": [{\"name\":\"k\",\"value\":\"v\"}]");

        var result = service.verifyCallback(body.getBytes(StandardCharsets.UTF_8), validHeaders());

        assertThat(result.valid()).isTrue();
        assertThat(result.providerOrderRef()).isEqualTo("1754380800000");
    }

    @Test
    void rejectsIpnWithWrongSecretKey() {
        var result = service.verifyCallback(
            ipnWithArrayCustomData("ORDER_PAID", "CAPTURED"), Map.of("X-Secret-Key", "sai-secret"));

        assertThat(result.valid()).isFalse();
    }

    @Test
    void rejectsIpnWithoutSecretKeyHeader() {
        var result = service.verifyCallback(ipnWithArrayCustomData("ORDER_PAID", "CAPTURED"), Map.of());

        assertThat(result.valid()).isFalse();
    }

    /**
     * Trạng thái lạ phải ra null (= chưa kết luận) để tầng trên giữ nguyên PENDING, chứ không được
     * gom thành thất bại rồi đóng hóa đơn của một giao dịch có thể vẫn đang chạy.
     */
    @Test
    void leavesUnmappedStatusUndecidedInsteadOfFailing() {
        var result = service.verifyCallback(
            ipnWithArrayCustomData("SOMETHING_NEW", "MOT_TRANG_THAI_LA"), validHeaders());

        assertThat(result.valid()).isTrue();
        assertThat(result.status()).isNull();
    }
}
