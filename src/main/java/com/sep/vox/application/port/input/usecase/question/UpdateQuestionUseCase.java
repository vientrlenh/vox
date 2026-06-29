package com.sep.vox.application.port.input.usecase.question;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.mapper.question.UpdateQuestionResponseMapper;
import com.sep.vox.application.port.input.command.UpdateQuestionCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.question.UpdateQuestionResponse;
import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.model.question.QuestionBankOwnerType;
import com.sep.vox.domain.model.question.QuestionCollaboratorPermission;
import com.sep.vox.domain.model.question.QuestionSharing;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.domain.model.question.QuestionType;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.QuestionCollaboratorRepository;
import com.sep.vox.domain.repository.QuestionRepository;

@Service
public class UpdateQuestionUseCase implements IUseCase<UpdateQuestionCommand, UpdateQuestionResponse> {

    private final QuestionRepository questionRepository;
    private final QuestionBankRepository questionBankRepository;
    private final QuestionCollaboratorRepository questionCollaboratorRepository;
    private final QuestionCloneService questionCloneService;
    private final UserContextPort userContextPort;

    public UpdateQuestionUseCase(
            QuestionRepository questionRepository,
            QuestionBankRepository questionBankRepository,
            QuestionCollaboratorRepository questionCollaboratorRepository,
            QuestionCloneService questionCloneService,
            UserContextPort userContextPort) {
        this.questionRepository = questionRepository;
        this.questionBankRepository = questionBankRepository;
        this.questionCollaboratorRepository = questionCollaboratorRepository;
        this.questionCloneService = questionCloneService;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public UpdateQuestionResponse execute(UpdateQuestionCommand input) {
        var command = normalize(input);
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();

        var question = questionRepository.findById(command.id())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy câu hỏi"));
        var bank = questionBankRepository.findById(question.getQuestionBankId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy ngân hàng câu hỏi"));

        var owner = currentUserId.equals(question.getCreatedBy());
        var editorCollaborator = questionCollaboratorRepository.findByQuestionIdAndUserId(question.getId(), currentUserId)
            .filter(collaborator -> collaborator.getPermission() == QuestionCollaboratorPermission.CAN_EDIT)
            .isPresent();
        var systemAdminOnSystemBank = userContextPort.isSystemAdmin()
            && bank.getOwnerType() == QuestionBankOwnerType.SYSTEM;

        if (!systemAdminOnSystemBank && !editorCollaborator && !owner) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        var usedInExam = questionRepository.existsUsedInExam(question.getId());
        var immutable = question.getStatus() == QuestionStatus.PUBLISHED || question.isLocked() || usedInExam;
        if (!systemAdminOnSystemBank
                && owner
                && !immutable
                && question.getStatus() != QuestionStatus.DRAFT
                && question.getStatus() != QuestionStatus.REVISION_REQUESTED) {
            throw new ForbiddenException("Chỉ được sửa câu hỏi của mình khi ở trạng thái DRAFT hoặc REVISION_REQUESTED");
        }

        var target = question;
        var clonedAsNew = false;
        if (immutable) {
            target = questionCloneService.cloneAsDraftWithDetails(question, currentUserId);
            clonedAsNew = true;
        }

        applyUpdates(target, command, currentUserId);
        var saved = questionRepository.save(target);
        return UpdateQuestionResponseMapper.toResponse(saved, clonedAsNew);
    }

    private void applyUpdates(Question question, UpdateQuestionCommand command, UUID currentUserId) {
        if (command.instructionText() != null) {
            question.setInstructionText(command.instructionText());
        }
        if (command.questionText() != null) {
            question.setQuestionText(command.questionText());
        }
        if (command.promptText() != null) {
            question.setPromptText(command.promptText());
        }
        if (command.preparationText() != null) {
            question.setPreparationText(command.preparationText());
        }
        if (command.questionType() != null) {
            question.setType(QuestionType.valueOf(command.questionType()));
        }
        if (command.preparationTimeSeconds() != null) {
            question.setPreparationTimeSeconds(command.preparationTimeSeconds());
        }
        if (command.minResponseSeconds() != null) {
            question.setMinResponseSeconds(command.minResponseSeconds());
        }
        if (command.maxResponseSeconds() != null) {
            question.setMaxResponseSeconds(command.maxResponseSeconds());
        }
        if (command.sharing() != null) {
            question.setSharing(QuestionSharing.valueOf(command.sharing()));
        }
        if (question.getMinResponseSeconds() > question.getMaxResponseSeconds()) {
            throw new IllegalStateException("Thời gian trả lời tối thiểu không được lớn hơn thời gian trả lời tối đa");
        }
        question.setUpdatedAt(OffsetDateTime.now());
        question.setUpdatedBy(currentUserId);
    }

    private UpdateQuestionCommand normalize(UpdateQuestionCommand input) {
        return new UpdateQuestionCommand(
            input.id(),
            StringNormalization.trimAndCollapseSpaces(input.instructionText()),
            StringNormalization.trimAndCollapseSpaces(input.questionText()),
            StringNormalization.trimAndCollapseSpaces(input.promptText()),
            StringNormalization.trimAndCollapseSpaces(input.preparationText()),
            input.questionType() == null ? null : StringNormalization.normalizeCode(input.questionType()),
            input.preparationTimeSeconds(),
            input.minResponseSeconds(),
            input.maxResponseSeconds(),
            input.sharing() == null ? null : StringNormalization.normalizeCode(input.sharing())
        );
    }
}
