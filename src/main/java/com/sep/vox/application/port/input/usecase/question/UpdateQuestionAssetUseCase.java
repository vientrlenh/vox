package com.sep.vox.application.port.input.usecase.question;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateQuestionAssetCommand;
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
public class UpdateQuestionAssetUseCase implements IUseCase<UpdateQuestionAssetCommand, QuestionAssetDto> {

    private final QuestionRepository questionRepository;
    private final QuestionBankRepository questionBankRepository;
    private final QuestionCollaboratorRepository questionCollaboratorRepository;
    private final QuestionAssetRepository questionAssetRepository;
    private final QuestionCloneService questionCloneService;
    private final QuestionAssetAnalysisRequestPublisher questionAssetAnalysisRequestPublisher;
    private final UserContextPort userContextPort;

    public UpdateQuestionAssetUseCase(
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
    public QuestionAssetDto execute(UpdateQuestionAssetCommand input) {
        var command = normalize(input);
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();

        var question = questionRepository.findById(command.questionId())
            .orElseThrow(() -> new NotFoundException("KhÃ´ng tÃ¬m tháº¥y cÃ¢u há»i"));
        var sourceAsset = questionAssetRepository.findById(command.assetId())
            .orElseThrow(() -> new NotFoundException("KhÃ´ng tÃ¬m tháº¥y tÃ i nguyÃªn cÃ¢u há»i"));
        if (!sourceAsset.getQuestionId().equals(question.getId())) {
            throw new ForbiddenException("TÃ i nguyÃªn khÃ´ng thuá»™c cÃ¢u há»i nÃ y");
        }

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
        var targetAsset = immutable
            ? resolveClonedAsset(targetQuestion.getId(), sourceAsset.getOrder())
            : sourceAsset;

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
            targetAsset.setType(QuestionAssetType.valueOf(command.type()));
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
        if (command.order() != null) {
            validateAssetOrder(targetQuestion.getId(), command.order(), targetAsset.getId());
            targetAsset.setOrder(command.order());
        }

        sanitizeForType(targetAsset);
        validateRequiredFields(targetAsset.getType(), targetAsset.getUrl(), targetAsset.getTranscript());

        // File/type replace no longer auto-clears transcript/description -- per the simplified
        // rule, existing content (whether AI-written or manually typed) always sticks unless the
        // teacher explicitly blanks the field or uses "Tạo lại bằng AI" to force regeneration.
        var saved = questionAssetRepository.save(targetAsset);
        // Tạm thời bỏ auto-publish yêu cầu AI phân tích asset để luồng lưu asset
        // luôn độc lập và ổn định. Sẽ bật lại ở task riêng về AI update asset sau.
        // questionAssetAnalysisRequestPublisher.publishIfNeeded(targetQuestion, saved);
        return QuestionAssetDtoMapper.toDto(saved);
    }

    private QuestionAsset resolveClonedAsset(UUID questionId, int order) {
        return questionAssetRepository.findByQuestionId(questionId).stream()
            .filter(asset -> asset.getOrder() == order)
            .findFirst()
            .orElseThrow(() -> new NotFoundException("KhÃ´ng tÃ¬m tháº¥y tÃ i nguyÃªn cÃ¢u há»i trong báº£n nhÃ¡p má»›i"));
    }

    private void validateAssetOrder(UUID questionId, int order, UUID currentAssetId) {
        var duplicated = questionAssetRepository.findByQuestionId(questionId).stream()
            .anyMatch(asset -> asset.getOrder() == order && !asset.getId().equals(currentAssetId));
        if (duplicated) {
            throw new IllegalStateException("Thá»© tá»± tÃ i nguyÃªn cÃ¢u há»i khÃ´ng Ä‘Æ°á»£c trÃ¹ng láº·p");
        }
    }

    private UpdateQuestionAssetCommand normalize(UpdateQuestionAssetCommand input) {
        return new UpdateQuestionAssetCommand(
            input.questionId(),
            input.assetId(),
            StringNormalization.trimAndCollapseSpaces(input.title()),
            input.durationSeconds(),
            StringNormalization.trimAndCollapseSpaces(input.altText()),
            input.type() == null ? null : StringNormalization.normalizeCode(input.type()),
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

    private void sanitizeForType(QuestionAsset asset) {
        if (!QuestionAssetAnalysisRequestPublisher.supportsTranscript(asset.getType())) {
            asset.setTranscript(null);
        }
        if (asset.getType() == QuestionAssetType.TEXT_PASSAGE) {
            asset.setUrl(null);
        }
    }
}
