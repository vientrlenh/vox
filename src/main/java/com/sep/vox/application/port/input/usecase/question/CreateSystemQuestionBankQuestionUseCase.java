package com.sep.vox.application.port.input.usecase.question;

import com.sep.vox.domain.model.question.QuestionBank;
import com.sep.vox.domain.model.question.QuestionTopic;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.mapper.question.CreateQuestionResponseMapper;
import com.sep.vox.application.port.input.command.CreateQuestionAssetCommand;
import com.sep.vox.application.port.input.command.CreateQuestionEvaluationGuideCommand;
import com.sep.vox.application.port.input.command.CreateSystemQuestionBankQuestionCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.application.response.input.question.CreateQuestionResponse;
import com.sep.vox.domain.mapper.QuestionAssetDtoMapper;
import com.sep.vox.domain.mapper.QuestionEvaluationGuideDtoMapper;
import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.model.question.QuestionAsset;
import com.sep.vox.domain.model.question.QuestionBankOwnerType;
import com.sep.vox.domain.model.question.QuestionConfidentiality;
import com.sep.vox.domain.model.question.QuestionEvaluationGuide;
import com.sep.vox.domain.model.question.QuestionSharing;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.domain.model.question.QuestionTopicStatus;
import com.sep.vox.domain.repository.QuestionAssetRepository;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.QuestionEvaluationGuideRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.QuestionTopicRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
public class CreateSystemQuestionBankQuestionUseCase implements IUseCase<CreateSystemQuestionBankQuestionCommand, CreateQuestionResponse> {

    private final SchoolUserRepository schoolUserRepository;
    private final QuestionRepository questionRepository;
    private final QuestionAssetRepository questionAssetRepository;
    private final QuestionEvaluationGuideRepository questionEvaluationGuideRepository;
    private final QuestionTopicRepository questionTopicRepository;
    private final QuestionBankRepository questionBankRepository;
    private final UserContextPort userContextPort;
    private final UserRoleQueryRepository userRoleQueryRepository;

    public CreateSystemQuestionBankQuestionUseCase(
            SchoolUserRepository schoolUserRepository,
            QuestionRepository questionRepository,
            QuestionAssetRepository questionAssetRepository,
            QuestionEvaluationGuideRepository questionEvaluationGuideRepository,
            QuestionTopicRepository questionTopicRepository,
            QuestionBankRepository questionBankRepository,
            UserContextPort userContextPort,
            UserRoleQueryRepository userRoleQueryRepository) {
        this.schoolUserRepository = schoolUserRepository;
        this.questionRepository = questionRepository;
        this.questionAssetRepository = questionAssetRepository;
        this.questionEvaluationGuideRepository = questionEvaluationGuideRepository;
        this.questionTopicRepository = questionTopicRepository;
        this.questionBankRepository = questionBankRepository;
        this.userContextPort = userContextPort;
        this.userRoleQueryRepository = userRoleQueryRepository;
    }

    @Override
    @Transactional
    public CreateQuestionResponse execute(CreateSystemQuestionBankQuestionCommand input) {
        CreateSystemQuestionBankQuestionCommand command = normalize(input);

        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        UUID currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        boolean teacher = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "TEACHER".equals(role.roleCode()));

        QuestionBank questionBank = questionBankRepository.findById(command.questionBankId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy ngân hàng câu hỏi"));
        QuestionTopic questionTopic = questionTopicRepository.findById(command.questionTopicId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy chủ đề câu hỏi"));

        if (!questionTopic.getQuestionBankId().equals(questionBank.getId())) {
            throw new IllegalStateException("Chủ đề không thuộc ngân hàng câu hỏi đã chọn");
        }
        if (questionTopic.getStatus() != QuestionTopicStatus.PUBLISHED) {
            throw new IllegalStateException("Chỉ được tạo câu hỏi trong chủ đề đã PUBLISHED");
        }

        if (questionBank.getOwnerType() == QuestionBankOwnerType.SYSTEM) {
            if (!userContextPort.isSystemAdmin()) {
                throw new ForbiddenException("Quyền truy cập bị từ chối");
            }
        } else if (!teacher || currentSchoolId == null || !currentSchoolId.equals(questionBank.getSchoolId())) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        validateResponseDurationRange(command);
        validateAssetOrders(command.assets());

        QuestionSharing sharing = command.sharing() == null ? QuestionSharing.PRIVATE : command.sharing();
        Question question = Question.builder()
                .questionBankId(command.questionBankId())
                .questionTopicId(command.questionTopicId())
                .code(questionCodeOf(command))
                .instructionText(command.instructionText())
                .questionText(command.questionText())
                .promptText(command.promptText())
                .preparationText(command.preparationText())
                .type(command.type())
                .preparationTimeSeconds(command.preparationTimeSeconds())
                .minResponseSeconds(command.minResponseSeconds())
                .maxResponseSeconds(command.maxResponseSeconds())
                .sharing(sharing)
                .sourceQuestionId(null)
                .locked(false)
                .confidentiality(QuestionConfidentiality.OPEN)
                .securePoolId(null)
                .status(QuestionStatus.DRAFT)
                .build();

        Question savedQuestion = questionRepository.save(question);
        List<QuestionAsset> savedAssets = createAssets(savedQuestion.getId(), command.assets());
        QuestionEvaluationGuide savedEvaluationGuide = createEvaluationGuide(savedQuestion.getId(), command.evaluationGuide());

        return CreateQuestionResponseMapper.toResponse(
            savedQuestion,
            QuestionAssetDtoMapper.toDtoList(savedAssets),
            savedEvaluationGuide == null ? null : QuestionEvaluationGuideDtoMapper.toDto(savedEvaluationGuide)
        );
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
            throw new IllegalStateException("Thời gian trả lời tối thiểu không được lớn hơn thời gian trả lời tối đa");
        }
    }

    private void validateAssetOrders(List<CreateQuestionAssetCommand> assets) {
        Set<Integer> orders = new HashSet<Integer>();
        for (CreateQuestionAssetCommand asset : assets) {
            if (!orders.add(asset.order())) {
                throw new IllegalStateException("Thứ tự tài nguyên câu hỏi không được trùng lặp");
            }
        }
    }

    private QuestionEvaluationGuide createEvaluationGuide(UUID questionId, CreateQuestionEvaluationGuideCommand command) {
        if (!hasEvaluationGuide(command)) {
            return null;
        }

        QuestionEvaluationGuide evaluationGuide = QuestionEvaluationGuide.builder()
                .questionId(questionId)
                .expectedContent(command.expectedContent())
                .keyPoints(command.keyPoints())
                .acceptableResponses(command.acceptableResponses())
                .offTopicExamples(command.offTopicExamples())
                .scoringHints(command.scoringHints())
                .commonMistakes(command.commonMistakes())
                .build();
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
            .map(asset -> QuestionAsset.builder()
                    .questionId(questionId)
                    .title(asset.title())
                    .durationSeconds(asset.durationSeconds())
                    .altText(asset.altText())
                    .type(asset.type())
                    .url(asset.url())
                    .transcript(asset.transcript())
                    .description(asset.description())
                    .order(asset.order())
                    .build())
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
