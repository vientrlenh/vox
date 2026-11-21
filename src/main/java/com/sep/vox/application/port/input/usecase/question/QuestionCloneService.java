package com.sep.vox.application.port.input.usecase.question;

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
        Question clonedQuestion = Question.builder()
                .questionBankId(source.getQuestionBankId())
                .questionTopicId(source.getQuestionTopicId())
                .code(cloneCodeOf(source.getCode()))
                .instructionText(source.getInstructionText())
                .questionText(source.getQuestionText())
                .promptText(source.getPromptText())
                .preparationText(source.getPreparationText())
                .type(source.getType())
                .preparationTimeSeconds(source.getPreparationTimeSeconds())
                .minResponseSeconds(source.getMinResponseSeconds())
                .maxResponseSeconds(source.getMaxResponseSeconds())
                .sharing(source.getSharing())
                .sourceQuestionId(source.getId())
                .locked(false)
                .confidentiality(source.getConfidentiality())
                .securePoolId(source.getSecurePoolId())
                .status(QuestionStatus.DRAFT)
                .build();
        Question savedQuestion = questionRepository.save(clonedQuestion);

        cloneAssets(source.getId(), savedQuestion.getId());
        cloneEvaluationGuide(source.getId(), savedQuestion.getId());

        return savedQuestion;
    }

    private void cloneAssets(UUID sourceQuestionId, UUID targetQuestionId) {
        List<QuestionAsset> sourceAssets = questionAssetRepository.findByQuestionId(sourceQuestionId);
        if (sourceAssets.isEmpty()) {
            return;
        }

        List<QuestionAsset> clonedAssets = sourceAssets.stream()
            .map(asset -> QuestionAsset.builder()
                    .questionId(targetQuestionId)
                    .title(asset.getTitle())
                    .durationSeconds(asset.getDurationSeconds())
                    .altText(asset.getAltText())
                    .type(asset.getType())
                    .url(asset.getUrl())
                    .transcript(asset.getTranscript())
                    .description(asset.getDescription())
                    .order(asset.getOrder())
                    .build())
            .toList();
        questionAssetRepository.saveAll(clonedAssets);
    }

    private void cloneEvaluationGuide(UUID sourceQuestionId, UUID targetQuestionId) {
        questionEvaluationGuideRepository.findByQuestionId(sourceQuestionId)
            .map(guide -> QuestionEvaluationGuide.builder()
                    .questionId(targetQuestionId)
                    .expectedContent(guide.getExpectedContent())
                    .keyPoints(guide.getKeyPoints())
                    .acceptableResponses(guide.getAcceptableResponses())
                    .offTopicExamples(guide.getOffTopicExamples())
                    .scoringHints(guide.getScoringHints())
                    .commonMistakes(guide.getCommonMistakes())
                    .build())
            .ifPresent(questionEvaluationGuideRepository::save);
    }

    private String cloneCodeOf(String sourceCode) {
        String suffix = "-COPY-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        if (sourceCode == null || sourceCode.isBlank()) {
            return "Q" + suffix;
        }
        int maxBaseLength = Math.max(0, 100 - suffix.length());
        String base = sourceCode.length() > maxBaseLength
            ? sourceCode.substring(0, maxBaseLength)
            : sourceCode;
        return base + suffix;
    }
}
