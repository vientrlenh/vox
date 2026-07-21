package com.sep.vox.interfaces.rest.dto.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record AssignExamAppealReviewersRequest(
    @NotEmpty(message = "Phải phân công ít nhất 1 giám khảo")
    @Size(max = 5, message = "Chỉ được phân công tối đa 5 giám khảo")
    List<UUID> reviewerIds
) {
}
