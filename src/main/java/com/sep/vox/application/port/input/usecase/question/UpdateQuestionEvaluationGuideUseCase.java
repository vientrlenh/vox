package com.sep.vox.application.port.input.usecase.question;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.permission.QuestionCommandPermissionChecker;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.mapper.question.UpdateQuestionResponseMapper;
import com.sep.vox.application.port.input.command.UpdateQuestionEvaluationGuideCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.response.input.question.UpdateQuestionResponse;
import com.sep.vox.domain.model.question.QuestionEvaluationGuide;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.QuestionEvaluationGuideRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.QuestionTopicRepository;

@Service
public class UpdateQuestionEvaluationGuideUseCase implements IUseCase<UpdateQuestionEvaluationGuideCommand, UpdateQuestionResponse> {

    private final QuestionRepository questionRepository;
    private final QuestionTopicRepository questionTopicRepository;
    private final QuestionBankRepository questionBankRepository;
    private final QuestionEvaluationGuideRepository questionEvaluationGuideRepository;
    private final QuestionCommandPermissionChecker permissionChecker;

    public UpdateQuestionEvaluationGuideUseCase(
            QuestionRepository questionRepository,
            QuestionTopicRepository questionTopicRepository,
            QuestionBankRepository questionBankRepository,
            QuestionEvaluationGuideRepository questionEvaluationGuideRepository,
            QuestionCommandPermissionChecker permissionChecker) {
        this.questionRepository = questionRepository;
        this.questionTopicRepository = questionTopicRepository;
        this.questionBankRepository = questionBankRepository;
        this.questionEvaluationGuideRepository = questionEvaluationGuideRepository;
        this.permissionChecker = permissionChecker;
    }

    @Override
    @Transactional
    public UpdateQuestionResponse execute(UpdateQuestionEvaluationGuideCommand input) {
        var user = permissionChecker.resolveCurrentUser();

        var question = questionRepository.findById(input.questionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy câu hỏi"));
        var topic = questionTopicRepository.findById(question.getQuestionTopicId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy chủ đề"));
        var bank = questionBankRepository.findById(topic.getQuestionBankId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy ngân hàng câu hỏi"));

        permissionChecker.checkCanEditContent(question, topic, bank, user);

        // Delete existing guide
        questionEvaluationGuideRepository.deleteByQuestionId(input.questionId());

        // Create new guide
        var guide = new QuestionEvaluationGuide(
            UUID.randomUUID(),
            input.questionId(),
            input.expectedContent(),
            input.keyPoints(),
            input.acceptableResponses(),
            input.offTopicExamples(),
            input.scoringHints(),
            input.commonMistakes()
        );
        questionEvaluationGuideRepository.save(guide);

        question.setUpdatedAt(OffsetDateTime.now());
        question.setUpdatedBy(user.userId());
        questionRepository.save(question);

        return UpdateQuestionResponseMapper.toResponse(question.getId());
    }
}
