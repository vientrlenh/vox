package com.sep.vox.application.port.input.usecase.question;

import com.sep.vox.domain.model.question.QuestionBank;
import com.sep.vox.domain.model.question.QuestionTopic;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.mapper.question.CreateQuestionResponseMapper;
import com.sep.vox.application.port.input.command.question.CreateQuestionAssetCommand;
import com.sep.vox.application.port.input.command.question.CreateQuestionEvaluationGuideCommand;
import com.sep.vox.application.port.input.command.question.CreateSystemQuestionBankQuestionCommand;
import com.sep.vox.application.port.input.service.QuestionEditAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.question.CreateQuestionResponse;
import com.sep.vox.domain.mapper.QuestionAssetDtoMapper;
import com.sep.vox.domain.mapper.QuestionEvaluationGuideDtoMapper;
import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.model.question.QuestionAsset;
import com.sep.vox.domain.model.question.QuestionEvaluationGuide;
import com.sep.vox.domain.model.question.QuestionSharing;
import com.sep.vox.domain.model.question.QuestionTopicStatus;
import com.sep.vox.domain.repository.QuestionAssetRepository;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.QuestionEvaluationGuideRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.QuestionTopicRepository;

@Service
@RequiredArgsConstructor
public class CreateSystemQuestionBankQuestionUseCase implements IUseCase<CreateSystemQuestionBankQuestionCommand, CreateQuestionResponse> {

    private final QuestionRepository questionRepository;
    private final QuestionAssetRepository questionAssetRepository;
    private final QuestionEvaluationGuideRepository questionEvaluationGuideRepository;
    private final QuestionTopicRepository questionTopicRepository;
    private final QuestionBankRepository questionBankRepository;
    private final UserContextPort userContextPort;
    private final QuestionEditAccessService questionEditAccessService;

    @Override
    @Transactional
    public CreateQuestionResponse execute(CreateSystemQuestionBankQuestionCommand input) {
        CreateSystemQuestionBankQuestionCommand command = normalize(input);

        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        QuestionBank questionBank = loadBankOfPublishedTopic(command);
        questionEditAccessService.requireCreationAccess(questionBank, currentUserId);

        validateResponseDurationRange(command);
        validateAssetOrders(command.assets());

        Question savedQuestion = questionRepository.save(buildDraftQuestion(command));
        List<QuestionAsset> savedAssets = createAssets(savedQuestion.getId(), command.assets());
        QuestionEvaluationGuide savedEvaluationGuide = createEvaluationGuide(savedQuestion.getId(), command.evaluationGuide());

        return CreateQuestionResponseMapper.toResponse(
            savedQuestion,
            QuestionAssetDtoMapper.toDtoList(savedAssets),
            savedEvaluationGuide == null ? null : QuestionEvaluationGuideDtoMapper.toDto(savedEvaluationGuide)
        );
    }

    private QuestionBank loadBankOfPublishedTopic(CreateSystemQuestionBankQuestionCommand command) {
        QuestionBank questionBank = questionBankRepository.findById(command.questionBankId())
            .orElseThrow(() -> new ResourceNotFoundException("Question bank not found"));
        QuestionTopic questionTopic = questionTopicRepository.findById(command.questionTopicId())
            .orElseThrow(() -> new ResourceNotFoundException("Question topic not found"));

        if (!questionTopic.getQuestionBankId().equals(questionBank.getId())) {
            throw new IllegalArgumentException("The topic does not belong to the selected question bank");
        }
        if (questionTopic.getStatus() != QuestionTopicStatus.PUBLISHED) {
            throw new IllegalArgumentException("Questions can only be created in a PUBLISHED topic");
        }
        return questionBank;
    }

    private Question buildDraftQuestion(CreateSystemQuestionBankQuestionCommand command) {
        QuestionSharing sharing = command.sharing() == null ? QuestionSharing.PRIVATE : command.sharing();
        return Question.draft(
                command.questionBankId(),
                command.questionTopicId(),
                questionCodeOf(command),
                command.instructionText(),
                command.questionText(),
                command.promptText(),
                command.preparationText(),
                command.type(),
                command.preparationTimeSeconds(),
                command.minResponseSeconds(),
                command.maxResponseSeconds(),
                sharing);
    }

