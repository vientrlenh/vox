package com.sep.vox.infrastructure.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cấu hình cổng thanh toán SePay (SePay Payment Gateway — KHÔNG phải dòng Banking API/webhook biến
 * động số dư ở docs.sepay.vn, hai dòng sản phẩm này có payload và cách xác thực hoàn toàn khác nhau).
 */
@ConfigurationProperties(prefix = "sepay.pg")
public record SePayPaymentProperties(
    SePayEnvironment env,
    String merchantId,
    // Khóa ký HMAC của merchant, đồng thời là password của Basic auth khi gọi Open API.
    String secretKey,
    // Giá trị của header X-Secret-Key trên IPN, cấu hình riêng ở dashboard (Payment Gateway ->
    // Cấu hình -> IPN). Tách khỏi secretKey một cách có chủ đích: dùng chung một giá trị nghĩa là
    // khóa dùng để KÝ bị gửi qua mạng trong mọi lần SePay gọi IPN.
    String ipnSecretKey,
    SePayCheckoutMethod paymentMethod,
    String returnBaseUrl
) {

    // Mặc định về sandbox thay vì để null: thiếu cấu hình mà mặc định vào production là kiểu hỏng
    // nguy hiểm nhất — nó thu tiền thật.
    public SePayPaymentProperties {
        env = env == null ? SePayEnvironment.SANDBOX : env;
        paymentMethod = paymentMethod == null ? SePayCheckoutMethod.BANK_TRANSFER : paymentMethod;
    }

    /** Đích POST của form checkout. */
    public String checkoutUrl() {
        return env.payBaseUrl() + "/v1/checkout/init";
    }

    /** Gốc của Open API, đã kèm version — SDK chính thức luôn gọi dưới dạng {base}/v1/{resource}. */
    public String apiBaseUrl() {
        return env.pgApiBaseUrl() + "/v1";
    }

    public boolean isConfigured() {
        return hasText(merchantId) && hasText(secretKey);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * URL được derive từ env chứ không khai từng cái trong yaml: hai cặp URL này đi theo nhau, khai
     * rời rạc thì một lần deploy quên đổi một dòng là form checkout trỏ production còn API tra cứu
     * vẫn trỏ sandbox — mọi hóa đơn sẽ bị đối soát thành "không tồn tại".
     */
    public enum SePayEnvironment {

        SANDBOX("https://pay-sandbox.sepay.vn", "https://pgapi-sandbox.sepay.vn"),
        PRODUCTION("https://pay.sepay.vn", "https://pgapi.sepay.vn");

        private final String payBaseUrl;
        private final String pgApiBaseUrl;

        SePayEnvironment(String payBaseUrl, String pgApiBaseUrl) {
            this.payBaseUrl = payBaseUrl;
            this.pgApiBaseUrl = pgApiBaseUrl;
        }

        public String payBaseUrl() {
            return payBaseUrl;
        }

        public String pgApiBaseUrl() {
            return pgApiBaseUrl;
        }
    }

    /** Giá trị hợp lệ của field {@code payment_method} trên form checkout. */
    public enum SePayCheckoutMethod {
        BANK_TRANSFER,
        NAPAS_BANK_TRANSFER,
        CARD
    }
}
