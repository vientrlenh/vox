package com.sep.vox.application.port.input.usecase.question;

import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.model.question.QuestionBank;
import java.time.Instant;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.question.CreateQuestionCollaboratorCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.dto.QuestionCollaboratorDto;
import com.sep.vox.domain.mapper.QuestionCollaboratorDtoMapper;
import com.sep.vox.domain.model.question.QuestionBankOwnerType;
import com.sep.vox.domain.model.question.QuestionCollaborator;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.QuestionCollaboratorRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
@RequiredArgsConstructor
public class CreateQuestionCollaboratorUseCase implements IUseCase<CreateQuestionCollaboratorCommand, QuestionCollaboratorDto> {

    private final QuestionRepository questionRepository;
    private final QuestionBankRepository questionBankRepository;
    private final QuestionCollaboratorRepository questionCollaboratorRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public QuestionCollaboratorDto execute(CreateQuestionCollaboratorCommand command) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();

        Question question = questionRepository.findById(command.questionId())
            .orElseThrow(() -> new ResourceNotFoundException("Question not found"));
        QuestionBank bank = questionBankRepository.findById(question.getQuestionBankId())
            .orElseThrow(() -> new ResourceNotFoundException("Question bank not found"));

        validateCanAddCollaborator(question, bank, command, currentUserId);

        QuestionCollaborator collaborator = new QuestionCollaborator();
        collaborator.setQuestionId(question.getId());
        collaborator.setUserId(command.userId());
        collaborator.setPermission(command.permission());
        collaborator.setAssignedAt(Instant.now());

        QuestionCollaborator saved = questionCollaboratorRepository.save(collaborator);
        return QuestionCollaboratorDtoMapper.toDto(saved);
    }

    private void validateCanAddCollaborator(
            Question question, QuestionBank bank, CreateQuestionCollaboratorCommand command, UUID currentUserId) {
        validateOwner(question, currentUserId);
        validateSchoolQuestion(bank.getOwnerType());

        if (question.getStatus() == QuestionStatus.ARCHIVED) {
            throw new IllegalArgumentException("Cannot add a collaborator to an ARCHIVED question");
        }

        if (currentUserId.equals(command.userId())) {
            throw new IllegalArgumentException("Cannot add yourself as a collaborator");
        }

        validateCollaboratorEligibility(bank, command);
    }

    private void validateCollaboratorEligibility(QuestionBank bank, CreateQuestionCollaboratorCommand command) {
        UUID schoolId = bank.getSchoolId();
        if (schoolId == null || !schoolUserRepository.existsBySchoolIdAndUserId(schoolId, command.userId())) {
            throw new ForbiddenException("The collaborator must belong to the same school");
        }

        boolean isTeacher = userRoleQueryRepository.findByUserIdWithRoleInfo(command.userId()).stream()
            .anyMatch(role -> "TEACHER".equals(role.roleCode()));
        if (!isTeacher) {
            throw new IllegalArgumentException("A question can only be shared with a teacher");
        }

        if (questionCollaboratorRepository.findByQuestionIdAndUserId(command.questionId(), command.userId()).isPresent()) {
            throw new IllegalArgumentException("This user is already a collaborator of the question");
        }
    }

    private void validateOwner(Question question, UUID currentUserId) {
        if (!question.isCreatedBy(currentUserId)) {
            throw new ForbiddenException("Only the owner can manage collaborators");
        }
    }

    private void validateSchoolQuestion(QuestionBankOwnerType ownerType) {
        if (ownerType != QuestionBankOwnerType.SCHOOL) {
            throw new IllegalArgumentException("Only a question in a school bank can have collaborators");
        }
    }
}
