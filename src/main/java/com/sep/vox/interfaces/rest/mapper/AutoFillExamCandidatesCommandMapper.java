package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.AutoFillExamCandidatesCommand;
import com.sep.vox.interfaces.rest.dto.request.AutoFillExamCandidatesRequest;

public final class AutoFillExamCandidatesCommandMapper {

    private AutoFillExamCandidatesCommandMapper() {
    }

    public static AutoFillExamCandidatesCommand fromRequest(UUID examId, AutoFillExamCandidatesRequest request) {
        var scheduleIds = request == null ? null : request.scheduleIds();
        return new AutoFillExamCandidatesCommand(examId, scheduleIds);
    }
}
