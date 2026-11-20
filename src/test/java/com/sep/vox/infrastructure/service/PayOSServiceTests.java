package com.sep.vox.infrastructure.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.response.output.CallbackVerificationResult;
import com.sep.vox.application.response.output.PaymentLinkRemoteStatus;
import com.sep.vox.domain.model.payment.PaymentProvider;

import tools.jackson.databind.json.JsonMapper;

/**
 * Chữ ký webhook là ranh giới xác thực duy nhất của endpoint callback — nó được gọi bởi cổng
 * thanh toán chứ không phải bởi client đã đăng nhập, nên không có JWT nào đứng sau.
 *
 * <p>Test đi qua {@code verifyCallback} thay vì gọi thẳng hàm ký, để phủ cả đường parse payload:
 * chữ ký đúng mà bóc sai orderCode thì tiền vẫn vào nhầm hóa đơn.
 *
 * <p>Vector dưới đây được tính độc lập bằng {@code openssl dgst -sha256 -hmac} trên chuỗi
 * {@code amount=3000&code=00&desc=&orderCode=123}, để test không chỉ khẳng định lại chính thuật
 * toán mà nó đang kiểm tra.
 */
class PayOSServiceTests {

    private static final String CHECKSUM_KEY = "test-checksum-key";
    private static final String EXPECTED_SIGNATURE =
        "12eba8b724145a079dc83c934acd23fe9ba856c0321ebb23c937e8014ce0a173";
    private static final String DATA_JSON =
        "{\"orderCode\":123,\"amount\":3000,\"code\":\"00\",\"desc\":null}";

    private PayOSService service;

    @BeforeEach
    void setUp() {
        // payOSClient chỉ được dùng ở createPaymentLink/getPaymentLinkStatus, không đụng tới ở đây.
        // clientId/apiKey cũng vậy -- chúng chỉ phục vụ isConfigured(), nhưng phải đặt giá trị thật
        // để test không vô tình chạy trên một adapter tự coi mình là chưa cấu hình.
        service = new PayOSService(
            null, JsonMapper.builder().build(), "client-id", "api-key", CHECKSUM_KEY,
            "https://vox.test/return", "https://vox.test/cancel");
    }

    private static byte[] webhook(String dataJson, String signature) {
        return ("{\"data\":" + dataJson + ",\"signature\":\"" + signature + "\"}")
            .getBytes(StandardCharsets.UTF_8);
    }

    private CallbackVerificationResult verify(byte[] rawBody) {
        return service.verifyCallback(rawBody, Map.of());
    }

    @Test
    void reportsItselfAsThePayosAdapter() {
        assertThat(service.provider()).isEqualTo(PaymentProvider.PAYOS);
    }

    @Test
    void acceptsWebhookMatchingIndependentlyComputedSignature() {
        var result = verify(webhook(DATA_JSON, EXPECTED_SIGNATURE));

        assertThat(result.valid()).isTrue();
        assertThat(result.providerOrderRef()).isEqualTo("123");
        assertThat(result.status()).isEqualTo(PaymentLinkRemoteStatus.PAID);
        assertThat(result.amount()).isEqualByComparingTo("3000");
    }

    @Test
    void rejectsTamperedSignature() {
        var tampered = EXPECTED_SIGNATURE.substring(0, EXPECTED_SIGNATURE.length() - 1) + "0";

        assertThat(verify(webhook(DATA_JSON, tampered)).valid()).isFalse();
    }

    @Test
    void rejectsPayloadTamperedAfterSigning() {
        var tampered = "{\"orderCode\":123,\"amount\":1,\"code\":\"00\",\"desc\":null}";

        assertThat(verify(webhook(tampered, EXPECTED_SIGNATURE)).valid()).isFalse();
    }

    /**
     * PayOS ký trên chuỗi query đã sắp xếp theo tên field, còn thứ tự key trong JSON nhận được là
     * bất kỳ — nếu bỏ bước sort thì mọi webhook thật sẽ trượt chữ ký.
     */
    @Test
    void ignoresKeyOrderOfIncomingPayload() {
        var reordered = "{\"desc\":null,\"code\":\"00\",\"amount\":3000,\"orderCode\":123}";

        assertThat(verify(webhook(reordered, EXPECTED_SIGNATURE)).valid()).isTrue();
    }

    /** Field null phải ký thành chuỗi rỗng, không phải chữ "null". */
    @Test
    void treatsNullValueAsEmptyString() {
        var withEmptyString = "{\"orderCode\":123,\"amount\":3000,\"code\":\"00\",\"desc\":\"\"}";

        assertThat(verify(webhook(withEmptyString, EXPECTED_SIGNATURE)).valid()).isTrue();
    }

    /**
     * Webhook hợp lệ nhưng code khác "00" là giao dịch thất bại — phải ra FAILED, không phải PAID.
     * Chữ ký tính riêng cho payload này trên {@code amount=3000&code=99&desc=&orderCode=123}.
     */
    @Test
    void mapsNonSuccessCodeToFailedInsteadOfPaid() {
        var failedData = "{\"orderCode\":123,\"amount\":3000,\"code\":\"99\",\"desc\":null}";
        var failedSignature = "c660e09fea828e1c944f8638ff82b1053c15baf01269a9c2d603db7258273975";

        var result = verify(webhook(failedData, failedSignature));

        assertThat(result.valid()).isTrue();
        assertThat(result.status()).isEqualTo(PaymentLinkRemoteStatus.FAILED);
    }

    /** Body rác/không phải JSON phải trả invalid chứ không được ném ra ngoài thành lỗi 500. */
    @Test
    void rejectsMalformedBodyWithoutThrowing() {
        assertThat(verify("khong phai json".getBytes(StandardCharsets.UTF_8)).valid()).isFalse();
    }

    /** Payload thiếu data hoặc signature cũng phải là invalid, không phải NullPointerException. */
    @Test
    void rejectsPayloadMissingDataOrSignature() {
        assertThat(verify("{\"signature\":\"abc\"}".getBytes(StandardCharsets.UTF_8)).valid()).isFalse();
        assertThat(verify(("{\"data\":" + DATA_JSON + "}").getBytes(StandardCharsets.UTF_8)).valid()).isFalse();
    }

    /**
     * Payload webhook của PayOS đôi khi chứa byte UTF-8 không hợp lệ ở phần text tiếng Việt (desc).
     * Yêu cầu là không crash — decode lenient rồi trượt chữ ký, chứ không ném StreamReadException.
     */
    @Test
    void rejectsInvalidUtf8BytesWithoutThrowing() {
        var prefix = "{\"data\":{\"orderCode\":123,\"desc\":\"".getBytes(StandardCharsets.UTF_8);
        var suffix = "\"},\"signature\":\"abc\"}".getBytes(StandardCharsets.UTF_8);
        var body = new byte[prefix.length + 2 + suffix.length];
        System.arraycopy(prefix, 0, body, 0, prefix.length);
        body[prefix.length] = (byte) 0xFF;      // byte không hợp lệ trong UTF-8
        body[prefix.length + 1] = (byte) 0xFE;
        System.arraycopy(suffix, 0, body, prefix.length + 2, suffix.length);

        assertThat(verify(body).valid()).isFalse();
    }
}
