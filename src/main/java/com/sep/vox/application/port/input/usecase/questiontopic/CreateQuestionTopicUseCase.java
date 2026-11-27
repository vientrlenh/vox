package com.sep.vox.application.port.input.usecase.questiontopic;

import com.sep.vox.domain.model.question.QuestionBank;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.mapper.questiontopic.CreateQuestionTopicResponseMapper;
import com.sep.vox.application.port.input.command.questiontopic.CreateQuestionTopicCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.application.response.input.questiontopic.CreateQuestionTopicResponse;
import com.sep.vox.domain.model.question.QuestionBankOwnerType;
import com.sep.vox.domain.model.question.QuestionTopic;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.QuestionTopicRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
@RequiredArgsConstructor
public class CreateQuestionTopicUseCase implements IUseCase<CreateQuestionTopicCommand, CreateQuestionTopicResponse> {

    private final QuestionTopicRepository questionTopicRepository;
    private final QuestionBankRepository questionBankRepository;
    private final UserContextPort userContextPort;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;

    @Override
    @Transactional
    public CreateQuestionTopicResponse execute(CreateQuestionTopicCommand input) {
        CreateQuestionTopicCommand command = normalize(input);
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        UUID currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        boolean schoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));

        QuestionBank questionBank = questionBankRepository.findById(command.questionBankId())
            .orElseThrow(() -> new ResourceNotFoundException("Question bank not found"));
        requireCreateAccess(questionBank, schoolAdmin, currentSchoolId);

        QuestionTopic topic = QuestionTopic.draft(
                command.questionBankId(), command.code(), command.name(), command.description());
        QuestionTopic saved = questionTopicRepository.save(topic);
        return CreateQuestionTopicResponseMapper.toResponse(saved.getId());
    }

    private void requireCreateAccess(QuestionBank questionBank, boolean schoolAdmin, UUID currentSchoolId) {
        if (questionBank.getOwnerType() == QuestionBankOwnerType.SYSTEM) {
            if (!userContextPort.isSystemAdmin()) {
                throw new ForbiddenException("Access denied");
            }
        } else if (!schoolAdmin || currentSchoolId == null || !currentSchoolId.equals(questionBank.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }
    }

    private CreateQuestionTopicCommand normalize(CreateQuestionTopicCommand input) {
        return new CreateQuestionTopicCommand(
            input.questionBankId(),
            StringNormalization.normalizeCode(input.code()),
            StringNormalization.trimAndCollapseSpaces(input.name()),
            StringNormalization.trimAndCollapseSpaces(input.description())
        );
    }
}
