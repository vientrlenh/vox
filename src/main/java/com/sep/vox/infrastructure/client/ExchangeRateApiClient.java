package com.sep.vox.infrastructure.client;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.sep.vox.infrastructure.properties.ExchangeRateApiProperties;

/**
 * Calls the real USD->VND exchange rate API (free, no key needed -- see ExchangeRateApiProperties) for
 * ExchangeRateRefreshJob. Never throws outward: the job runs in the background, nobody handles an exception,
 * so every failure (network, malformed response, abnormal value) is turned into Optional.empty() with a log --
 * the caller keeps the latest existing snapshot / static fallback.
 */
@Component
@Slf4j
public class ExchangeRateApiClient {

    // Daily background job that nobody waits on directly -- the short timeout only keeps a whole run from hanging.
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

    private final ExchangeRateApiProperties properties;
    private final RestClient restClient;

    public ExchangeRateApiClient(ExchangeRateApiProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
            .baseUrl(properties.baseUrl())
            .requestFactory(requestFactory())
            .build();
    }

    private static ClientHttpRequestFactory requestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT);
        factory.setReadTimeout(READ_TIMEOUT);
        return factory;
    }

    public Optional<BigDecimal> fetchUsdToVndRate() {
        Map<?, ?> response;
        try {
            response = restClient.get().retrieve().body(Map.class);
        } catch (RuntimeException ex) {
            log.warn("[exchange-rate] Exchange rate API call failed, keeping the existing snapshot/fallback: {}",
                ex.getMessage());
            return Optional.empty();
        }

        BigDecimal rate = extractRate(response);
        if (rate == null) {
            log.warn("[exchange-rate] Exchange rate API response lacks a valid rates.VND: {}", response);
            return Optional.empty();
        }
        if (rate.compareTo(properties.minRateBound()) < 0 || rate.compareTo(properties.maxRateBound()) > 0) {
            log.warn("[exchange-rate] Rate {} is outside the safe range [{}, {}], skipping this fetch",
                rate, properties.minRateBound(), properties.maxRateBound());
            return Optional.empty();
        }
        return Optional.of(rate);
    }

    /** Split out so the pure parsing logic can be tested without making a real network call. */
    static BigDecimal extractRate(Map<?, ?> response) {
        if (response == null) {
            return null;
        }
        if (!(response.get("rates") instanceof Map<?, ?> rates)) {
            return null;
        }
        Object vnd = rates.get("VND");
        if (vnd == null) {
            return null;
        }
        try {
            return new BigDecimal(vnd.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
