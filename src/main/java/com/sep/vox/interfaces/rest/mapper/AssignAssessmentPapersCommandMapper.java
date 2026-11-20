package com.sep.vox.interfaces.rest.mapper;

import java.util.List;
import java.util.UUID;

import com.sep.vox.application.port.input.command.AssignAssessmentPapersCommand;
import com.sep.vox.application.port.input.command.AssessmentPaperAssignmentItem;
import com.sep.vox.interfaces.rest.dto.request.AssignAssessmentPapersRequest;

public final class AssignAssessmentPapersCommandMapper {

    private AssignAssessmentPapersCommandMapper() {
    }

    public static AssignAssessmentPapersCommand fromRequest(UUID assessmentId, AssignAssessmentPapersRequest request) {
        List<AssessmentPaperAssignmentItem> assignments = request.assignments() == null ? List.of()
            : request.assignments().stream()
                .map(item -> new AssessmentPaperAssignmentItem(item.candidateId(), item.paperId()))
                .toList();
        return new AssignAssessmentPapersCommand(assessmentId, assignments);
    }
}
