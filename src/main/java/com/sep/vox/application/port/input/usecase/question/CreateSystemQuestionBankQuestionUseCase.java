package com.sep.vox.application.port.input.usecase.question;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.mapper.question.CreateQuestionResponseMapper;
import com.sep.vox.application.port.input.command.CreateQuestionAssetCommand;
import com.sep.vox.application.port.input.command.CreateSystemQuestionBankQuestionCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.question.CreateQuestionResponse;
import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.model.question.QuestionAsset;
import com.sep.vox.domain.model.question.QuestionAssetType;
import com.sep.vox.domain.model.question.QuestionEvaluationGuide;
import com.sep.vox.domain.model.question.QuestionScope;
import com.sep.vox.domain.model.question.QuestionTopic;
import com.sep.vox.domain.model.question.QuestionType;
import com.sep.vox.domain.model.question.QuestionVisibility;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.QuestionAssetRepository;
import com.sep.vox.domain.repository.QuestionEvaluationGuideRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.QuestionTopicRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
public class CreateSystemQuestionBankQuestionUseCase implements IUseCase<CreateSystemQuestionBankQuestionCommand, CreateQuestionResponse> {

    private final UserRepository userRepository;
    private final QuestionRepository questionRepository;
    private final QuestionAssetRepository questionAssetRepository;
    private final QuestionEvaluationGuideRepository questionEvaluationGuideRepository;
    private final QuestionTopicRepository questionTopicRepository;
    private final UserContextPort userContextPort;

    public CreateSystemQuestionBankQuestionUseCase(
            UserRepository userRepository,
            QuestionRepository questionRepository,
            QuestionAssetRepository questionAssetRepository,
            QuestionEvaluationGuideRepository questionEvaluationGuideRepository,
            QuestionTopicRepository questionTopicRepository,
            UserContextPort userContextPort) {
        this.userRepository = userRepository;
        this.questionRepository = questionRepository;
        this.questionAssetRepository = questionAssetRepository;
        this.questionEvaluationGuideRepository = questionEvaluationGuideRepository;
        this.questionTopicRepository = questionTopicRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public CreateQuestionResponse execute(CreateSystemQuestionBankQuestionCommand input) {
        var command = normalize(input);

        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentUser = userRepository.findByIdAndStatus(currentUserId, UserStatus.ACTIVE)
            .orElseThrow(() -> new UnauthorizedException("Trạng thái người dùng không hợp lệ"));

        var questionTopic = getQuestionTopic(command.questionTopicId());
        if (currentUser.getSchoolId() == null
                || !questionTopicRepository.isTopicBelongToSchool(questionTopic.getId(), currentUser.getSchoolId())) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        validateResponseDurationRange(command);
        validateAssetOrders(command.assets());

        var now = OffsetDateTime.now();
        var question = Question.create(
            command.questionTopicId(), 
            command.code(),
            command.instructionText(),
            command.questionText(),
            command.promptText(),
            command.preparationText(),
            QuestionType.valueOf(command.type()),
            command.preparationTimeSeconds(),
            command.minResponseSeconds(),
            command.maxResponseSeconds(),
            QuestionScope.QUESTION_BANK,
            QuestionVisibility.BANK_VISIBLE,
            null,
            false,
            now,
            currentUserId
        );

        var saved = questionRepository.save(question);
        createEvaluationGuide(saved.getId(), command);
        createAssets(saved.getId(), command.assets());

        return CreateQuestionResponseMapper.toResponse(saved.getId());
    }

    private CreateSystemQuestionBankQuestionCommand normalize(CreateSystemQuestionBankQuestionCommand input) {
        return new CreateSystemQuestionBankQuestionCommand(
            input.questionTopicId(),
            StringNormalization.normalizeCode(input.code()),
            StringNormalization.trimAndCollapseSpaces(input.instructionText()),
            StringNormalization.trimAndCollapseSpaces(input.questionText()),
            StringNormalization.trimAndCollapseSpaces(input.promptText()),
            StringNormalization.trimAndCollapseSpaces(input.preparationText()),
            StringNormalization.trimAndCollapseSpaces(input.expectedContent()),
            StringNormalization.trimAndCollapseSpaces(input.keyPoints()),
            StringNormalization.trimAndCollapseSpaces(input.acceptableResponses()),
            StringNormalization.trimAndCollapseSpaces(input.offTopicExamples()),
            StringNormalization.trimAndCollapseSpaces(input.scoringHints()),
            StringNormalization.trimAndCollapseSpaces(input.commonMistakes()),
            StringNormalization.trimAndCollapseSpaces(input.type()),
            input.preparationTimeSeconds(),
            input.minResponseSeconds(),
            input.maxResponseSeconds(),
            assetsOf(input).stream()
                .map(this::normalizeAsset)
                .toList()
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
            StringNormalization.trimAndCollapseSpaces(input.type()),
            StringNormalization.trimAndCollapseSpaces(input.url()),
            StringNormalization.trimAndCollapseSpaces(input.transcript()),
            StringNormalization.trimAndCollapseSpaces(input.description()),
            input.order()
        );
    }

    private QuestionTopic getQuestionTopic(UUID questionTopicId) {
        var questionTopic = questionTopicRepository.findById(questionTopicId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy chủ đề câu hỏi với ID này"));

        if (!questionTopic.isActive()) {
            throw new IllegalStateException("Chủ đề câu hỏi yêu cầu hiện không hoạt động");
        }
        return questionTopic;
    }


    private void validateResponseDurationRange(CreateSystemQuestionBankQuestionCommand command) {
        if (command.minResponseSeconds() > command.maxResponseSeconds()) {
            throw new IllegalStateException("Thời gian trả lời tối thiểu không được lớn hơn thời gian trả lời tối đa");
        }
    }

    private void validateAssetOrders(List<CreateQuestionAssetCommand> assets) {
        var orders = new HashSet<Integer>();
        for (var asset : assets) {
            if (!orders.add(asset.order())) {
                throw new IllegalStateException("Thứ tự tài nguyên câu hỏi không được trùng lặp");
            }
        }
    }


    private void createEvaluationGuide(UUID questionId, CreateSystemQuestionBankQuestionCommand command) {
        if (!hasEvaluationGuide(command)) {
            return;
        }

        var evaluationGuide = new QuestionEvaluationGuide(
            questionId,
            command.expectedContent(),
            command.keyPoints(),
            command.acceptableResponses(),
            command.offTopicExamples(),
            command.scoringHints(),
            command.commonMistakes()
        );
        questionEvaluationGuideRepository.save(evaluationGuide);
    }

    private boolean hasEvaluationGuide(CreateSystemQuestionBankQuestionCommand command) {
        return command.expectedContent() != null
            || command.keyPoints() != null
            || command.acceptableResponses() != null
            || command.offTopicExamples() != null
            || command.scoringHints() != null
            || command.commonMistakes() != null;
    }

    private void createAssets(UUID questionId, List<CreateQuestionAssetCommand> assetCommands) {
        if (assetCommands.isEmpty()) {
            return;
        }

        var assets = assetCommands.stream()
            .map(asset -> new QuestionAsset(
                questionId,
                asset.title(),
                asset.durationSeconds(),
                asset.altText(),
                QuestionAssetType.valueOf(asset.type()),
                asset.url(),
                asset.transcript(),
                asset.description(),
                asset.order()
            ))
            .toList();
        questionAssetRepository.saveAll(assets);
    }
}
