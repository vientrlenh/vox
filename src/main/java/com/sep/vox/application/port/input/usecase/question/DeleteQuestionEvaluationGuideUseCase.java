package com.sep.vox.application.port.input.usecase.question;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.mapper.question.UpdateQuestionResponseMapper;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.QuestionPermissionQuery;
import com.sep.vox.application.response.input.question.UpdateQuestionResponse;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.domain.repository.QuestionEvaluationGuideRepository;
import com.sep.vox.domain.repository.QuestionRepository;

@Service
public class DeleteQuestionEvaluationGuideUseCase implements IUseCase<UUID, UpdateQuestionResponse> {

    private final QuestionRepository questionRepository;
    private final QuestionEvaluationGuideRepository questionEvaluationGuideRepository;
    private final QuestionPermissionQuery permissionQuery;
    private final UserContextPort userContextPort;

    public DeleteQuestionEvaluationGuideUseCase(
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
    public UpdateQuestionResponse execute(UUID questionId) {
        if (!permissionQuery.canEditContent(questionId)) {
            throw new ForbiddenException("Khong co quyen xoa huong dan danh gia");
        }

        var question = questionRepository.findById(questionId)
            .orElseThrow(() -> new NotFoundException("Khong tim thay cau hoi"));

        if (question.getStatus() != QuestionStatus.DRAFT) {
            throw new ForbiddenException("Chi duoc xoa huong dan danh gia khi cau hoi dang o DRAFT");
        }

        if (questionEvaluationGuideRepository.findByQuestionId(questionId).isEmpty()) {
            throw new NotFoundException("Cau hoi chua co huong dan danh gia de xoa");
        }

        questionEvaluationGuideRepository.deleteByQuestionId(questionId);
        question.setUpdatedAt(OffsetDateTime.now());
        question.setUpdatedBy(userContextPort.getCurrentAuthenticatedUserId());
        var saved = questionRepository.save(question);
        return UpdateQuestionResponseMapper.toResponse(saved.getId());
    }
}
