package com.sep.vox.application.port.input.usecase.question;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.permission.QuestionCommandPermissionChecker;
import com.sep.vox.application.common.permission.ReviewAction;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.mapper.question.UpdateQuestionResponseMapper;
import com.sep.vox.application.port.input.command.ReviewQuestionCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.response.input.question.UpdateQuestionResponse;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.QuestionTopicRepository;

@Service
public class ReviewQuestionUseCase implements IUseCase<ReviewQuestionCommand, UpdateQuestionResponse> {

    private final QuestionRepository questionRepository;
    private final QuestionTopicRepository questionTopicRepository;
    private final QuestionBankRepository questionBankRepository;
    private final QuestionCommandPermissionChecker permissionChecker;

    public ReviewQuestionUseCase(
            QuestionRepository questionRepository,
            QuestionTopicRepository questionTopicRepository,
            QuestionBankRepository questionBankRepository,
            QuestionCommandPermissionChecker permissionChecker) {
        this.questionRepository = questionRepository;
        this.questionTopicRepository = questionTopicRepository;
        this.questionBankRepository = questionBankRepository;
        this.permissionChecker = permissionChecker;
    }

    @Override
    @Transactional
    public UpdateQuestionResponse execute(ReviewQuestionCommand input) {
        var user = permissionChecker.resolveCurrentUser();

        var question = questionRepository.findById(input.questionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy câu hỏi"));
        var topic = questionTopicRepository.findById(question.getQuestionTopicId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy chủ đề"));
        var bank = questionBankRepository.findById(topic.getQuestionBankId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy ngân hàng câu hỏi"));

        permissionChecker.checkCanReviewAction(question, topic, bank, input.action(), user);

        OffsetDateTime now = OffsetDateTime.now();

        switch (input.action()) {
            case SUBMIT_FOR_REVIEW -> {
                question.setStatus(QuestionStatus.SUBMITTED_FOR_REVIEW);
            }
            case REQUEST_REVISION -> {
                question.setStatus(QuestionStatus.REVISION_REQUESTED);
            }
            case APPROVE -> {
                question.setStatus(QuestionStatus.APPROVED);
            }
            case REJECT -> {
                question.setStatus(QuestionStatus.REJECTED);
            }
            case PUBLISH -> {
                question.setStatus(QuestionStatus.PUBLISHED);
            }
            case ARCHIVE -> {
                question.setStatus(QuestionStatus.ARCHIVED);
            }
            case RESTORE -> {
                question.setStatus(QuestionStatus.DRAFT);
            }
            case CLONE_FOR_REVISION -> {
                throw new ForbiddenException("Sử dụng endpoint POST /questions/{id}/clone để sao chép câu hỏi");
            }
        }

        question.setUpdatedAt(now);
        question.setUpdatedBy(user.userId());
        var saved = questionRepository.save(question);

        return UpdateQuestionResponseMapper.toResponse(saved.getId());
    }
}
