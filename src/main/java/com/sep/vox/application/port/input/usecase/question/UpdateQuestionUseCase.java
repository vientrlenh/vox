package com.sep.vox.application.port.input.usecase.question;

import com.sep.vox.domain.model.question.QuestionBank;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.mapper.question.UpdateQuestionResponseMapper;
import com.sep.vox.application.port.input.command.question.UpdateQuestionCommand;
import com.sep.vox.application.port.input.service.QuestionEditAccessService;
import com.sep.vox.application.port.input.service.QuestionEditAccessService.EditAccess;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.question.UpdateQuestionResponse;
import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.QuestionRepository;

@Service
@RequiredArgsConstructor
public class UpdateQuestionUseCase implements IUseCase<UpdateQuestionCommand, UpdateQuestionResponse> {

    private final QuestionRepository questionRepository;
    private final QuestionBankRepository questionBankRepository;
    private final QuestionEditAccessService questionEditAccessService;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public UpdateQuestionResponse execute(UpdateQuestionCommand input) {
        UpdateQuestionCommand command = normalize(input);
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();

        Question question = questionRepository.findById(command.id())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu hỏi"));
        EditAccess access = requireEditAccess(question, currentUserId);

        // Sharing is a private setting, not content -- it can be changed at any time.
        if (command.sharing() != null) {
            question.setSharing(command.sharing());
        }

        if (hasContentUpdate(command)) {
            requireContentEditable(question, command, access);
            applyContentUpdates(question, command, currentUserId);
        }

        questionRepository.save(question);

        return UpdateQuestionResponseMapper.toResponse(question);
    }

    private EditAccess requireEditAccess(Question question, UUID currentUserId) {
        QuestionBank bank = questionBankRepository.findById(question.getQuestionBankId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ngân hàng câu hỏi"));
        EditAccess access = questionEditAccessService.resolveEditAccess(question, bank, currentUserId);
        if (!access.allowed()) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
        return access;
    }

    private boolean hasContentUpdate(UpdateQuestionCommand command) {
        return command.instructionText() != null || command.questionText() != null
                || command.promptText() != null || command.preparationText() != null
                || command.questionType() != null || command.preparationTimeSeconds() != null
                || command.minResponseSeconds() != null || command.maxResponseSeconds() != null;
    }

    private void requireContentEditable(Question question, UpdateQuestionCommand command, EditAccess access) {
        boolean usedInAssessment = questionRepository.existsUsedInAssessment(question.getId());
        boolean immutable = question.getStatus() == QuestionStatus.PUBLISHED || question.isLocked() || usedInAssessment;
        if (immutable) {
            throw new IllegalArgumentException(
                "Câu hỏi đã publish hoặc đã dùng trong bài kiểm tra, không thể sửa trực tiếp — dùng nút Nhân bản để tạo bản có thể sửa");
        }
        if (!access.systemAdminOnSystemBank()
                && (access.owner() || access.editorCollaborator())
                && question.getStatus() != QuestionStatus.DRAFT
                && question.getStatus() != QuestionStatus.REVISION_REQUESTED) {
            throw new ForbiddenException("Chỉ được sửa câu hỏi khi ở trạng thái DRAFT hoặc REVISION_REQUESTED");
        }
        if (command.preparationTimeSeconds() == null || command.minResponseSeconds() == null || command.maxResponseSeconds() == null) {
            throw new IllegalArgumentException("Phải nhập đầy đủ thời gian chuẩn bị, thời gian trả lời tối thiểu và tối đa");
        }
    }

    private void applyContentUpdates(Question question, UpdateQuestionCommand command, UUID currentUserId) {
        if (command.instructionText() != null) question.setInstructionText(command.instructionText());
        if (command.questionText() != null) question.setQuestionText(command.questionText());
        if (command.promptText() != null) question.setPromptText(command.promptText());
        if (command.preparationText() != null) question.setPreparationText(command.preparationText());
        if (command.questionType() != null) question.setType(command.questionType());
        if (command.preparationTimeSeconds() != null) question.setPreparationTimeSeconds(command.preparationTimeSeconds());
        if (command.minResponseSeconds() != null) question.setMinResponseSeconds(command.minResponseSeconds());
        if (command.maxResponseSeconds() != null) question.setMaxResponseSeconds(command.maxResponseSeconds());
        if (question.getMinResponseSeconds() > question.getMaxResponseSeconds()) {
            throw new IllegalArgumentException("Thời gian trả lời tối thiểu không được lớn hơn thời gian trả lời tối đa");
        }
    }

    private UpdateQuestionCommand normalize(UpdateQuestionCommand input) {
        return new UpdateQuestionCommand(
            input.id(),
            StringNormalization.trimAndCollapseSpaces(input.instructionText()),
            StringNormalization.trimAndCollapseSpaces(input.questionText()),
            StringNormalization.trimAndCollapseSpaces(input.promptText()),
            StringNormalization.trimAndCollapseSpaces(input.preparationText()),
            input.questionType(),
            input.preparationTimeSeconds(),
            input.minResponseSeconds(),
            input.maxResponseSeconds(),
            input.sharing()
        );
    }
}
