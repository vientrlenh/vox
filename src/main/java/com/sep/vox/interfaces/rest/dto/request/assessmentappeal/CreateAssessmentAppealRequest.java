package com.sep.vox.interfaces.rest.dto.request.assessmentappeal;

import com.sep.vox.application.port.input.command.assessmentappeal.CreateAssessmentAppealCommand;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAssessmentAppealRequest(
    @NotNull(message = "Phải chọn kết quả bài thi cần phúc khảo")
    UUID candidateResultId,

    /**
     * Empty = appeal the WHOLE submission, the use case fills in every part that has an answer.
     * A specific list is still accepted so old clients do not break -- but the appeal grader
     * always has to grade every part, so this list does not narrow the grading scope.
     */
    List<UUID> paperItemIds,

    @NotBlank(message = "Phải nêu lý do phúc khảo")
    @Size(max = 512, message = "Lý do phúc khảo tối đa 512 ký tự")
    String reason,

    @Size(max = 512, message = "Ghi chú tối đa 512 ký tự")
    String notes
) {

    public static CreateAssessmentAppealCommand toCommand(CreateAssessmentAppealRequest request) {
        return new CreateAssessmentAppealCommand(
            request.candidateResultId(),
            request.paperItemIds(),
            request.reason(),
            request.notes()
        );
    }
}
