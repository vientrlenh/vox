package com.sep.vox.interfaces.rest.dto.request.assessmentcandidate;

import com.sep.vox.application.port.input.command.assessmentcandidate.AssessmentPaperAssignmentItem;
import com.sep.vox.application.port.input.command.assessmentcandidate.AssignAssessmentPapersCommand;
import java.util.UUID;
import java.util.List;

import jakarta.validation.constraints.NotNull;

public record AssignAssessmentPapersRequest(
    @NotNull(message = "Danh sách phân đề là bắt buộc")
    List<AssessmentPaperAssignmentRequestItem> assignments
) {

    public static AssignAssessmentPapersCommand toCommand(UUID assessmentId, AssignAssessmentPapersRequest request) {
        List<AssessmentPaperAssignmentItem> assignments = request.assignments() == null ? List.of()
            : request.assignments().stream()
                .map(item -> new AssessmentPaperAssignmentItem(item.candidateId(), item.paperId()))
                .toList();
        return new AssignAssessmentPapersCommand(assessmentId, assignments);
    }
}
