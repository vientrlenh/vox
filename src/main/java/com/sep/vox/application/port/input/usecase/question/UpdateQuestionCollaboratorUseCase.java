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
import com.sep.vox.application.port.input.command.UpdateQuestionCollaboratorCommand;
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
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu hỏi"));
        QuestionBank bank = questionBankRepository.findById(question.getQuestionBankId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ngân hàng câu hỏi"));
        if (!question.isCreatedBy(currentUserId)) {
            throw new ForbiddenException("Chỉ owner mới được quản lý collaborator");
        }
        if (bank.getOwnerType() != QuestionBankOwnerType.SCHOOL) {
            throw new IllegalArgumentException("Chỉ question thuộc school bank mới dùng collaborator");
        }

        QuestionCollaborator collaborator = questionCollaboratorRepository.findById(command.collaboratorId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy collaborator"));
        if (!collaborator.getQuestionId().equals(question.getId())) {
            throw new ForbiddenException("Collaborator không thuộc câu hỏi này");
        }

        collaborator.setPermission(command.permission());
        QuestionCollaborator saved = questionCollaboratorRepository.save(collaborator);
        return QuestionCollaboratorDtoMapper.toDto(saved);
    }
}
