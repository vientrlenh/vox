package com.sep.vox.application.port.input.usecase.question;

import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.mapper.question.UpdateQuestionResponseMapper;
import com.sep.vox.application.port.input.command.UpdateQuestionEvaluationGuideCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.QuestionPermissionQuery;
import com.sep.vox.application.response.input.question.UpdateQuestionResponse;
import com.sep.vox.domain.model.question.QuestionEvaluationGuide;
import com.sep.vox.domain.repository.QuestionEvaluationGuideRepository;
import com.sep.vox.domain.repository.QuestionRepository;

@Service
public class CreateQuestionEvaluationGuideUseCase implements IUseCase<UpdateQuestionEvaluationGuideCommand, UpdateQuestionResponse> {

    private final QuestionRepository questionRepository;
    private final QuestionEvaluationGuideRepository questionEvaluationGuideRepository;
    private final QuestionPermissionQuery permissionQuery;
    private final UserContextPort userContextPort;

    public CreateQuestionEvaluationGuideUseCase(
            QuestionRepository questionRepository,
            QuestionEvaluationGuideRepository questionEvaluationGuideRepository,
            QuestionPermissionQuery permissionQuery,
            UserContextPort userContextPort) {
        this.questionRepository = questionRepository;
        this.questionEvaluationGuideRepository = questionEvaluationGuideRepository;
        this.permissionQuery = permissionQuery;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public UpdateQuestionResponse execute(UpdateQuestionEvaluationGuideCommand input) {
        if (!permissionQuery.canEditContent(input.questionId())) {
            throw new ForbiddenException("Khong co quyen tao huong dan danh gia");
        }

        var question = questionRepository.findById(input.questionId())
            .orElseThrow(() -> new NotFoundException("Khong tim thay cau hoi"));

        if (questionEvaluationGuideRepository.findByQuestionId(input.questionId()).isPresent()) {
            throw new DuplicatedException("Cau hoi da co huong dan danh gia, hay dung endpoint update");
        }

        var guide = new QuestionEvaluationGuide(
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
        question.setUpdatedBy(userContextPort.getCurrentAuthenticatedUserId());
        questionRepository.save(question);

        return UpdateQuestionResponseMapper.toResponse(question.getId());
    }
}
