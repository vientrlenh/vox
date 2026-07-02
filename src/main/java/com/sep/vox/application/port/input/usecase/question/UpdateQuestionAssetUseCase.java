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
    private final UserContextPort userContextPort;

    public UpdateQuestionAssetUseCase(
            QuestionRepository questionRepository,
            QuestionBankRepository questionBankRepository,
            QuestionCollaboratorRepository questionCollaboratorRepository,
            QuestionAssetRepository questionAssetRepository,
            QuestionCloneService questionCloneService,
            UserContextPort userContextPort) {
        this.questionRepository = questionRepository;
        this.questionBankRepository = questionBankRepository;
        this.questionCollaboratorRepository = questionCollaboratorRepository;
        this.questionAssetRepository = questionAssetRepository;
        this.questionCloneService = questionCloneService;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public QuestionAssetDto execute(UpdateQuestionAssetCommand input) {
        var command = normalize(input);
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();

        var question = questionRepository.findById(command.questionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy câu hỏi"));
        var sourceAsset = questionAssetRepository.findById(command.assetId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy tài nguyên câu hỏi"));
        if (!sourceAsset.getQuestionId().equals(question.getId())) {
            throw new ForbiddenException("Tài nguyên không thuộc câu hỏi này");
        }

        var bank = questionBankRepository.findById(question.getQuestionBankId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy ngân hàng câu hỏi"));

        var owner = currentUserId.equals(question.getCreatedBy());
        var editorCollaborator = questionCollaboratorRepository.findByQuestionIdAndUserId(question.getId(), currentUserId)
            .filter(collaborator -> collaborator.getPermission() == QuestionCollaboratorPermission.CAN_EDIT)
            .isPresent();
        var systemAdminOnSystemBank = userContextPort.isSystemAdmin()
            && bank.getOwnerType() == QuestionBankOwnerType.SYSTEM;
        if (!systemAdminOnSystemBank && !owner && !editorCollaborator) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        var immutable = question.getStatus() == QuestionStatus.PUBLISHED
            || question.isLocked()
            || questionRepository.existsUsedInExam(question.getId());
        if (!systemAdminOnSystemBank
                && owner
                && !immutable
                && question.getStatus() != QuestionStatus.DRAFT
                && question.getStatus() != QuestionStatus.REVISION_REQUESTED) {
            throw new ForbiddenException("Chỉ được sửa câu hỏi của mình khi ở trạng thái DRAFT hoặc REVISION_REQUESTED");
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

        var saved = questionAssetRepository.save(targetAsset);
        return QuestionAssetDtoMapper.toDto(saved);
    }

    private QuestionAsset resolveClonedAsset(UUID questionId, int order) {
        return questionAssetRepository.findByQuestionId(questionId).stream()
            .filter(asset -> asset.getOrder() == order)
            .findFirst()
            .orElseThrow(() -> new NotFoundException("Không tìm thấy tài nguyên câu hỏi trong bản nháp mới"));
    }

    private void validateAssetOrder(UUID questionId, int order, UUID currentAssetId) {
        var duplicated = questionAssetRepository.findByQuestionId(questionId).stream()
            .anyMatch(asset -> asset.getOrder() == order && !asset.getId().equals(currentAssetId));
        if (duplicated) {
            throw new IllegalStateException("Thứ tự tài nguyên câu hỏi không được trùng lặp");
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
}
