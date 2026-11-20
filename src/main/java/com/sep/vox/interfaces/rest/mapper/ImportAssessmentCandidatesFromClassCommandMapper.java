package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.ImportAssessmentCandidatesFromClassCommand;
import com.sep.vox.interfaces.rest.dto.request.ImportAssessmentCandidatesFromClassRequest;

public final class ImportAssessmentCandidatesFromClassCommandMapper {

    private ImportAssessmentCandidatesFromClassCommandMapper() {
    }

    public static ImportAssessmentCandidatesFromClassCommand fromRequest(UUID assessmentId,
            ImportAssessmentCandidatesFromClassRequest request) {
        return new ImportAssessmentCandidatesFromClassCommand(assessmentId, request.schoolClassId());
    }
}
