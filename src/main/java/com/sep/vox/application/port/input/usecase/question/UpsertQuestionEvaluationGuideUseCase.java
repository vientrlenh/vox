package com.sep.vox.application.port.input.usecase.question;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpsertQuestionEvaluationGuideCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.QuestionEvaluationGuideDto;
import com.sep.vox.domain.mapper.QuestionEvaluationGuideDtoMapper;
import com.sep.vox.domain.model.question.QuestionBankOwnerType;
import com.sep.vox.domain.model.question.QuestionCollaboratorPermission;
import com.sep.vox.domain.model.question.QuestionEvaluationGuide;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.QuestionCollaboratorRepository;
import com.sep.vox.domain.repository.QuestionEvaluationGuideRepository;
import com.sep.vox.domain.repository.QuestionRepository;

@Service
public class UpsertQuestionEvaluationGuideUseCase implements IUseCase<UpsertQuestionEvaluationGuideCommand, QuestionEvaluationGuideDto> {

    private final QuestionRepository questionRepository;
    private final QuestionBankRepository questionBankRepository;
    private final QuestionCollaboratorRepository questionCollaboratorRepository;
    private final QuestionEvaluationGuideRepository questionEvaluationGuideRepository;
    private final QuestionCloneService questionCloneService;
    private final UserContextPort userContextPort;

    public UpsertQuestionEvaluationGuideUseCase(
            QuestionRepository questionRepository,
            QuestionBankRepository questionBankRepository,
            QuestionCollaboratorRepository questionCollaboratorRepository,
            QuestionEvaluationGuideRepository questionEvaluationGuideRepository,
            QuestionCloneService questionCloneService,
            UserContextPort userContextPort) {
        this.questionRepository = questionRepository;
        this.questionBankRepository = questionBankRepository;
        this.questionCollaboratorRepository = questionCollaboratorRepository;
        this.questionEvaluationGuideRepository = questionEvaluationGuideRepository;
        this.questionCloneService = questionCloneService;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public QuestionEvaluationGuideDto execute(UpsertQuestionEvaluationGuideCommand input) {
        var command = normalize(input);
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();

        var question = questionRepository.findById(command.questionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy câu hỏi"));
        var bank = questionBankRepository.findById(question.getQuestionBankId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy ngân hàng câu hỏi"));

        var owner = currentUserId.equals(question.getCreatedBy());
        var editorCollaborator = questionCollaboratorRepository.findByQuestionIdAndUserId(question.getId(), currentUserId)
            .filter(collaborator -> collaborator.getPermission() == QuestionCollaboratorPermission.CAN_EDIT)
            .isPresent();
        var systemAdminOnSystemBank = userContextPort.isSystemAdmin()
            && bank.getOwnerType() == QuestionBankOwnerType.SYSTEM;
        if (!systemAdminOnSystemBank && !owner && !editorCollaborator) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        var immutable = question.getStatus() == QuestionStatus.PUBLISHED
            || question.isLocked()
            || questionRepository.existsUsedInExam(question.getId());
        if (!systemAdminOnSystemBank
                && owner
                && !immutable
                && question.getStatus() != QuestionStatus.DRAFT
                && question.getStatus() != QuestionStatus.REVISION_REQUESTED) {
            throw new ForbiddenException("Chỉ được sửa câu hỏi của mình khi ở trạng thái DRAFT hoặc REVISION_REQUESTED");
        }

        var targetQuestion = immutable
            ? questionCloneService.cloneAsDraftWithDetails(question, currentUserId)
            : question;

        var guide = questionEvaluationGuideRepository.findByQuestionId(targetQuestion.getId())
            .orElseGet(() -> new QuestionEvaluationGuide(
                targetQuestion.getId(),
                null,
                null,
                null,
                null,
                null,
                null
            ));

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

        var saved = questionEvaluationGuideRepository.save(guide);
        return QuestionEvaluationGuideDtoMapper.toDto(saved);
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
