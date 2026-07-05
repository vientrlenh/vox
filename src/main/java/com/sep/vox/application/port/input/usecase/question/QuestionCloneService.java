package com.sep.vox.application.port.input.usecase.question;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.model.question.QuestionAsset;
import com.sep.vox.domain.model.question.QuestionEvaluationGuide;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.domain.repository.QuestionAssetRepository;
import com.sep.vox.domain.repository.QuestionEvaluationGuideRepository;
import com.sep.vox.domain.repository.QuestionRepository;

@Service
public class QuestionCloneService {

    private final QuestionRepository questionRepository;
    private final QuestionAssetRepository questionAssetRepository;
    private final QuestionEvaluationGuideRepository questionEvaluationGuideRepository;

    public QuestionCloneService(
            QuestionRepository questionRepository,
            QuestionAssetRepository questionAssetRepository,
            QuestionEvaluationGuideRepository questionEvaluationGuideRepository) {
        this.questionRepository = questionRepository;
        this.questionAssetRepository = questionAssetRepository;
        this.questionEvaluationGuideRepository = questionEvaluationGuideRepository;
    }

    @Transactional
    public Question cloneAsDraftWithDetails(Question source, UUID currentUserId) {
        var now = OffsetDateTime.now();
        var clonedQuestion = new Question(
            source.getQuestionBankId(),
            source.getQuestionTopicId(),
            cloneCodeOf(source.getCode()),
            source.getInstructionText(),
            source.getQuestionText(),
            source.getPromptText(),
            source.getPreparationText(),
            source.getType(),
            source.getPreparationTimeSeconds(),
            source.getMinResponseSeconds(),
            source.getMaxResponseSeconds(),
            source.getSharing(),
            source.getId(),
            false,
            source.getConfidentiality(),
            source.getSecurePoolId(),
            QuestionStatus.DRAFT,
            now,
            now,
            currentUserId,
            currentUserId
        );
        var savedQuestion = questionRepository.save(clonedQuestion);

        cloneAssets(source.getId(), savedQuestion.getId());
        cloneEvaluationGuide(source.getId(), savedQuestion.getId());

        return savedQuestion;
    }

    private void cloneAssets(UUID sourceQuestionId, UUID targetQuestionId) {
        List<QuestionAsset> sourceAssets = questionAssetRepository.findByQuestionId(sourceQuestionId);
        if (sourceAssets.isEmpty()) {
            return;
        }

        var clonedAssets = sourceAssets.stream()
            .map(asset -> new QuestionAsset(
                targetQuestionId,
                asset.getTitle(),
                asset.getDurationSeconds(),
                asset.getAltText(),
                asset.getType(),
                asset.getUrl(),
                asset.getTranscript(),
                asset.getDescription(),
                asset.getOrder()
            ))
            .toList();
        questionAssetRepository.saveAll(clonedAssets);
    }

    private void cloneEvaluationGuide(UUID sourceQuestionId, UUID targetQuestionId) {
        questionEvaluationGuideRepository.findByQuestionId(sourceQuestionId)
            .map(guide -> new QuestionEvaluationGuide(
                targetQuestionId,
                guide.getExpectedContent(),
                guide.getKeyPoints(),
                guide.getAcceptableResponses(),
                guide.getOffTopicExamples(),
                guide.getScoringHints(),
                guide.getCommonMistakes()
            ))
            .ifPresent(questionEvaluationGuideRepository::save);
    }

    private String cloneCodeOf(String sourceCode) {
        var suffix = "-COPY-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        if (sourceCode == null || sourceCode.isBlank()) {
            return "Q" + suffix;
        }
        var maxBaseLength = Math.max(0, 100 - suffix.length());
        var base = sourceCode.length() > maxBaseLength
            ? sourceCode.substring(0, maxBaseLength)
            : sourceCode;
        return base + suffix;
    }
}
