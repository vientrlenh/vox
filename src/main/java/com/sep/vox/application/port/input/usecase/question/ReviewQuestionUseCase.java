package com.sep.vox.application.port.input.usecase.question;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.mapper.question.UpdateQuestionResponseMapper;
import com.sep.vox.application.port.input.command.ReviewQuestionCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.QuestionPermissionQuery;
import com.sep.vox.application.response.input.question.UpdateQuestionResponse;
import com.sep.vox.domain.repository.QuestionRepository;

@Service
public class ReviewQuestionUseCase implements IUseCase<ReviewQuestionCommand, UpdateQuestionResponse> {

    private final QuestionRepository questionRepository;
    private final QuestionPermissionQuery permissionQuery;
    private final UserContextPort userContextPort;

    public ReviewQuestionUseCase(
            QuestionRepository questionRepository,
            QuestionPermissionQuery permissionQuery,
            UserContextPort userContextPort) {
        this.questionRepository = questionRepository;
        this.permissionQuery = permissionQuery;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public UpdateQuestionResponse execute(ReviewQuestionCommand input) {
        if (!permissionQuery.canReview(input.questionId(), input.targetStatus())) {
            throw new ForbiddenException("Không có quyền thực hiện hành động duyệt");
        }

        var question = questionRepository.findById(input.questionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy câu hỏi"));

        question.setStatus(input.targetStatus());
        question.setUpdatedAt(OffsetDateTime.now());
        question.setUpdatedBy(userContextPort.getCurrentAuthenticatedUserId());
        var saved = questionRepository.save(question);

        return UpdateQuestionResponseMapper.toResponse(saved.getId());
    }
}
