package com.sep.vox.interfaces.rest.dto.request.assessment;

import java.util.List;

import com.sep.vox.interfaces.rest.dto.request.AssessmentPaperAssignmentRequestItem;

import jakarta.validation.constraints.NotNull;

public record AssignAssessmentPapersRequest(
    @NotNull(message = "Danh sách phân đề là bắt buộc")
    List<AssessmentPaperAssignmentRequestItem> assignments
) {
}