    private CreateSystemQuestionBankQuestionCommand normalize(CreateSystemQuestionBankQuestionCommand input) {
        return new CreateSystemQuestionBankQuestionCommand(
            input.questionBankId(),
            input.questionTopicId(),
            StringNormalization.normalizeCode(input.code()),
            StringNormalization.trimAndCollapseSpaces(input.instructionText()),
            StringNormalization.trimAndCollapseSpaces(input.questionText()),
            StringNormalization.trimAndCollapseSpaces(input.promptText()),
            StringNormalization.trimAndCollapseSpaces(input.preparationText()),
            input.type(),
            input.preparationTimeSeconds(),
            input.minResponseSeconds(),
            input.maxResponseSeconds(),
            input.sharing(),
            assetsOf(input).stream()
                .map(this::normalizeAsset)
                .toList(),
            normalizeEvaluationGuide(input.evaluationGuide())
        );
    }

    private List<CreateQuestionAssetCommand> assetsOf(CreateSystemQuestionBankQuestionCommand input) {
        return input.assets() == null ? List.of() : input.assets();
    }

    private CreateQuestionAssetCommand normalizeAsset(CreateQuestionAssetCommand input) {
        return new CreateQuestionAssetCommand(
            StringNormalization.trimAndCollapseSpaces(input.title()),
            input.durationSeconds(),
            StringNormalization.trimAndCollapseSpaces(input.altText()),
            input.type(),
            StringNormalization.trimAndCollapseSpaces(input.url()),
            StringNormalization.trimAndCollapseSpaces(input.transcript()),
            StringNormalization.trimAndCollapseSpaces(input.description()),
            input.order()
        );
    }

    private CreateQuestionEvaluationGuideCommand normalizeEvaluationGuide(CreateQuestionEvaluationGuideCommand input) {
        if (input == null) {
            return null;
        }

        return new CreateQuestionEvaluationGuideCommand(
            StringNormalization.trimAndCollapseSpaces(input.expectedContent()),
            StringNormalization.trimAndCollapseSpaces(input.keyPoints()),
            StringNormalization.trimAndCollapseSpaces(input.acceptableResponses()),
            StringNormalization.trimAndCollapseSpaces(input.offTopicExamples()),
            StringNormalization.trimAndCollapseSpaces(input.scoringHints()),
            StringNormalization.trimAndCollapseSpaces(input.commonMistakes())
        );
    }

    private void validateResponseDurationRange(CreateSystemQuestionBankQuestionCommand command) {
        if (command.minResponseSeconds() > command.maxResponseSeconds()) {
            throw new IllegalArgumentException("The minimum answer time must not be greater than the maximum answer time");
        }
    }

    private void validateAssetOrders(List<CreateQuestionAssetCommand> assets) {
        Set<Integer> orders = new HashSet<Integer>();
        for (CreateQuestionAssetCommand asset : assets) {
            if (!orders.add(asset.order())) {
                throw new IllegalArgumentException("Question asset order must not be duplicated");
            }
        }
    }

    private QuestionEvaluationGuide createEvaluationGuide(UUID questionId, CreateQuestionEvaluationGuideCommand command) {
        if (!hasEvaluationGuide(command)) {
            return null;
        }

        QuestionEvaluationGuide evaluationGuide = QuestionEvaluationGuide.forQuestion(
                questionId,
                command.expectedContent(),
                command.keyPoints(),
                command.acceptableResponses(),
                command.offTopicExamples(),
                command.scoringHints(),
                command.commonMistakes());
        return questionEvaluationGuideRepository.save(evaluationGuide);
    }

    private boolean hasEvaluationGuide(CreateQuestionEvaluationGuideCommand command) {
        return command != null
            && (command.expectedContent() != null
            || command.keyPoints() != null
            || command.acceptableResponses() != null
            || command.offTopicExamples() != null
            || command.scoringHints() != null
            || command.commonMistakes() != null);
    }

    private List<QuestionAsset> createAssets(UUID questionId, List<CreateQuestionAssetCommand> assetCommands) {
        if (assetCommands.isEmpty()) {
            return List.of();
        }

        List<QuestionAsset> assets = assetCommands.stream()
            .map(asset -> QuestionAsset.attachedTo(
                    questionId,
                    asset.title(),
                    asset.durationSeconds(),
                    asset.altText(),
                    asset.type(),
                    asset.url(),
                    asset.transcript(),
                    asset.description(),
                    asset.order()))
            .toList();
        return questionAssetRepository.saveAll(assets);
    }

    private String questionCodeOf(CreateSystemQuestionBankQuestionCommand command) {
        if (command.code() != null && !command.code().isBlank()) {
            return command.code();
        }
        return "Q-" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }
}
