package com.sep.vox.interfaces.rest.dto.request.questionbank;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Shared by the bulk status change of question banks and question topics.
 *
 * <p>Accepts only PUBLISH/ARCHIVE -- these two entities have no review cycle like questions, so there is no
 * SUBMIT/APPROVE/REJECT and no note is needed either.
 */
public record BulkUpdateQuestionScopeStatusRequest(
    @NotEmpty(message = "List must not be blank")
    @Size(max = 200, message = "At most 200 items can be updated at a time")
    List<UUID> ids,

    @NotBlank(message = "Action is required")
    @Pattern(regexp = "PUBLISH|ARCHIVE", message = "Action is invalid")
    String action
) {
}
