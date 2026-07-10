package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.AssignExamCandidateScheduleCommand;
import com.sep.vox.interfaces.rest.dto.request.AssignExamCandidateScheduleRequest;

public final class AssignExamCandidateScheduleCommandMapper {

    private AssignExamCandidateScheduleCommandMapper() {
    }

    public static AssignExamCandidateScheduleCommand fromRequest(UUID examId, UUID candidateId,
            AssignExamCandidateScheduleRequest request) {
        return new AssignExamCandidateScheduleCommand(examId, candidateId, request.scheduleId());
    }
}
