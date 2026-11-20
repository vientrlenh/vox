package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.ImportAssessmentCandidatesFromGradeCommand;
import com.sep.vox.interfaces.rest.dto.request.ImportAssessmentCandidatesFromGradeRequest;

public final class ImportAssessmentCandidatesFromGradeCommandMapper {

    private ImportAssessmentCandidatesFromGradeCommandMapper() {
    }

    public static ImportAssessmentCandidatesFromGradeCommand fromRequest(UUID assessmentId,
            ImportAssessmentCandidatesFromGradeRequest request) {
        return new ImportAssessmentCandidatesFromGradeCommand(assessmentId, request.schoolGradeId());
    }
}
