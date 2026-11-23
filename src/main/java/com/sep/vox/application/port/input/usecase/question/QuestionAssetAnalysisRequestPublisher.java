package com.sep.vox.application.port.input.usecase.question;

import com.sep.vox.domain.model.question.QuestionEvaluationGuide;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.sep.vox.application.event.QuestionAssetAnalysisRequestedExternalEvent;
import com.sep.vox.application.port.output.ExternalEventPublisherPort;
import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.model.question.QuestionAsset;
import com.sep.vox.domain.model.question.QuestionAssetType;
import com.sep.vox.domain.repository.QuestionEvaluationGuideRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionAssetAnalysisRequestPublisher {

    private final QuestionEvaluationGuideRepository questionEvaluationGuideRepository;
    private final ExternalEventPublisherPort externalEventPublisherPort;

    public void publishIfNeeded(Question question, QuestionAsset asset) {
        // AI only ever fills a field that is currently blank -- see the matching check in
        // QuestionAssetAnalysisCompletedConsumer. Whatever is already in transcript/description
        // (typed by a person or previously written by AI) always wins and is never regenerated
        // here; only the explicit "regenerate" action (RegenerateQuestionAssetAnalysisUseCase)
        // clears a field first so AI is allowed to overwrite it.
        boolean transcriptNeedsGeneration = supportsAiTranscript(asset.getType())
            && isBlank(asset.getTranscript());
        boolean descriptionNeedsGeneration = supportsDescription(asset.getType())
            && isBlank(asset.getDescription());

        if (!transcriptNeedsGeneration && !descriptionNeedsGeneration) {
            return;
        }

        publish(question, asset);
    }

    public void publish(Question question, QuestionAsset asset) {
        QuestionEvaluationGuide guide = questionEvaluationGuideRepository.findByQuestionId(question.getId()).orElse(null);
        QuestionAssetAnalysisRequestedExternalEvent event = new QuestionAssetAnalysisRequestedExternalEvent(
            asset.getId().toString(),
            question.getId().toString(),
            new QuestionAssetAnalysisRequestedExternalEvent.Payload(
                asset.getType().name(),
                asset.getUrl(),
                question.getQuestionText(),
                guide == null
                    ? null
                    : new QuestionAssetAnalysisRequestedExternalEvent.EvaluationGuide(
                        guide.getExpectedContent(),
                        guide.getKeyPoints(),
                        guide.getAcceptableResponses(),
                        guide.getOffTopicExamples(),
                        guide.getScoringHints(),
                        guide.getCommonMistakes()
                    ),
                isBlank(asset.getTranscript()) ? null : asset.getTranscript(),
                isBlank(asset.getDescription()) ? null : asset.getDescription()
            )
        );

        try {
            externalEventPublisherPort.publish(event);
        } catch (Exception ex) {
            log.warn("Không thể publish yêu cầu phân tích asset questionAssetId={}", asset.getId(), ex);
        }
    }

    public static boolean supportsTranscript(QuestionAssetType type) {
        return type == QuestionAssetType.AUDIO
            || type == QuestionAssetType.VIDEO
            || type == QuestionAssetType.TEXT_PASSAGE;
    }

    public static boolean supportsAiTranscript(QuestionAssetType type) {
        return type == QuestionAssetType.AUDIO || type == QuestionAssetType.VIDEO;
    }

    public static boolean supportsDescription(QuestionAssetType type) {
        return type == QuestionAssetType.IMAGE
            || type == QuestionAssetType.AUDIO
            || type == QuestionAssetType.VIDEO
            || type == QuestionAssetType.TEXT_PASSAGE;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
