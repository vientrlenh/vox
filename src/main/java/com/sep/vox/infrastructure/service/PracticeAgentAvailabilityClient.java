package com.sep.vox.infrastructure.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PracticeAgentAvailabilityClient {

    private final HttpClient httpClient;
    private final URI readinessUri;

    public PracticeAgentAvailabilityClient(
            @Value("${PRACTICE_AGENTS_BASE_URL:http://localhost:8000}")
            String agentsBaseUrl) {
        this.httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(2))
            .build();
        this.readinessUri = URI.create(
            agentsBaseUrl.replaceAll("/+$", "")
                + "/internal/voice-live/readiness"
        );
    }

    public void requireReady() {
        try {
            var request = HttpRequest.newBuilder(readinessUri)
                .timeout(Duration.ofSeconds(12))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
            var response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
            );
            if (response.statusCode() / 100 != 2) {
                throw new IllegalStateException(
                    "Voice Live readiness returned " + response.statusCode() + ": " + response.body()
                );
            }
        } catch (Exception exception) {
            throw new IllegalStateException(
                "Không thể kết nối dịch vụ AI để bắt đầu phiên luyện.",
                exception
            );
        }
    }
}
