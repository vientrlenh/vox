package com.sep.vox.application.port.input.usecase.question;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.permission.QuestionCommandPermissionChecker;
import com.sep.vox.application.common.permission.ReviewAction;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.mapper.question.CreateQuestionResponseMapper;
import com.sep.vox.application.port.input.command.CloneQuestionCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.response.input.question.CreateQuestionResponse;
import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.model.question.QuestionAsset;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.domain.repository.QuestionAssetRepository;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.QuestionEvaluationGuideRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.QuestionTopicRepository;

@Service
public class CloneQuestionUseCase implements IUseCase<CloneQuestionCommand, CreateQuestionResponse> {

    private final QuestionRepository questionRepository;
    private final QuestionTopicRepository questionTopicRepository;
    private final QuestionBankRepository questionBankRepository;
    private final QuestionAssetRepository questionAssetRepository;
    private final QuestionEvaluationGuideRepository questionEvaluationGuideRepository;
    private final QuestionCommandPermissionChecker permissionChecker;

    public CloneQuestionUseCase(
            QuestionRepository questionRepository,
            QuestionTopicRepository questionTopicRepository,
            QuestionBankRepository questionBankRepository,
            QuestionAssetRepository questionAssetRepository,
            QuestionEvaluationGuideRepository questionEvaluationGuideRepository,
            QuestionCommandPermissionChecker permissionChecker) {
        this.questionRepository = questionRepository;
        this.questionTopicRepository = questionTopicRepository;
        this.questionBankRepository = questionBankRepository;
        this.questionAssetRepository = questionAssetRepository;
        this.questionEvaluationGuideRepository = questionEvaluationGuideRepository;
        this.permissionChecker = permissionChecker;
    }

    @Override
    @Transactional
    public CreateQuestionResponse execute(CloneQuestionCommand input) {
        var user = permissionChecker.resolveCurrentUser();

        var source = questionRepository.findById(input.questionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy câu hỏi"));
        var topic = questionTopicRepository.findById(source.getQuestionTopicId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy chủ đề"));
        var bank = questionBankRepository.findById(topic.getQuestionBankId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy ngân hàng câu hỏi"));

        permissionChecker.checkCanReviewAction(source, topic, bank, ReviewAction.CLONE_FOR_REVISION, user);

        OffsetDateTime now = OffsetDateTime.now();

        // Create cloned question
        var cloned = Question.create(
            source.getQuestionTopicId(),
            source.getCode() + "-COPY",
            source.getInstructionText(),
            source.getQuestionText(),
            source.getPromptText(),
            source.getPreparationText(),
            source.getType(),
            source.getPreparationTimeSeconds(),
            source.getMinResponseSeconds(),
            source.getMaxResponseSeconds(),
            source.getScope(),
            source.getVisibility(),
            source.getId(), // sourceQuestionId points to original
            false, // not locked
            now,
            user.userId()
        );
        var savedQuestion = questionRepository.save(cloned);

        // Clone assets
        var sourceAssets = questionAssetRepository.findByQuestionId(source.getId());
        if (!sourceAssets.isEmpty()) {
            var clonedAssets = sourceAssets.stream()
                .map(a -> new QuestionAsset(
                    UUID.randomUUID(),
                    savedQuestion.getId(),
                    a.getTitle(),
                    a.getDurationSeconds(),
                    a.getAltText(),
                    a.getType(),
                    a.getUrl(),
                    a.getTranscript(),
                    a.getDescription(),
                    a.getOrder()
                ))
                .toList();
            questionAssetRepository.saveAll(clonedAssets);
        }

        // Clone evaluation guide
        var sourceGuide = questionEvaluationGuideRepository.findByQuestionId(source.getId());
        if (sourceGuide.isPresent()) {
            var g = sourceGuide.get();
            var clonedGuide = new com.sep.vox.domain.model.question.QuestionEvaluationGuide(
                UUID.randomUUID(),
                savedQuestion.getId(),
                g.getExpectedContent(),
                g.getKeyPoints(),
                g.getAcceptableResponses(),
                g.getOffTopicExamples(),
                g.getScoringHints(),
                g.getCommonMistakes()
            );
            questionEvaluationGuideRepository.save(clonedGuide);
        }

        return CreateQuestionResponseMapper.toResponse(savedQuestion.getId());
    }
}
