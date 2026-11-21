package com.sep.vox.application.port.output;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.framework.FrameworkResultBand;

/**
 * Generates new practice questions with AI and indexes the questions just generated. Implemented by
 * PracticeQuestionGenerationClient.
 *
 * <p>UNLIKE the other AI ports, this port does NOT degrade gracefully -- a failed service call throws
 * IllegalStateException, because if no question can be generated there is nothing to store and the caller needs to know.
 */
public interface PracticeQuestionGenerationPort {

    /**
     * @param bandCount  the number of bands of the scale in use -- Python uses it to map difficulty_rank onto the
     *                   right scale, instead of defaulting to 6 VSTEP-style bands.
     * @param bandLadder a description of each band, so the grading prompt speaks the school's own scale. When
     *                   empty Python falls back to its own default ladder.
     */
    List<GeneratedQuestion> generate(
        TopicDetails topic,
        String criterionCode,
        String subAttribute,
        String targetTense,
        int targetRank,
        int count,
        Duration timeout,
        int bandCount,
        List<FrameworkResultBand> bandLadder,
        List<UUID> excludeQuestionIds);

    void index(GeneratedQuestion question);

    record TopicDetails(
        UUID id,
        String name,
        String interestDimension,
        String curriculumGroup
    ) {
    }

    /**
     * sourceJson holds the raw JSON object Python returned, as JSON text -- {@link #index} sends that same object
     * back to Chroma as a JSON object, not as a string inside a string.
     */
    record GeneratedQuestion(
        UUID id,
        UUID topicId,
        String questionText,
        String criterionCode,
        String subAttribute,
        String targetTense,
        int difficultyRank,
        String difficultyFeaturesJson,
        String evaluationGuideJson,
        String suggestedIdeasJson,
        String questionType,
        int maxResponseSeconds,
        int minResponseSeconds,
        int vstepPart,
        String sourceJson
    ) {
    }
}
