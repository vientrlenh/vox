package com.sep.vox.application.port.input.usecase.questionbank;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.ReviewQuestionBankCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.QuestionBankPermissionQuery;
import com.sep.vox.application.response.input.questionbank.UpdateQuestionBankResponse;
import com.sep.vox.domain.model.question.QuestionBankStatus;
import com.sep.vox.domain.repository.QuestionBankRepository;

@Service
public class ReviewQuestionBankUseCase implements IUseCase<ReviewQuestionBankCommand, UpdateQuestionBankResponse> {

    private final QuestionBankRepository questionBankRepository;
    private final QuestionBankPermissionQuery permissionQuery;
    private final UserContextPort userContextPort;

    public ReviewQuestionBankUseCase(
            QuestionBankRepository questionBankRepository,
            QuestionBankPermissionQuery permissionQuery,
            UserContextPort userContextPort) {
        this.questionBankRepository = questionBankRepository;
        this.permissionQuery = permissionQuery;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public UpdateQuestionBankResponse execute(ReviewQuestionBankCommand input) {
        boolean permitted = switch (input.targetStatus()) {
            case PUBLISHED -> permissionQuery.canPublishBank(input.bankId());
            case ARCHIVED -> permissionQuery.canArchiveBank(input.bankId());
            case DRAFT -> permissionQuery.canRestoreBank(input.bankId());
        };

        if (!permitted) {
            throw new ForbiddenException("Không có quyền thực hiện hành động này");
        }

        var bank = questionBankRepository.findById(input.bankId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy ngân hàng câu hỏi"));

        bank.setStatus(input.targetStatus());
        bank.setUpdatedAt(OffsetDateTime.now());
        bank.setUpdatedBy(userContextPort.getCurrentAuthenticatedUserId());
        var saved = questionBankRepository.save(bank);

        return new UpdateQuestionBankResponse(saved.getId());
    }
}
