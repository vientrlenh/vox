package com.sep.vox.infrastructure.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration of the SePay payment gateway (SePay Payment Gateway -- NOT the Banking API/balance-change
 * webhook line at docs.sepay.vn, these two product lines have entirely different payloads and authentication).
 */
@ConfigurationProperties(prefix = "sepay.pg")
public record SePayPaymentProperties(
    SePayEnvironment env,
    String merchantId,
    // The merchant's HMAC signing key, and also the Basic auth password when calling the Open API.
    String secretKey,
    // The value of the X-Secret-Key header on the IPN, configured separately in the dashboard (Payment Gateway ->
    // Configuration -> IPN). Intentionally kept apart from secretKey: sharing one value would mean the
    // key used to SIGN is sent over the network on every IPN call from SePay.
    String ipnSecretKey,
    SePayCheckoutMethod paymentMethod,
    String returnBaseUrl
) {

    // Default to the sandbox instead of null: a missing configuration that defaults to production is the most dangerous
    // kind of failure -- it charges real money.
    public SePayPaymentProperties {
        env = env == null ? SePayEnvironment.SANDBOX : env;
        paymentMethod = paymentMethod == null ? SePayCheckoutMethod.BANK_TRANSFER : paymentMethod;
    }

    /** The POST target of the checkout form. */
    public String checkoutUrl() {
        return env.payBaseUrl() + "/v1/checkout/init";
    }

    /** Root of the Open API, already including the version — the official SDK always calls it as {base}/v1/{resource}. */
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
     * URLs are derived from the env rather than declared one by one in yaml: the two URL pairs go together, and declared
     * separately a deploy that forgets to change one line leaves the checkout form pointing to production while the lookup API
     * still points to the sandbox -- every invoice would be reconciled as "does not exist".
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

    /** Valid values of the {@code payment_method} field on the checkout form. */
    public enum SePayCheckoutMethod {
        BANK_TRANSFER,
        NAPAS_BANK_TRANSFER,
        CARD
    }
}
