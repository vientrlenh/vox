package com.sep.vox.application.port.input.usecase.question;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.CloneQuestionCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.QuestionDto;
import com.sep.vox.domain.mapper.QuestionDtoMapper;
import com.sep.vox.domain.model.question.QuestionBankOwnerType;
import com.sep.vox.domain.model.question.QuestionCollaboratorPermission;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.QuestionCollaboratorRepository;
import com.sep.vox.domain.repository.QuestionRepository;

@Service
public class CloneQuestionUseCase implements IUseCase<CloneQuestionCommand, QuestionDto> {

    private final QuestionRepository questionRepository;
    private final QuestionBankRepository questionBankRepository;
    private final QuestionCollaboratorRepository questionCollaboratorRepository;
    private final QuestionCloneService questionCloneService;
    private final UserContextPort userContextPort;

    public CloneQuestionUseCase(
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
    public QuestionDto execute(CloneQuestionCommand input) {
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();

        var question = questionRepository.findById(input.questionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy câu hỏi"));
        var bank = questionBankRepository.findById(question.getQuestionBankId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy ngân hàng câu hỏi"));

        var owner = currentUserId.equals(question.getCreatedBy());
        var usableCollaborator = questionCollaboratorRepository.findByQuestionIdAndUserId(question.getId(), currentUserId)
            .filter(collaborator -> collaborator.getPermission() == QuestionCollaboratorPermission.CAN_EDIT
                || collaborator.getPermission() == QuestionCollaboratorPermission.CAN_USE)
            .isPresent();
        var systemAdminOnSystemBank = userContextPort.isSystemAdmin()
            && bank.getOwnerType() == QuestionBankOwnerType.SYSTEM;

        if (!systemAdminOnSystemBank && !usableCollaborator && !owner) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        var cloned = questionCloneService.cloneAsDraftWithDetails(question, currentUserId);
        return QuestionDtoMapper.toQuestionDto(cloned);
    }
}
