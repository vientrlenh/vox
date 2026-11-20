package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.BulkAssignExamCandidateScheduleCommand;
import com.sep.vox.interfaces.rest.dto.request.BulkAssignExamCandidateScheduleRequest;

public final class BulkAssignExamCandidateScheduleCommandMapper {

    private BulkAssignExamCandidateScheduleCommandMapper() {
    }

    public static BulkAssignExamCandidateScheduleCommand fromRequest(UUID examId,
            BulkAssignExamCandidateScheduleRequest request) {
        return new BulkAssignExamCandidateScheduleCommand(examId, request.candidateIds(), request.scheduleId());
    }
}
