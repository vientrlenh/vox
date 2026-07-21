package com.sep.vox.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.grpc.test.autoconfigure.AutoConfigureTestGrpcTransport;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.config.ContainerTestConfig;
import com.sep.vox.infrastructure.persistence.entity.ExamItemEvaluationJpaEntity;

/**
 * Guards the status filter that keeps unpublished appeal re-grades out of the
 * score calculator. The filter must be a no-op for the AI-only data that exists
 * today, hide reviewer reports while an appeal is in progress, and switch over
 * once the appeal is published.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestGrpcTransport
@Transactional
class SpringDataExamItemEvaluationRepositoryTests extends ContainerTestConfig {

    @Autowired
    private SpringDataExamItemEvaluationRepository repository;

    private UUID responseId;
    private UUID paperItemId;
    private OffsetDateTime baseTime;

    @BeforeEach
    void setUp() {
        responseId = UUID.randomUUID();
        paperItemId = UUID.randomUUID();
        baseTime = OffsetDateTime.parse("2026-07-15T09:00:00+07:00");
    }

    @Test
    void should_return_ai_evaluation_when_only_auto_graded_exists() {
        var ai = save("AI_SINGLE", "AUTO_GRADED", new BigDecimal("6.00"), baseTime);

        var latest = repository.findLatestByResponseId(responseId);
        var batched = repository.findLatestByResponseIdIn(List.of(responseId));

        assertThat(latest).isPresent();
        assertThat(latest.get().getId()).isEqualTo(ai.getId());
        assertThat(batched).hasSize(1);
        assertThat(batched.get(0).getId()).isEqualTo(ai.getId());
    }

    @Test
    void should_still_return_ai_evaluation_when_newer_reviewer_report_is_under_review() {
        var ai = save("AI_SINGLE", "AUTO_GRADED", new BigDecimal("6.00"), baseTime);
        save("HUMAN", "UNDER_REVIEW", new BigDecimal("8.00"), baseTime.plusDays(1));
        save("HUMAN", "UNDER_REVIEW", new BigDecimal("8.50"), baseTime.plusDays(2));

        var latest = repository.findLatestByResponseId(responseId);
        var batched = repository.findLatestByResponseIdIn(List.of(responseId));

        assertThat(latest).isPresent();
        assertThat(latest.get().getId()).isEqualTo(ai.getId());
        assertThat(latest.get().getItemScore()).isEqualByComparingTo("6.00");
        // The batched query must not drop the response: if the MAX subquery
        // ignored the status filter it would return the UNDER_REVIEW timestamp,
        // match no row, and the calculator would fail with NotFoundException.
        assertThat(batched).hasSize(1);
        assertThat(batched.get(0).getId()).isEqualTo(ai.getId());
    }

    @Test
    void should_return_finalized_evaluation_when_appeal_published() {
        save("AI_SINGLE", "SUPERSEDED", new BigDecimal("6.00"), baseTime);
        save("HUMAN", "SUPERSEDED", new BigDecimal("8.00"), baseTime.plusDays(1));
        var published = save("HUMAN", "FINALIZED", new BigDecimal("7.50"), baseTime.plusDays(2));

        var latest = repository.findLatestByResponseId(responseId);
        var batched = repository.findLatestByResponseIdIn(List.of(responseId));

        assertThat(latest).isPresent();
        assertThat(latest.get().getId()).isEqualTo(published.getId());
        assertThat(latest.get().getItemScore()).isEqualByComparingTo("7.50");
        assertThat(batched).hasSize(1);
        assertThat(batched.get(0).getId()).isEqualTo(published.getId());
    }

    @Test
    void should_return_empty_when_every_evaluation_is_hidden() {
        save("HUMAN", "UNDER_REVIEW", new BigDecimal("8.00"), baseTime);

        assertThat(repository.findLatestByResponseId(responseId)).isEmpty();
        assertThat(repository.findLatestByResponseIdIn(List.of(responseId))).isEmpty();
    }

    private ExamItemEvaluationJpaEntity save(
            String engineType, String status, BigDecimal itemScore, OffsetDateTime evaluatedAt) {
        var entity = new ExamItemEvaluationJpaEntity(
            null,
            responseId,
            paperItemId,
            engineType,
            "HUMAN".equals(engineType) ? "HUMAN" : "gpt-test",
            null,
            "HUMAN".equals(engineType) ? UUID.randomUUID() : null,
            itemScore,
            itemScore,
            null,
            false,
            null,
            false,
            false,
            null,
            null,
            null,
            null,
            null,
            status,
            evaluatedAt
        );
        return repository.saveAndFlush(entity);
    }
}
