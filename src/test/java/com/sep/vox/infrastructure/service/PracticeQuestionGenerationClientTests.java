package com.sep.vox.infrastructure.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.output.PracticeQuestionGenerationPort.GeneratedQuestion;
import com.sep.vox.application.port.output.PracticeQuestionGenerationPort.TopicDetails;
import com.sep.vox.domain.model.framework.FrameworkResultBand;
import com.sun.net.httpserver.HttpServer;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Runs the client against a local JDK HTTP server: no Docker and no network.
 */
class PracticeQuestionGenerationClientTests {

    private static final String QUESTION_ID = "0190a0a0-0000-7000-8000-000000000001";
    private static final String TOPIC_ID = "0190a0a0-0000-7000-8000-000000000002";
    private static final String RESPONSE_ITEM = "{\"id\":\"" + QUESTION_ID + "\",\"topic_id\":\"" + TOPIC_ID
        + "\",\"question_text\":\"Describe your weekend.\",\"target_criterion_code\":\"FLUENCY\","
        + "\"target_sub_attribute\":null,\"target_tense\":\"past_simple\",\"difficulty_rank\":3,"
        + "\"difficulty_features\":{\"reasoning_type\":\"recount\"},\"evaluation_guide\":{\"focus\":[\"tense\"]},"
        + "\"suggested_ideas\":[\"park\",\"friends\"],\"question_type\":\"SHORT_ANSWER\","
        + "\"max_response_seconds\":60,\"min_response_seconds\":10,\"vstep_part\":1}";

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final AtomicReference<String> indexRequestBody = new AtomicReference<>();
    private HttpServer server;
    private PracticeQuestionGenerationClient client;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/internal/practice-generation/questions/index", exchange -> {
            indexRequestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.createContext("/internal/practice-generation/questions", exchange -> {
            byte[] body = ("{\"questions\":[" + RESPONSE_ITEM + "]}").getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        client = new PracticeQuestionGenerationClient(
            jsonMapper, "http://127.0.0.1:" + server.getAddress().getPort());
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void generateKeepsTheResponseItemAsJsonTextInSourceJson() {
        List<GeneratedQuestion> questions = generateOneQuestion();

        assertThat(questions).hasSize(1);
        GeneratedQuestion question = questions.get(0);
        assertThat(question.id()).isEqualTo(UUID.fromString(QUESTION_ID));
        assertThat(question.questionText()).isEqualTo("Describe your weekend.");
        assertThat(jsonMapper.readTree(question.sourceJson())).isEqualTo(jsonMapper.readTree(RESPONSE_ITEM));
    }

    @Test
    void indexSendsTheSourceJsonAsAnObjectUnderQuestion() {
        GeneratedQuestion question = generateOneQuestion().get(0);

        client.index(question);

        JsonNode sent = jsonMapper.readTree(indexRequestBody.get());
        assertThat(sent.size()).isEqualTo(1);
        assertThat(sent.path("question").isObject()).isTrue();
        assertThat(sent.path("question")).isEqualTo(jsonMapper.readTree(RESPONSE_ITEM));
    }

    private List<GeneratedQuestion> generateOneQuestion() {
        TopicDetails topic = new TopicDetails(UUID.fromString(TOPIC_ID), "Weekend", "LEISURE", "GRADE_10");
        return client.generate(topic, "FLUENCY", null, "past_simple", 3, 1, Duration.ofSeconds(5), 6,
            List.<FrameworkResultBand>of(), List.<UUID>of());
    }
}
