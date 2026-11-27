package com.sep.vox.application.port.input.usecase.question;

import com.sep.vox.domain.model.question.Question;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.question.CreateQuestionAssetMutationCommand;
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
public class CreateQuestionAssetUseCase implements IUseCase<CreateQuestionAssetMutationCommand, QuestionAssetDto> {

    private final QuestionRepository questionRepository;
    private final QuestionAssetRepository questionAssetRepository;
    private final QuestionEditAccessService questionEditAccessService;
    private final QuestionCloneService questionCloneService;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public QuestionAssetDto execute(CreateQuestionAssetMutationCommand input) {
        CreateQuestionAssetMutationCommand command = normalize(input);
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();

        Question question = questionRepository.findById(command.questionId())
            .orElseThrow(() -> new ResourceNotFoundException("Question not found"));
        boolean immutable = questionEditAccessService.authorizeEditing(question, currentUserId);

        Question targetQuestion = immutable
            ? questionCloneService.cloneAsDraftWithDetails(question, currentUserId)
            : question;

        validateNewAsset(targetQuestion.getId(), command);

        QuestionAsset saved = questionAssetRepository.save(buildAsset(targetQuestion.getId(), command));
        // Auto-publishing the AI asset-analysis request is switched off for now so that saving an asset
        // stays independent and stable. It will be turned back on in a separate task about AI asset updates.
        // questionAssetAnalysisRequestPublisher.publishIfNeeded(targetQuestion, saved);
        return QuestionAssetDtoMapper.toDto(saved);
    }

    private void validateNewAsset(UUID questionId, CreateQuestionAssetMutationCommand command) {
        if (questionAssetRepository.existsByQuestionId(questionId)) {
            throw new IllegalArgumentException("The question already has an asset; edit or delete the existing asset first");
        }

        validateAssetOrder(questionId, command.order(), null);
        QuestionAssetContentValidator.validate(
            command.type(),
            command.url(),
            command.transcript(),
            command.description(),
            command.altText());
    }

    private QuestionAsset buildAsset(UUID questionId, CreateQuestionAssetMutationCommand command) {
        QuestionAssetType assetType = command.type();
        return QuestionAsset.attachedTo(
            questionId,
            command.title(),
            command.durationSeconds(),
            command.altText(),
            assetType,
            assetType == QuestionAssetType.TEXT_PASSAGE ? null : command.url(),
            sanitizeTranscript(assetType, command.transcript()),
            command.description(),
            command.order());
    }

    private void validateAssetOrder(UUID questionId, int order, UUID currentAssetId) {
        boolean duplicated = questionAssetRepository.findByQuestionId(questionId).stream()
            .anyMatch(asset -> asset.getOrder() == order && !asset.getId().equals(currentAssetId));
        if (duplicated) {
            throw new IllegalArgumentException("Question asset order must not be duplicated");
        }
    }

    private CreateQuestionAssetMutationCommand normalize(CreateQuestionAssetMutationCommand input) {
        return new CreateQuestionAssetMutationCommand(
            input.questionId(),
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

    private String sanitizeTranscript(QuestionAssetType type, String transcript) {
        return QuestionAssetAnalysisRequestPublisher.supportsTranscript(type) ? transcript : null;
    }
}
