package com.sep.vox.application.port.input.usecase.question;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.model.question.QuestionAsset;
import com.sep.vox.domain.repository.QuestionAssetRepository;
import com.sep.vox.domain.repository.QuestionEvaluationGuideRepository;
import com.sep.vox.domain.repository.QuestionRepository;

@Service
@RequiredArgsConstructor
public class QuestionCloneService {

    private final QuestionRepository questionRepository;
    private final QuestionAssetRepository questionAssetRepository;
    private final QuestionEvaluationGuideRepository questionEvaluationGuideRepository;

    @Transactional
    public Question cloneAsDraftWithDetails(Question source, UUID currentUserId) {
        Question clonedQuestion = source.draftCopyWithCode(cloneCodeOf(source.getCode()));
        Question savedQuestion = questionRepository.save(clonedQuestion);

        cloneAssets(source.getId(), savedQuestion.getId());
        cloneEvaluationGuide(source.getId(), savedQuestion.getId());

        return savedQuestion;
    }

    public QuestionAsset findClonedAsset(UUID clonedQuestionId, int order) {
        return questionAssetRepository.findByQuestionId(clonedQuestionId).stream()
            .filter(asset -> asset.getOrder() == order)
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài nguyên câu hỏi trong bản nháp mới"));
    }

    private void cloneAssets(UUID sourceQuestionId, UUID targetQuestionId) {
        List<QuestionAsset> sourceAssets = questionAssetRepository.findByQuestionId(sourceQuestionId);
        if (sourceAssets.isEmpty()) {
            return;
        }

        List<QuestionAsset> clonedAssets = sourceAssets.stream()
            .map(asset -> asset.copyTo(targetQuestionId))
            .toList();
        questionAssetRepository.saveAll(clonedAssets);
    }

    private void cloneEvaluationGuide(UUID sourceQuestionId, UUID targetQuestionId) {
        questionEvaluationGuideRepository.findByQuestionId(sourceQuestionId)
            .map(guide -> guide.copyTo(targetQuestionId))
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
