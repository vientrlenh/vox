package com.sep.vox.application.port.input.usecase.question;

import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.model.question.QuestionBank;
import com.sep.vox.domain.model.question.QuestionCollaborator;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.question.UpdateQuestionCollaboratorCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.QuestionCollaboratorDto;
import com.sep.vox.domain.mapper.QuestionCollaboratorDtoMapper;
import com.sep.vox.domain.model.question.QuestionBankOwnerType;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.QuestionCollaboratorRepository;
import com.sep.vox.domain.repository.QuestionRepository;

@Service
@RequiredArgsConstructor
public class UpdateQuestionCollaboratorUseCase implements IUseCase<UpdateQuestionCollaboratorCommand, QuestionCollaboratorDto> {

    private final QuestionRepository questionRepository;
    private final QuestionBankRepository questionBankRepository;
    private final QuestionCollaboratorRepository questionCollaboratorRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public QuestionCollaboratorDto execute(UpdateQuestionCollaboratorCommand command) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();

        Question question = questionRepository.findById(command.questionId())
            .orElseThrow(() -> new ResourceNotFoundException("Question not found"));
        QuestionBank bank = questionBankRepository.findById(question.getQuestionBankId())
            .orElseThrow(() -> new ResourceNotFoundException("Question bank not found"));
        if (!question.isCreatedBy(currentUserId)) {
            throw new ForbiddenException("Only the owner can manage collaborators");
        }
        if (bank.getOwnerType() != QuestionBankOwnerType.SCHOOL) {
            throw new IllegalArgumentException("Only a question in a school bank can have collaborators");
        }

        QuestionCollaborator collaborator = questionCollaboratorRepository.findById(command.collaboratorId())
            .orElseThrow(() -> new ResourceNotFoundException("Collaborator not found"));
        if (!collaborator.getQuestionId().equals(question.getId())) {
            throw new ForbiddenException("The collaborator does not belong to this question");
        }

        collaborator.setPermission(command.permission());
        QuestionCollaborator saved = questionCollaboratorRepository.save(collaborator);
        return QuestionCollaboratorDtoMapper.toDto(saved);
    }
}
