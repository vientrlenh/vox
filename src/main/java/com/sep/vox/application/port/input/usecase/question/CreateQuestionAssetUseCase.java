package com.sep.vox.application.port.input.usecase.question;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.CreateQuestionAssetMutationCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.QuestionAssetDto;
import com.sep.vox.domain.mapper.QuestionAssetDtoMapper;
import com.sep.vox.domain.model.question.QuestionAsset;
import com.sep.vox.domain.model.question.QuestionAssetType;
import com.sep.vox.domain.model.question.QuestionBankOwnerType;
import com.sep.vox.domain.model.question.QuestionCollaboratorPermission;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.domain.repository.QuestionAssetRepository;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.QuestionCollaboratorRepository;
import com.sep.vox.domain.repository.QuestionRepository;

@Service
public class CreateQuestionAssetUseCase implements IUseCase<CreateQuestionAssetMutationCommand, QuestionAssetDto> {

    private final QuestionRepository questionRepository;
    private final QuestionBankRepository questionBankRepository;
    private final QuestionCollaboratorRepository questionCollaboratorRepository;
    private final QuestionAssetRepository questionAssetRepository;
    private final QuestionCloneService questionCloneService;
    private final QuestionAssetAnalysisRequestPublisher questionAssetAnalysisRequestPublisher;
    private final UserContextPort userContextPort;

    public CreateQuestionAssetUseCase(
            QuestionRepository questionRepository,
            QuestionBankRepository questionBankRepository,
            QuestionCollaboratorRepository questionCollaboratorRepository,
            QuestionAssetRepository questionAssetRepository,
            QuestionCloneService questionCloneService,
            QuestionAssetAnalysisRequestPublisher questionAssetAnalysisRequestPublisher,
            UserContextPort userContextPort) {
        this.questionRepository = questionRepository;
        this.questionBankRepository = questionBankRepository;
        this.questionCollaboratorRepository = questionCollaboratorRepository;
        this.questionAssetRepository = questionAssetRepository;
        this.questionCloneService = questionCloneService;
        this.questionAssetAnalysisRequestPublisher = questionAssetAnalysisRequestPublisher;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public QuestionAssetDto execute(CreateQuestionAssetMutationCommand input) {
        var command = normalize(input);
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();

        var question = questionRepository.findById(command.questionId())
            .orElseThrow(() -> new NotFoundException("KhÃ´ng tÃ¬m tháº¥y cÃ¢u há»i"));
        var bank = questionBankRepository.findById(question.getQuestionBankId())
            .orElseThrow(() -> new NotFoundException("KhÃ´ng tÃ¬m tháº¥y ngÃ¢n hÃ ng cÃ¢u há»i"));

        var owner = currentUserId.equals(question.getCreatedBy());
        var editorCollaborator = questionCollaboratorRepository.findByQuestionIdAndUserId(question.getId(), currentUserId)
            .filter(collaborator -> collaborator.getPermission() == QuestionCollaboratorPermission.CAN_EDIT)
            .isPresent();
        var systemAdminOnSystemBank = userContextPort.isSystemAdmin()
            && bank.getOwnerType() == QuestionBankOwnerType.SYSTEM;
        if (!systemAdminOnSystemBank && !owner && !editorCollaborator) {
            throw new ForbiddenException("Quyá»n truy cáº­p bá»‹ tá»« chá»‘i");
        }

        var immutable = question.getStatus() == QuestionStatus.PUBLISHED
            || question.isLocked()
            || questionRepository.existsUsedInExam(question.getId());
        if (!systemAdminOnSystemBank
                && owner
                && !immutable
                && question.getStatus() != QuestionStatus.DRAFT
                && question.getStatus() != QuestionStatus.REVISION_REQUESTED) {
            throw new ForbiddenException("Chá»‰ Ä‘Æ°á»£c sá»­a cÃ¢u há»i cá»§a mÃ¬nh khi á»Ÿ tráº¡ng thÃ¡i DRAFT hoáº·c REVISION_REQUESTED");
        }

        var targetQuestion = immutable
            ? questionCloneService.cloneAsDraftWithDetails(question, currentUserId)
            : question;

        if (questionAssetRepository.existsByQuestionId(targetQuestion.getId())) {
            throw new IllegalStateException("CÃ¢u há»i Ä‘Ã£ cÃ³ asset, hÃ£y sá»­a hoáº·c xoÃ¡ asset hiá»‡n cÃ³ trÆ°á»›c");
        }

        validateAssetOrder(targetQuestion.getId(), command.order(), null);

        var assetType = QuestionAssetType.valueOf(command.type());
        validateRequiredFields(assetType, command.url(), command.transcript());

        var transcript = sanitizeTranscript(assetType, command.transcript());
        var description = command.description();
        var asset = new QuestionAsset(
            targetQuestion.getId(),
            command.title(),
            command.durationSeconds(),
            command.altText(),
            assetType,
            assetType == QuestionAssetType.TEXT_PASSAGE ? null : command.url(),
            transcript,
            description,
            command.order()
        );

        var saved = questionAssetRepository.save(asset);
        questionAssetAnalysisRequestPublisher.publishIfNeeded(targetQuestion, saved);
        return QuestionAssetDtoMapper.toDto(saved);
    }

    private void validateAssetOrder(java.util.UUID questionId, int order, java.util.UUID currentAssetId) {
        var duplicated = questionAssetRepository.findByQuestionId(questionId).stream()
            .anyMatch(asset -> asset.getOrder() == order && !asset.getId().equals(currentAssetId));
        if (duplicated) {
            throw new IllegalStateException("Thá»© tá»± tÃ i nguyÃªn cÃ¢u há»i khÃ´ng Ä‘Æ°á»£c trÃ¹ng láº·p");
        }
    }

    private CreateQuestionAssetMutationCommand normalize(CreateQuestionAssetMutationCommand input) {
        return new CreateQuestionAssetMutationCommand(
            input.questionId(),
            StringNormalization.trimAndCollapseSpaces(input.title()),
            input.durationSeconds(),
            StringNormalization.trimAndCollapseSpaces(input.altText()),
            StringNormalization.normalizeCode(input.type()),
            StringNormalization.trimAndCollapseSpaces(input.url()),
            StringNormalization.trimAndCollapseSpaces(input.transcript()),
            StringNormalization.trimAndCollapseSpaces(input.description()),
            input.order()
        );
    }

    private void validateRequiredFields(QuestionAssetType type, String url, String transcript) {
        if (type == QuestionAssetType.TEXT_PASSAGE) {
            if (transcript == null || transcript.isBlank()) {
                throw new IllegalArgumentException("Transcript khÃ´ng Ä‘Æ°á»£c Ä‘á»ƒ trá»‘ng vá»›i asset TEXT_PASSAGE");
            }
            return;
        }

        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL tÃ i nguyÃªn khÃ´ng Ä‘Æ°á»£c Ä‘á»ƒ trá»‘ng");
        }
    }

    private String sanitizeTranscript(QuestionAssetType type, String transcript) {
        return QuestionAssetAnalysisRequestPublisher.supportsTranscript(type) ? transcript : null;
    }
}
