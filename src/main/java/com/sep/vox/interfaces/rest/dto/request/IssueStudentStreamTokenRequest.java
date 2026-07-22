package com.sep.vox.interfaces.rest.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;


public record IssueStudentStreamTokenRequest(
    @NotNull(message = "Phiên thi không được để trống")
    UUID examSessionId, 

    @NotBlank(message = "Loại stream không được để trống")
    String streamType
) {
    
}
