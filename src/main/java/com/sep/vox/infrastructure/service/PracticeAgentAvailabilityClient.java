package com.sep.vox.infrastructure.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sep.vox.application.port.output.PracticeAgentAvailabilityPort;

@Service
public class PracticeAgentAvailabilityClient implements PracticeAgentAvailabilityPort {

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

    @Override
    public void requireReady() {
        try {
            HttpRequest request = HttpRequest.newBuilder(readinessUri)
                .timeout(Duration.ofSeconds(12))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
            HttpResponse<String> response = httpClient.send(
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
                "Cannot connect to the AI service to start the practice session.",
                exception
            );
        }
    }
}
