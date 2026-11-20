package com.sep.vox.interfaces.rest.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotNull;

public record AssignAssessmentPapersRequest(
    @NotNull(message = "Danh sách phân đề là bắt buộc")
    List<AssessmentPaperAssignmentRequestItem> assignments
) {
}
