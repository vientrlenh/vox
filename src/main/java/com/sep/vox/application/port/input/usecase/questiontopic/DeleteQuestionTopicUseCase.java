package com.sep.vox.application.port.input.usecase.questiontopic;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.DeleteQuestionTopicCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.repository.QuestionTopicPermissionQuery;
import com.sep.vox.application.response.input.questiontopic.UpdateQuestionTopicResponse;
import com.sep.vox.domain.model.question.QuestionTopicStatus;
import com.sep.vox.domain.repository.QuestionAssetRepository;
import com.sep.vox.domain.repository.QuestionEvaluationGuideRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.QuestionTopicRepository;

@Service
public class DeleteQuestionTopicUseCase implements IUseCase<DeleteQuestionTopicCommand, UpdateQuestionTopicResponse> {

    private final QuestionTopicRepository questionTopicRepository;
    private final QuestionRepository questionRepository;
    private final QuestionAssetRepository questionAssetRepository;
    private final QuestionEvaluationGuideRepository questionEvaluationGuideRepository;
    private final QuestionTopicPermissionQuery permissionQuery;

    public DeleteQuestionTopicUseCase(
            QuestionTopicRepository questionTopicRepository,
            QuestionRepository questionRepository,
            QuestionAssetRepository questionAssetRepository,
            QuestionEvaluationGuideRepository questionEvaluationGuideRepository,
            QuestionTopicPermissionQuery permissionQuery) {
        this.questionTopicRepository = questionTopicRepository;
        this.questionRepository = questionRepository;
        this.questionAssetRepository = questionAssetRepository;
        this.questionEvaluationGuideRepository = questionEvaluationGuideRepository;
        this.permissionQuery = permissionQuery;
    }

    @Override
    @Transactional
    public UpdateQuestionTopicResponse execute(DeleteQuestionTopicCommand input) {
        if (!permissionQuery.canUpdateTopic(input.topicId())) {
            throw new ForbiddenException("Khong co quyen xoa question topic");
        }

        var topic = questionTopicRepository.findById(input.topicId())
            .orElseThrow(() -> new NotFoundException("Khong tim thay chu de cau hoi"));

        if (topic.getStatus() != QuestionTopicStatus.DRAFT) {
            throw new ForbiddenException("Chi duoc xoa question topic khi dang o DRAFT");
        }

        for (var question : questionRepository.findByTopicId(input.topicId())) {
            questionAssetRepository.deleteByQuestionId(question.getId());
            questionEvaluationGuideRepository.deleteByQuestionId(question.getId());
            questionRepository.deleteById(question.getId());
        }
        questionTopicRepository.deleteById(input.topicId());
        return new UpdateQuestionTopicResponse(input.topicId());
    }
}
