package com.sep.vox.interfaces.rest.dto.request.assessment;

import java.util.List;

import com.sep.vox.interfaces.rest.dto.request.ExamPaperAssignmentRequestItem;

import jakarta.validation.constraints.NotNull;

public record AssignExamPapersRequest(
    @NotNull(message = "Danh sách phân đề là bắt buộc")
    List<ExamPaperAssignmentRequestItem> assignments
) {
}
