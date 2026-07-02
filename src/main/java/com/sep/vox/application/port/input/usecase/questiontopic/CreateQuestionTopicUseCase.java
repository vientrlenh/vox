package com.sep.vox.application.port.input.usecase.questiontopic;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.mapper.questiontopic.CreateQuestionTopicResponseMapper;
import com.sep.vox.application.port.input.command.CreateQuestionTopicCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.application.response.input.questiontopic.CreateQuestionTopicResponse;
import com.sep.vox.domain.model.question.QuestionBankOwnerType;
import com.sep.vox.domain.model.question.QuestionTopic;
import com.sep.vox.domain.model.question.QuestionTopicStatus;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.QuestionTopicRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
public class CreateQuestionTopicUseCase implements IUseCase<CreateQuestionTopicCommand, CreateQuestionTopicResponse> {

    private final QuestionTopicRepository questionTopicRepository;
    private final QuestionBankRepository questionBankRepository;
    private final UserContextPort userContextPort;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;

    public CreateQuestionTopicUseCase(QuestionTopicRepository questionTopicRepository,
            QuestionBankRepository questionBankRepository, UserContextPort userContextPort,
            SchoolUserRepository schoolUserRepository, UserRoleQueryRepository userRoleQueryRepository) {
        this.questionTopicRepository = questionTopicRepository;
        this.questionBankRepository = questionBankRepository;
        this.userContextPort = userContextPort;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
    }

    @Override
    @Transactional
    public CreateQuestionTopicResponse execute(CreateQuestionTopicCommand input) {
        var command = normalize(input);
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        var schoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));

        var questionBank = questionBankRepository.findById(command.questionBankId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy ngân hàng câu hỏi"));

        if (questionBank.getOwnerType() == QuestionBankOwnerType.SYSTEM) {
            if (!userContextPort.isSystemAdmin()) {
                throw new ForbiddenException("Quyền truy cập bị từ chối");
            }
        } else if (!schoolAdmin || currentSchoolId == null || !currentSchoolId.equals(questionBank.getSchoolId())) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        var now = OffsetDateTime.now();
        var topic = new QuestionTopic(
            command.questionBankId(),
            command.code(),
            command.name(),
            command.description(),
            QuestionTopicStatus.DRAFT,
            now,
            now,
            currentUserId,
            currentUserId
        );
        var saved = questionTopicRepository.save(topic);
        return CreateQuestionTopicResponseMapper.toResponse(saved.getId());
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
