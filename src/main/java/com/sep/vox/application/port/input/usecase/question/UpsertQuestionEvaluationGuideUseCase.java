package com.sep.vox.application.port.input.usecase.question;

import com.sep.vox.domain.model.question.Question;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.question.UpsertQuestionEvaluationGuideCommand;
import com.sep.vox.application.port.input.service.QuestionEditAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.QuestionEvaluationGuideDto;
import com.sep.vox.domain.mapper.QuestionEvaluationGuideDtoMapper;
import com.sep.vox.domain.model.question.QuestionEvaluationGuide;
import com.sep.vox.domain.repository.QuestionEvaluationGuideRepository;
import com.sep.vox.domain.repository.QuestionRepository;

@Service
@RequiredArgsConstructor
public class UpsertQuestionEvaluationGuideUseCase implements IUseCase<UpsertQuestionEvaluationGuideCommand, QuestionEvaluationGuideDto> {

    private final QuestionRepository questionRepository;
    private final QuestionEvaluationGuideRepository questionEvaluationGuideRepository;
    private final QuestionEditAccessService questionEditAccessService;
    private final QuestionCloneService questionCloneService;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public QuestionEvaluationGuideDto execute(UpsertQuestionEvaluationGuideCommand input) {
        UpsertQuestionEvaluationGuideCommand command = normalize(input);
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();

        Question question = questionRepository.findById(command.questionId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu hỏi"));
        boolean immutable = questionEditAccessService.authorizeEditing(question, currentUserId);

        Question targetQuestion = immutable
            ? questionCloneService.cloneAsDraftWithDetails(question, currentUserId)
            : question;

        QuestionEvaluationGuide guide = questionEvaluationGuideRepository.findByQuestionId(targetQuestion.getId())
            .orElseGet(() -> QuestionEvaluationGuide.emptyFor(targetQuestion.getId()));

        applyProvidedFields(guide, command);

        QuestionEvaluationGuide saved = questionEvaluationGuideRepository.save(guide);
        return QuestionEvaluationGuideDtoMapper.toDto(saved);
    }

    private void applyProvidedFields(QuestionEvaluationGuide guide, UpsertQuestionEvaluationGuideCommand command) {
        if (command.expectedContent() != null) {
            guide.setExpectedContent(command.expectedContent());
        }
        if (command.keyPoints() != null) {
            guide.setKeyPoints(command.keyPoints());
        }
        if (command.acceptableResponses() != null) {
            guide.setAcceptableResponses(command.acceptableResponses());
        }
        if (command.offTopicExamples() != null) {
            guide.setOffTopicExamples(command.offTopicExamples());
        }
        if (command.scoringHints() != null) {
            guide.setScoringHints(command.scoringHints());
        }
        if (command.commonMistakes() != null) {
            guide.setCommonMistakes(command.commonMistakes());
        }
    }

    private UpsertQuestionEvaluationGuideCommand normalize(UpsertQuestionEvaluationGuideCommand input) {
        return new UpsertQuestionEvaluationGuideCommand(
            input.questionId(),
            StringNormalization.trimAndCollapseSpaces(input.expectedContent()),
            StringNormalization.trimAndCollapseSpaces(input.keyPoints()),
            StringNormalization.trimAndCollapseSpaces(input.acceptableResponses()),
            StringNormalization.trimAndCollapseSpaces(input.offTopicExamples()),
            StringNormalization.trimAndCollapseSpaces(input.scoringHints()),
            StringNormalization.trimAndCollapseSpaces(input.commonMistakes())
        );
    }
}
