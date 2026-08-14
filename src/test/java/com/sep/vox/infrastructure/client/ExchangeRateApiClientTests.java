package com.sep.vox.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Map;

import org.junit.jupiter.api.Test;

class ExchangeRateApiClientTests {

    @Test
    void extractRateReadsRatesVndFromResponse() {
        var response = Map.of("result", "success", "rates", Map.of("VND", 26777.5));

        assertThat(ExchangeRateApiClient.extractRate(response)).isEqualByComparingTo(new BigDecimal("26777.5"));
    }

    @Test
    void extractRateReturnsNullWhenRatesMissing() {
        var response = Map.of("result", "success");

        assertThat(ExchangeRateApiClient.extractRate(response)).isNull();
    }

    @Test
    void extractRateReturnsNullWhenVndMissing() {
        var response = Map.of("rates", Map.of("EUR", 0.9));

        assertThat(ExchangeRateApiClient.extractRate(response)).isNull();
    }

    @Test
    void extractRateReturnsNullWhenResponseNull() {
        assertThat(ExchangeRateApiClient.extractRate(null)).isNull();
    }
}
