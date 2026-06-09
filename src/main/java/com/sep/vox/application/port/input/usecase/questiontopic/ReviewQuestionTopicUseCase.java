package com.sep.vox.application.port.input.usecase.questiontopic;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.ReviewQuestionTopicCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.QuestionTopicPermissionQuery;
import com.sep.vox.application.response.input.questiontopic.UpdateQuestionTopicResponse;
import com.sep.vox.domain.model.question.QuestionTopicStatus;
import com.sep.vox.domain.repository.QuestionTopicRepository;

@Service
public class ReviewQuestionTopicUseCase implements IUseCase<ReviewQuestionTopicCommand, UpdateQuestionTopicResponse> {

    private final QuestionTopicRepository questionTopicRepository;
    private final QuestionTopicPermissionQuery permissionQuery;
    private final UserContextPort userContextPort;

    public ReviewQuestionTopicUseCase(
            QuestionTopicRepository questionTopicRepository,
            QuestionTopicPermissionQuery permissionQuery,
            UserContextPort userContextPort) {
        this.questionTopicRepository = questionTopicRepository;
        this.permissionQuery = permissionQuery;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public UpdateQuestionTopicResponse execute(ReviewQuestionTopicCommand input) {
        boolean permitted = switch (input.targetStatus()) {
            case PUBLISHED -> permissionQuery.canPublishTopic(input.topicId());
            case ARCHIVED -> permissionQuery.canArchiveTopic(input.topicId());
            case DRAFT -> permissionQuery.canRestoreTopic(input.topicId());
        };

        if (!permitted) {
            throw new ForbiddenException("Không có quyền thực hiện hành động này");
        }

        var topic = questionTopicRepository.findById(input.topicId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy chủ đề câu hỏi"));

        topic.setStatus(input.targetStatus());
        topic.setUpdatedAt(OffsetDateTime.now());
        topic.setUpdatedBy(userContextPort.getCurrentAuthenticatedUserId());
        var saved = questionTopicRepository.save(topic);

        return new UpdateQuestionTopicResponse(saved.getId());
    }
}
