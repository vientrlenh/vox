package com.sep.vox.infrastructure.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sepay.pg")
public record SePayPaymentProperties(
    String merchantId, 
    String secretKey, 
    String checkoutUrl, 
    String apiBaseUrl, 
    String returnBaseUrl
) {
    
}
