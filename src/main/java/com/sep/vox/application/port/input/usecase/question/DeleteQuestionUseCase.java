package com.sep.vox.application.port.input.usecase.question;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.DeleteQuestionCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.QuestionPermissionQuery;
import com.sep.vox.application.response.input.question.DeleteQuestionResponse;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.domain.repository.QuestionAssetRepository;
import com.sep.vox.domain.repository.QuestionEvaluationGuideRepository;
import com.sep.vox.domain.repository.QuestionRepository;

@Service
public class DeleteQuestionUseCase implements IUseCase<DeleteQuestionCommand, DeleteQuestionResponse> {

    private static final String HARD_DELETE = "HARD_DELETE";
    private static final String ARCHIVE = "ARCHIVE";

    private final QuestionRepository questionRepository;
    private final QuestionAssetRepository questionAssetRepository;
    private final QuestionEvaluationGuideRepository questionEvaluationGuideRepository;
    private final QuestionPermissionQuery permissionQuery;
    private final UserContextPort userContextPort;

    public DeleteQuestionUseCase(
            QuestionRepository questionRepository,
            QuestionAssetRepository questionAssetRepository,
            QuestionEvaluationGuideRepository questionEvaluationGuideRepository,
            QuestionPermissionQuery permissionQuery,
            UserContextPort userContextPort) {
        this.questionRepository = questionRepository;
        this.questionAssetRepository = questionAssetRepository;
        this.questionEvaluationGuideRepository = questionEvaluationGuideRepository;
        this.permissionQuery = permissionQuery;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public DeleteQuestionResponse execute(DeleteQuestionCommand input) {
        if (!permissionQuery.canEditContent(input.questionId())) {
            throw new ForbiddenException("Khong co quyen xoa cau hoi");
        }

        var question = questionRepository.findById(input.questionId())
            .orElseThrow(() -> new NotFoundException("Khong tim thay cau hoi"));

        var isUsed = questionRepository.existsBySourceQuestionId(input.questionId());
        if (question.getStatus() == QuestionStatus.DRAFT && !isUsed) {
            questionAssetRepository.deleteByQuestionId(input.questionId());
            questionEvaluationGuideRepository.deleteByQuestionId(input.questionId());
            questionRepository.deleteById(input.questionId());
            return new DeleteQuestionResponse(input.questionId(), HARD_DELETE, null);
        }

        question.setStatus(QuestionStatus.ARCHIVED);
        question.setUpdatedAt(OffsetDateTime.now());
        question.setUpdatedBy(userContextPort.getCurrentAuthenticatedUserId());
        var saved = questionRepository.save(question);
        return new DeleteQuestionResponse(saved.getId(), ARCHIVE, saved.getStatus().name());
    }
}
