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
import com.sep.vox.application.port.input.command.DeleteQuestionCollaboratorCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.question.QuestionBankOwnerType;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.QuestionCollaboratorRepository;
import com.sep.vox.domain.repository.QuestionRepository;

@Service
@RequiredArgsConstructor
public class DeleteQuestionCollaboratorUseCase implements IUseCase<DeleteQuestionCollaboratorCommand, Void> {

    private final QuestionRepository questionRepository;
    private final QuestionBankRepository questionBankRepository;
    private final QuestionCollaboratorRepository questionCollaboratorRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public Void execute(DeleteQuestionCollaboratorCommand input) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();

        Question question = questionRepository.findById(input.questionId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu hỏi"));
        QuestionBank bank = questionBankRepository.findById(question.getQuestionBankId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ngân hàng câu hỏi"));
        if (!question.isCreatedBy(currentUserId)) {
            throw new ForbiddenException("Chỉ owner mới được quản lý collaborator");
        }
        if (bank.getOwnerType() != QuestionBankOwnerType.SCHOOL) {
            throw new IllegalArgumentException("Chỉ question thuộc school bank mới dùng collaborator");
        }

        QuestionCollaborator collaborator = questionCollaboratorRepository.findById(input.collaboratorId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy collaborator"));
        if (!collaborator.getQuestionId().equals(question.getId())) {
            throw new ForbiddenException("Collaborator không thuộc câu hỏi này");
        }

        questionCollaboratorRepository.deleteById(collaborator.getId());
        return null;
    }
}
