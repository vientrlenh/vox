package com.sep.vox.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Map;

import org.junit.jupiter.api.Test;

class ExchangeRateApiClientTests {

    @Test
    void extractRateReadsRatesVndFromResponse() {
        Map<String, Object> response = Map.of("result", "success", "rates", Map.of("VND", 26777.5));

        assertThat(ExchangeRateApiClient.extractRate(response)).isEqualByComparingTo(new BigDecimal("26777.5"));
    }

    @Test
    void extractRateReturnsNullWhenRatesMissing() {
        Map<String, String> response = Map.of("result", "success");

        assertThat(ExchangeRateApiClient.extractRate(response)).isNull();
    }

    @Test
    void extractRateReturnsNullWhenVndMissing() {
        Map<String, Map<String, Double>> response = Map.of("rates", Map.of("EUR", 0.9));

        assertThat(ExchangeRateApiClient.extractRate(response)).isNull();
    }

    @Test
    void extractRateReturnsNullWhenResponseNull() {
        assertThat(ExchangeRateApiClient.extractRate(null)).isNull();
    }
}
