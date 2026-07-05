package com.sep.vox.application.port.input.usecase.question;

import java.util.ArrayList;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.BulkUpdateQuestionStatusCommand;
import com.sep.vox.application.port.input.command.UpdateQuestionStatusCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.response.input.question.BulkUpdateQuestionStatusFailure;
import com.sep.vox.application.response.input.question.BulkUpdateQuestionStatusResponse;

@Service
public class BulkUpdateQuestionStatusUseCase implements IUseCase<BulkUpdateQuestionStatusCommand, BulkUpdateQuestionStatusResponse> {

    private final UpdateQuestionStatusUseCase updateQuestionStatusUseCase;

    public BulkUpdateQuestionStatusUseCase(UpdateQuestionStatusUseCase updateQuestionStatusUseCase) {
        this.updateQuestionStatusUseCase = updateQuestionStatusUseCase;
    }

    @Override
    @Transactional
    public BulkUpdateQuestionStatusResponse execute(BulkUpdateQuestionStatusCommand input) {
        if (input.questionIds() == null || input.questionIds().isEmpty()) {
            throw new IllegalStateException("Danh sách câu hỏi không được để trống");
        }

        var updated = new ArrayList<com.sep.vox.domain.dto.QuestionDto>();
        var failed = new ArrayList<BulkUpdateQuestionStatusFailure>();

        for (var questionId : input.questionIds()) {
            try {
                var dto = updateQuestionStatusUseCase.execute(
                    new UpdateQuestionStatusCommand(questionId, input.action(), input.note())
                );
                updated.add(dto);
            } catch (NotFoundException | ForbiddenException | IllegalStateException e) {
                failed.add(new BulkUpdateQuestionStatusFailure(questionId, e.getMessage()));
            }
        }

        return new BulkUpdateQuestionStatusResponse(updated, failed);
    }
}
