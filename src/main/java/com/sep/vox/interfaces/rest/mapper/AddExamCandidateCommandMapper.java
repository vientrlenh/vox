package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.AddExamCandidateCommand;
import com.sep.vox.interfaces.rest.dto.request.AddExamCandidateRequest;

public final class AddExamCandidateCommandMapper {

    private AddExamCandidateCommandMapper() {
    }

    public static AddExamCandidateCommand fromRequest(UUID examId, AddExamCandidateRequest request) {
        return new AddExamCandidateCommand(examId, request.studentId());
    }
}
