package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.port.input.command.BulkUpdateQuestionStatusCommand;
import com.sep.vox.interfaces.rest.dto.request.BulkUpdateQuestionStatusRequest;

public final class BulkUpdateQuestionStatusCommandMapper {

    private BulkUpdateQuestionStatusCommandMapper() {
    }

    public static BulkUpdateQuestionStatusCommand fromRequest(BulkUpdateQuestionStatusRequest request) {
        return new BulkUpdateQuestionStatusCommand(request.questionIds(), request.action(), request.note());
    }
}
