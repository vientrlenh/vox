package com.sep.vox.application.port.input.usecase.questiontopic;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateQuestionTopicCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.QuestionTopicPermissionQuery;
import com.sep.vox.domain.dto.QuestionTopicDto;
import com.sep.vox.domain.mapper.QuestionTopicDtoMapper;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.QuestionTopicRepository;

@Service
public class UpdateQuestionTopicUseCase implements IUseCase<UpdateQuestionTopicCommand, QuestionTopicDto> {

    private final QuestionTopicRepository questionTopicRepository;
    private final QuestionBankRepository questionBankRepository;
    private final QuestionTopicPermissionQuery permissionQuery;
    private final UserContextPort userContextPort;

    public UpdateQuestionTopicUseCase(
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
    public QuestionTopicDto execute(UpdateQuestionTopicCommand input) {
        var command = normalize(input);

        if (!questionBankRepository.existsById(command.bankId())) {
            throw new NotFoundException("Khong tim thay ngan hang cau hoi");
        }

        var topic = questionTopicRepository.findById(command.id())
            .orElseThrow(() -> new NotFoundException("Khong tim thay chu de cau hoi"));

        if (!topic.getQuestionBankId().equals(command.bankId())) {
            throw new ForbiddenException("Chu de cau hoi khong thuoc ngan hang duoc chi dinh");
        }
        if (!permissionQuery.canUpdateTopic(command.id())) {
            throw new ForbiddenException("Khong co quyen cap nhat chu de cau hoi");
        }

        topic.setName(command.topicName());
        topic.setDescription(command.description());
        topic.setUpdatedAt(OffsetDateTime.now());
        topic.setUpdatedBy(userContextPort.getCurrentAuthenticatedUserId());

        var saved = questionTopicRepository.save(topic);
        return QuestionTopicDtoMapper.toDto(saved);
    }

    private UpdateQuestionTopicCommand normalize(UpdateQuestionTopicCommand input) {
        return new UpdateQuestionTopicCommand(
            input.id(),
            input.bankId(),
            StringNormalization.trimAndCollapseSpaces(input.topicName()),
            StringNormalization.trimAndCollapseSpaces(input.description())
        );
    }
}
