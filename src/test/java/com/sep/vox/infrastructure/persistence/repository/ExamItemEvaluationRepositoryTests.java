package com.sep.vox.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.sep.vox.config.ContainerTestConfig;
import com.sep.vox.domain.model.exam.ExamEvaluationEngineType;
import com.sep.vox.domain.model.exam.ExamItemEvaluation;
import com.sep.vox.domain.model.exam.ExamItemEvaluationStatus;
import com.sep.vox.domain.repository.ExamItemEvaluationRepository;
import com.sep.vox.infrastructure.persistence.adapter.ExamItemEvaluationRepositoryImpl;

/**
 * {@code findLatestAiByResponseId} cố ý KHÔNG lọc theo status — đó là điểm khác biệt duy
 * nhất so với {@code findLatestByResponseId}, và cũng là lý do nó tồn tại: sau khi giáo
 * viên chấm lại, bản AI mang SUPERSEDED nhưng nó vẫn là nơi duy nhất có lượt nói và bằng
 * chứng AI. Viết bằng Testcontainers vì cái cần kiểm là JPQL thật, không phải mock.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(ExamItemEvaluationRepositoryImpl.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ExamItemEvaluationRepositoryTests extends ContainerTestConfig {

    @Autowired
    private ExamItemEvaluationRepository examItemEvaluationRepository;

    @Test
    void should_return_superseded_ai_evaluation_as_the_latest_ai() {
        var responseId = UUID.randomUUID();
        var ai = examItemEvaluationRepository.save(evaluation(responseId,
            ExamEvaluationEngineType.AI_SINGLE, ExamItemEvaluationStatus.SUPERSEDED, hoursAgo(2)));
        examItemEvaluationRepository.save(evaluation(responseId,
            ExamEvaluationEngineType.HUMAN, ExamItemEvaluationStatus.FINALIZED, hoursAgo(1)));

        var found = examItemEvaluationRepository.findLatestAiByResponseId(responseId);

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(ai.getId());
        assertThat(found.get().getStatus()).isEqualTo(ExamItemEvaluationStatus.SUPERSEDED);
    }

    /** Bản hiệu lực vẫn phải là bản chấm tay — hai query không được lẫn vào nhau. */
    @Test
    void should_keep_returning_the_human_row_as_the_authoritative_evaluation() {
        var responseId = UUID.randomUUID();
        examItemEvaluationRepository.save(evaluation(responseId,
            ExamEvaluationEngineType.AI_SINGLE, ExamItemEvaluationStatus.SUPERSEDED, hoursAgo(2)));
        var human = examItemEvaluationRepository.save(evaluation(responseId,
            ExamEvaluationEngineType.HUMAN, ExamItemEvaluationStatus.FINALIZED, hoursAgo(1)));

        var found = examItemEvaluationRepository.findLatestByResponseId(responseId);

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(human.getId());
    }

    @Test
    void should_prefer_the_newest_ai_evaluation_when_several_exist() {
        var responseId = UUID.randomUUID();
        examItemEvaluationRepository.save(evaluation(responseId,
            ExamEvaluationEngineType.AI_SINGLE, ExamItemEvaluationStatus.SUPERSEDED, hoursAgo(5)));
        var newest = examItemEvaluationRepository.save(evaluation(responseId,
            ExamEvaluationEngineType.AI_ENSEMBLE, ExamItemEvaluationStatus.AUTO_GRADED, hoursAgo(1)));

        var found = examItemEvaluationRepository.findLatestAiByResponseId(responseId);

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(newest.getId());
    }

    /**
     * UNDER_REVIEW là báo cáo phúc khảo CHƯA công bố. Bình thường nó mang engine HUMAN nên
     * không lọt vào query này, nhưng bộ lọc được viết tường minh để nếu sau có ai ghi bản
     * review bằng engine AI thì học sinh vẫn không thấy điểm chưa công bố.
     */
    @Test
    void should_ignore_an_under_review_ai_row() {
        var responseId = UUID.randomUUID();
        var published = examItemEvaluationRepository.save(evaluation(responseId,
            ExamEvaluationEngineType.AI_SINGLE, ExamItemEvaluationStatus.SUPERSEDED, hoursAgo(3)));
        examItemEvaluationRepository.save(evaluation(responseId,
            ExamEvaluationEngineType.AI_SINGLE, ExamItemEvaluationStatus.UNDER_REVIEW, hoursAgo(1)));

        var found = examItemEvaluationRepository.findLatestAiByResponseId(responseId);

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(published.getId());
    }

    @Test
    void should_return_empty_when_the_response_only_has_a_human_evaluation() {
        var responseId = UUID.randomUUID();
        examItemEvaluationRepository.save(evaluation(responseId,
            ExamEvaluationEngineType.HUMAN, ExamItemEvaluationStatus.FINALIZED, hoursAgo(1)));

        assertThat(examItemEvaluationRepository.findLatestAiByResponseId(responseId)).isEmpty();
    }

    @Test
    void should_not_leak_an_ai_evaluation_of_another_response() {
        var responseId = UUID.randomUUID();
        examItemEvaluationRepository.save(evaluation(UUID.randomUUID(),
            ExamEvaluationEngineType.AI_SINGLE, ExamItemEvaluationStatus.AUTO_GRADED, hoursAgo(1)));

        assertThat(examItemEvaluationRepository.findLatestAiByResponseId(responseId)).isEmpty();
    }

    private static Instant hoursAgo(int hours) {
        return Instant.now().minus(hours, ChronoUnit.HOURS);
    }

    private static ExamItemEvaluation evaluation(
            UUID responseId,
            ExamEvaluationEngineType engineType,
            ExamItemEvaluationStatus status,
            Instant evaluatedAt) {
        return new ExamItemEvaluation(
            null, responseId, UUID.randomUUID(), engineType,
            engineType == ExamEvaluationEngineType.HUMAN ? "HUMAN" : "gpt-x",
            null, null, new BigDecimal("6.00"), new BigDecimal("6.00"),
            null, false, null, false, false,
            null, null, null, null, null,
            status, evaluatedAt);
    }
}
