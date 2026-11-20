package com.sep.vox.infrastructure.client;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.sep.vox.infrastructure.properties.ExchangeRateApiProperties;

/**
 * Gọi API tỷ giá USD->VND thật (miễn phí, không cần key -- xem ExchangeRateApiProperties) cho
 * ExchangeRateRefreshJob. Không bao giờ throw ra ngoài: job chạy nền, không ai xử lý exception,
 * nên mọi lỗi (mạng, response dị dạng, giá trị bất thường) đều quy về Optional.empty() kèm log --
 * phía gọi giữ nguyên snapshot mới nhất / fallback tĩnh hiện có.
 */
@Component
public class ExchangeRateApiClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExchangeRateApiClient.class);

    // Job chạy nền hằng ngày, không ai chờ trực tiếp -- timeout ngắn chỉ để tránh treo cả lượt chạy.
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
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT);
        factory.setReadTimeout(READ_TIMEOUT);
        return factory;
    }

    public Optional<BigDecimal> fetchUsdToVndRate() {
        Map<?, ?> response;
        try {
            response = restClient.get().retrieve().body(Map.class);
        } catch (RuntimeException ex) {
            LOGGER.warn("[exchange-rate] Gọi API tỷ giá thất bại, giữ nguyên snapshot/fallback hiện có: {}",
                ex.getMessage());
            return Optional.empty();
        }

        var rate = extractRate(response);
        if (rate == null) {
            LOGGER.warn("[exchange-rate] Response API tỷ giá thiếu rates.VND hợp lệ: {}", response);
            return Optional.empty();
        }
        if (rate.compareTo(properties.minRateBound()) < 0 || rate.compareTo(properties.maxRateBound()) > 0) {
            LOGGER.warn("[exchange-rate] Tỷ giá {} nằm ngoài khoảng an toàn [{}, {}], bỏ qua lần fetch này",
                rate, properties.minRateBound(), properties.maxRateBound());
            return Optional.empty();
        }
        return Optional.of(rate);
    }

    /** Tách riêng để test được logic parse thuần, không cần gọi mạng thật. */
    static BigDecimal extractRate(Map<?, ?> response) {
        if (response == null) {
            return null;
        }
        if (!(response.get("rates") instanceof Map<?, ?> rates)) {
            return null;
        }
        var vnd = rates.get("VND");
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
