package com.sep.vox.application.port.input.service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.domain.model.personalization.PracticeQuestion;
import com.sep.vox.domain.repository.personalization.PracticeQuestionRepository;
import com.sep.vox.domain.repository.personalization.PracticeTopicRepository;
import com.sep.vox.domain.service.personalization.SubAttributePolicy;
import com.sep.vox.infrastructure.service.PracticeQuestionGenerationClient;
import com.sep.vox.infrastructure.service.PracticeQuestionGenerationClient.GeneratedQuestion;
import com.sep.vox.infrastructure.service.PracticeQuestionGenerationClient.TopicDetails;

@Service
public class PracticeQuestionGenerationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PracticeQuestionGenerationService.class);

    private final PracticeTopicRepository practiceTopicRepository;
    private final PracticeQuestionRepository practiceQuestionRepository;
    private final PracticeQuestionGenerationClient generationClient;

    public PracticeQuestionGenerationService(
            PracticeTopicRepository practiceTopicRepository,
            PracticeQuestionRepository practiceQuestionRepository,
            PracticeQuestionGenerationClient generationClient) {
        this.practiceTopicRepository = practiceTopicRepository;
        this.practiceQuestionRepository = practiceQuestionRepository;
        this.generationClient = generationClient;
    }

    // Deliberately NOT @Transactional -- generationClient.generate() is a slow synchronous
    // HTTP call to the Python agents service (LLM generation, can take 10-20s). Wrapping this
    // in a transaction holds a HikariCP connection for that whole wait, starving the pool
    // under concurrent load (confirmed: caused the leak-detector to fire on an unrelated
    // simple SELECT elsewhere while this was in flight). Each repository call below
    // (saveGenerated, findTopicById via topicDetails) still gets its own short-lived
    // transaction from Spring Data's repository proxy -- no atomicity is lost.
    public int generateAndStore(
            UUID topicId,
            String criterionCode,
            String subAttribute,
            int targetRank,
            int count,
            Duration timeout) {
        var generated = generationClient.generate(
            topicDetails(topicId),
            criterionCode,
            SubAttributePolicy.plannedSubAttribute(criterionCode, subAttribute),
            targetRank,
            count,
            timeout
        );
        for (var question : generated) {
            practiceQuestionRepository.saveGenerated(toPracticeQuestion(question));
        }
        for (var question : generated) {
            try {
                generationClient.index(question);
            } catch (RuntimeException exception) {
                LOGGER.warn(
                    "Đã lưu câu {} nhưng chưa cập nhật được Chroma.",
                    question.id(),
                    exception
                );
            }
        }
        return generated.size();
    }

    private TopicDetails topicDetails(UUID topicId) {
        var topic = practiceTopicRepository.findTopicById(topicId)
            .filter(com.sep.vox.domain.model.personalization.PracticeTopic::active)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy chủ đề luyện tập."));
        return new TopicDetails(
            topic.id(),
            topic.name(),
            topic.interestDimension(),
            topic.curriculumGroup()
        );
    }

    private PracticeQuestion toPracticeQuestion(GeneratedQuestion question) {
        return new PracticeQuestion(
            question.id(),
            question.topicId(),
            question.questionText(),
            question.criterionCode(),
            question.subAttribute(),
            question.difficultyRank(),
            question.difficultyFeaturesJson(),
            question.evaluationGuideJson(),
            question.suggestedIdeasJson(),
            question.preparationTimeSeconds(),
            question.maxResponseSeconds(),
            question.maxFollowupSeconds(),
            question.vstepPart(),
            null,
            0,
            true,
            OffsetDateTime.now()
        );
    }
}
