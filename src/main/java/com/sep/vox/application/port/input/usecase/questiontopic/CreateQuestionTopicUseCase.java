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
import com.sep.vox.application.query.repository.QuestionTopicPermissionQuery;
import com.sep.vox.application.response.input.questiontopic.CreateQuestionTopicResponse;
import com.sep.vox.domain.model.question.QuestionTopic;
import com.sep.vox.domain.model.question.QuestionTopicStatus;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.QuestionTopicRepository;

@Service
public class CreateQuestionTopicUseCase implements IUseCase<CreateQuestionTopicCommand, CreateQuestionTopicResponse> {

    private final QuestionTopicRepository questionTopicRepository;
    private final QuestionBankRepository questionBankRepository;
    private final QuestionTopicPermissionQuery permissionQuery;
    private final UserContextPort userContextPort;

    public CreateQuestionTopicUseCase(
            QuestionTopicRepository questionTopicRepository,
            QuestionBankRepository questionBankRepository,
            QuestionTopicPermissionQuery permissionQuery,
            UserContextPort userContextPort) {
        this.questionTopicRepository = questionTopicRepository;
        this.questionBankRepository = questionBankRepository;
        this.permissionQuery = permissionQuery;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public CreateQuestionTopicResponse execute(CreateQuestionTopicCommand input) {
        var command = normalize(input);

        if (!questionBankRepository.existsById(command.bankId())) {
            throw new NotFoundException("Khong tim thay ngan hang cau hoi");
        }
        if (!permissionQuery.canCreateTopic(command.bankId())) {
            throw new ForbiddenException("Khong co quyen tao chu de cau hoi");
        }

        var now = OffsetDateTime.now();
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var code = StringNormalization.normalizeCode(command.topicName());

        var topic = new QuestionTopic(
            command.bankId(),
            code,
            command.topicName(),
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
            input.bankId(),
            StringNormalization.trimAndCollapseSpaces(input.topicName()),
            StringNormalization.trimAndCollapseSpaces(input.description())
        );
    }
}
