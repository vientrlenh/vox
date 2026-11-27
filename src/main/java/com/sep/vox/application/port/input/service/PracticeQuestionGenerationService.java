package com.sep.vox.application.port.input.service;

import com.sep.vox.domain.model.personalization.PracticeTopic;
import java.time.Duration;
import java.util.List;
import java.time.Instant;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.output.PracticeQuestionGenerationPort;
import com.sep.vox.application.port.output.PracticeQuestionGenerationPort.GeneratedQuestion;
import com.sep.vox.application.port.output.PracticeQuestionGenerationPort.TopicDetails;
import com.sep.vox.domain.model.framework.FrameworkResultBand;
import com.sep.vox.domain.model.personalization.PracticeQuestion;
import com.sep.vox.domain.repository.PracticeQuestionRepository;
import com.sep.vox.domain.repository.PracticeTopicRepository;
import com.sep.vox.domain.service.personalization.SubAttributePolicy;

@Service
@RequiredArgsConstructor
@Slf4j
public class PracticeQuestionGenerationService {

    private final PracticeTopicRepository practiceTopicRepository;
    private final PracticeQuestionRepository practiceQuestionRepository;
    private final PracticeQuestionGenerationPort questionGenerationPort;

    // Deliberately NOT @Transactional -- questionGenerationPort.generate() is a slow synchronous
    // HTTP call to the Python agents service (LLM generation, can take 10-20s). Wrapping this
    // in a transaction holds a HikariCP connection for that whole wait, starving the pool
    // under concurrent load (confirmed: caused the leak-detector to fire on an unrelated
    // simple SELECT elsewhere while this was in flight). Each repository call below
    // (saveGenerated, findTopicById via topicDetails) still gets its own short-lived
    // transaction from Spring Data's repository proxy -- no atomicity is lost.
    /**
     * @param bandCount the number of bands of the scale in use. Sent down to Python so it maps
     *                  {@code difficulty_rank} onto exactly that scale and builds the band-describing ladder from
     *                  real data, instead of defaulting to a 6-band VSTEP-style scale.
     */
    public int generateAndStore(
            UUID topicId, String criterionCode, String subAttribute, String targetTense, int targetRank,
            int count, Duration timeout, int bandCount, List<FrameworkResultBand> bandLadder,
            List<UUID> excludeQuestionIds) {
        List<PracticeQuestionGenerationPort.GeneratedQuestion> generated = questionGenerationPort.generate(
            topicDetails(topicId), criterionCode, SubAttributePolicy.plannedSubAttribute(criterionCode, subAttribute),
            targetTense, targetRank, count, timeout, bandCount, bandLadder, excludeQuestionIds
        );
        if (generated.isEmpty()) {
            warnNothingGenerated(topicId, criterionCode, subAttribute, targetRank, bandCount);
        }
        for (PracticeQuestionGenerationPort.GeneratedQuestion question : generated) {
            practiceQuestionRepository.saveGenerated(toPracticeQuestion(question));
        }
        indexGeneratedQuestions(generated);
        return generated.size();
    }

    /**
     * Python returning 200 with an empty array is the WORST failure mode: the loop in the caller does not
     * run, there is no exception and no log -- then the caller reports "the topic has no suitable practice question yet"
     * and nobody knows why. Many hours were lost tracing back exactly this case (a null sub-attribute
     * wiped out cleanly by CandidateFilterNode). Log clearly here so next time it is seen right away.
     */
    private void warnNothingGenerated(
            UUID topicId, String criterionCode, String subAttribute, int targetRank, int bandCount) {
        log.warn(
            "The generation pipeline returned 0 questions (HTTP 2xx) for topic={} criterion={} "
                + "subAttribute={} targetRank={} bandCount={} -- nothing to save.",
            topicId,
            criterionCode,
            subAttribute,
            targetRank,
            bandCount
        );
    }

    private void indexGeneratedQuestions(List<PracticeQuestionGenerationPort.GeneratedQuestion> generated) {
        for (PracticeQuestionGenerationPort.GeneratedQuestion question : generated) {
            try {
                questionGenerationPort.index(question);
            } catch (RuntimeException exception) {
                log.warn(
                    "Saved question {} but could not update Chroma.",
                    question.id(),
                    exception
                );
            }
        }
    }

    private TopicDetails topicDetails(UUID topicId) {
        PracticeTopic topic = practiceTopicRepository.findTopicById(topicId)
            .filter(candidate -> candidate.isActive())
            .orElseThrow(() -> new ResourceNotFoundException("Practice topic not found."));
        return new TopicDetails(
            topic.getId(),
            topic.getName(),
            topic.getInterestDimension(),
            topic.getCurriculumGroup()
        );
    }

    private PracticeQuestion toPracticeQuestion(GeneratedQuestion question) {
        return PracticeQuestion.generated(
                question.id(),
                question.topicId(),
                question.questionText(),
                question.criterionCode(),
                question.subAttribute(),
                question.targetTense(),
                question.difficultyRank(),
                question.difficultyFeaturesJson(),
                question.evaluationGuideJson(),
                question.suggestedIdeasJson(),
                question.questionType(),
                question.maxResponseSeconds(),
                question.minResponseSeconds(),
                question.vstepPart(),
                Instant.now());
    }
}
