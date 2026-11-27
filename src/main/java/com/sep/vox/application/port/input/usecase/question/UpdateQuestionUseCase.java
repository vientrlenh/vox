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
            .orElseThrow(() -> new ResourceNotFoundException("Question not found"));
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
            .orElseThrow(() -> new ResourceNotFoundException("Question bank not found"));
        EditAccess access = questionEditAccessService.resolveEditAccess(question, bank, currentUserId);
        if (!access.allowed()) {
            throw new ForbiddenException("Access denied");
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
                "The question is published or already used in an assessment and cannot be edited directly — use the Clone button to create an editable copy");
        }
        if (!access.systemAdminOnSystemBank()
                && (access.owner() || access.editorCollaborator())
                && question.getStatus() != QuestionStatus.DRAFT
                && question.getStatus() != QuestionStatus.REVISION_REQUESTED) {
            throw new ForbiddenException("A question can only be edited in DRAFT or REVISION_REQUESTED status");
        }
        if (command.preparationTimeSeconds() == null || command.minResponseSeconds() == null || command.maxResponseSeconds() == null) {
            throw new IllegalArgumentException("The preparation time and the minimum and maximum answer times must all be provided");
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
            throw new IllegalArgumentException("The minimum answer time must not be greater than the maximum answer time");
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
