package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.ImportExamCandidatesFromClassCommand;
import com.sep.vox.interfaces.rest.dto.request.ImportExamCandidatesFromClassRequest;

public final class ImportExamCandidatesFromClassCommandMapper {

    private ImportExamCandidatesFromClassCommandMapper() {
    }

    public static ImportExamCandidatesFromClassCommand fromRequest(UUID examId,
            ImportExamCandidatesFromClassRequest request) {
        return new ImportExamCandidatesFromClassCommand(examId, request.schoolClassId());
    }
}
