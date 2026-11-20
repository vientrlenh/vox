package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.ImportExamCandidatesFromGradeCommand;
import com.sep.vox.interfaces.rest.dto.request.ImportExamCandidatesFromGradeRequest;

public final class ImportExamCandidatesFromGradeCommandMapper {

    private ImportExamCandidatesFromGradeCommandMapper() {
    }

    public static ImportExamCandidatesFromGradeCommand fromRequest(UUID examId,
            ImportExamCandidatesFromGradeRequest request) {
        return new ImportExamCandidatesFromGradeCommand(examId, request.schoolGradeId());
    }
}
