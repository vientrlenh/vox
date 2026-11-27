package com.sep.vox.application.port.input.usecase.question;

import com.sep.vox.domain.model.question.Question;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.question.UpdateQuestionAssetCommand;
import com.sep.vox.application.port.input.service.QuestionEditAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.QuestionAssetDto;
import com.sep.vox.domain.mapper.QuestionAssetDtoMapper;
import com.sep.vox.domain.model.question.QuestionAsset;
import com.sep.vox.domain.model.question.QuestionAssetType;
import com.sep.vox.domain.repository.QuestionAssetRepository;
import com.sep.vox.domain.repository.QuestionRepository;

@Service
@RequiredArgsConstructor
public class UpdateQuestionAssetUseCase implements IUseCase<UpdateQuestionAssetCommand, QuestionAssetDto> {

    private final QuestionRepository questionRepository;
    private final QuestionAssetRepository questionAssetRepository;
    private final QuestionEditAccessService questionEditAccessService;
    private final QuestionCloneService questionCloneService;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public QuestionAssetDto execute(UpdateQuestionAssetCommand input) {
        UpdateQuestionAssetCommand command = normalize(input);
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();

        Question question = questionRepository.findById(command.questionId())
            .orElseThrow(() -> new ResourceNotFoundException("Question not found"));
        QuestionAsset sourceAsset = questionEditAccessService.requireAssetOf(question, command.assetId());
        boolean immutable = questionEditAccessService.authorizeEditing(question, currentUserId);

        QuestionAsset targetAsset = resolveTargetAsset(question, sourceAsset, immutable, currentUserId);

        applyProvidedFields(targetAsset, command);
        applyOrderIfProvided(targetAsset, command.order());

        sanitizeForType(targetAsset);
        validateContent(targetAsset);
        return saveAsDto(targetAsset);
    }

    private QuestionAsset resolveTargetAsset(
            Question question, QuestionAsset sourceAsset, boolean immutable, UUID currentUserId) {
        if (!immutable) {
            return sourceAsset;
        }
        Question clonedQuestion = questionCloneService.cloneAsDraftWithDetails(question, currentUserId);
        return questionCloneService.findClonedAsset(clonedQuestion.getId(), sourceAsset.getOrder());
    }

    private QuestionAssetDto saveAsDto(QuestionAsset targetAsset) {
        // File/type replace no longer auto-clears transcript/description -- per the simplified
        // rule, existing content (whether AI-written or manually typed) always sticks unless the
        // teacher explicitly blanks the field or uses "Regenerate with AI" to force regeneration.
        QuestionAsset saved = questionAssetRepository.save(targetAsset);
        // Auto-publishing the AI asset-analysis request is switched off for now so that saving an asset
        // stays independent and stable. It will be turned back on in a separate task about AI asset updates.
        // questionAssetAnalysisRequestPublisher.publishIfNeeded(targetQuestion, saved);
        return QuestionAssetDtoMapper.toDto(saved);
    }

    private void applyOrderIfProvided(QuestionAsset targetAsset, Integer order) {
        if (order != null) {
            validateAssetOrder(targetAsset.getQuestionId(), order, targetAsset.getId());
            targetAsset.setOrder(order);
        }
    }

    private void validateContent(QuestionAsset asset) {
        QuestionAssetContentValidator.validate(
            asset.getType(),
            asset.getUrl(),
            asset.getTranscript(),
            asset.getDescription(),
            asset.getAltText());
    }

    private void applyProvidedFields(QuestionAsset targetAsset, UpdateQuestionAssetCommand command) {
        if (command.title() != null) {
            targetAsset.setTitle(command.title());
        }
        if (command.durationSeconds() != null) {
            targetAsset.setDurationSeconds(command.durationSeconds());
        }
        if (command.altText() != null) {
            targetAsset.setAltText(command.altText());
        }
        if (command.type() != null) {
            targetAsset.setType(command.type());
        }
        if (command.url() != null) {
            targetAsset.setUrl(command.url());
        }
        if (command.transcript() != null) {
            targetAsset.setTranscript(command.transcript());
        }
        if (command.description() != null) {
            targetAsset.setDescription(command.description());
        }
    }

    private void validateAssetOrder(UUID questionId, int order, UUID currentAssetId) {
        boolean duplicated = questionAssetRepository.findByQuestionId(questionId).stream()
            .anyMatch(asset -> asset.getOrder() == order && !asset.getId().equals(currentAssetId));
        if (duplicated) {
            throw new IllegalArgumentException("Question asset order must not be duplicated");
        }
    }

    private UpdateQuestionAssetCommand normalize(UpdateQuestionAssetCommand input) {
        return new UpdateQuestionAssetCommand(
            input.questionId(),
            input.assetId(),
            StringNormalization.trimAndCollapseSpaces(input.title()),
            input.durationSeconds(),
            StringNormalization.trimAndCollapseSpaces(input.altText()),
            input.type(),
            StringNormalization.trimAndCollapseSpaces(input.url()),
            StringNormalization.trimAndCollapseSpaces(input.transcript()),
            StringNormalization.trimAndCollapseSpaces(input.description()),
            input.order()
        );
    }

    private void sanitizeForType(QuestionAsset asset) {
        if (!QuestionAssetAnalysisRequestPublisher.supportsTranscript(asset.getType())) {
            asset.setTranscript(null);
        }
        if (asset.getType() == QuestionAssetType.TEXT_PASSAGE) {
            asset.setUrl(null);
        }
    }
}
