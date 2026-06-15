package com.sep.vox.application.port.input.usecase.question;

import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
public class UpdateQuestionEvaluationGuideUseCase implements IUseCase<UpdateQuestionEvaluationGuideCommand, UpdateQuestionResponse> {

    private final QuestionRepository questionRepository;
    private final QuestionEvaluationGuideRepository questionEvaluationGuideRepository;
    private final QuestionPermissionQuery permissionQuery;
    private final UserContextPort userContextPort;

    public UpdateQuestionEvaluationGuideUseCase(
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
            throw new ForbiddenException("Khong co quyen chinh sua huong dan danh gia");
        }

        var question = questionRepository.findById(input.questionId())
            .orElseThrow(() -> new NotFoundException("Khong tim thay cau hoi"));

        var guide = questionEvaluationGuideRepository.findByQuestionId(input.questionId())
            .orElseThrow(() -> new NotFoundException("Cau hoi chua co huong dan danh gia de cap nhat"));

        guide.setExpectedContent(input.expectedContent());
        guide.setKeyPoints(input.keyPoints());
        guide.setAcceptableResponses(input.acceptableResponses());
        guide.setOffTopicExamples(input.offTopicExamples());
        guide.setScoringHints(input.scoringHints());
        guide.setCommonMistakes(input.commonMistakes());
        questionEvaluationGuideRepository.save(guide);

        question.setUpdatedAt(OffsetDateTime.now());
        question.setUpdatedBy(userContextPort.getCurrentAuthenticatedUserId());
        questionRepository.save(question);

        return UpdateQuestionResponseMapper.toResponse(question.getId());
    }
}
