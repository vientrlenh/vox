package com.sep.vox.interfaces.rest.dto.request.stream;

import java.util.UUID;

import com.sep.vox.application.port.input.command.stream.IssueStudentStreamTokenCommand;
import com.sep.vox.domain.model.assessment.AssessmentRequiredStreamType;

import jakarta.validation.constraints.NotNull;


public record IssueStudentStreamTokenRequest(
    @NotNull(message = "Phiên thi không được để trống")
    UUID assessmentSessionId,

    /**
     * The stream type the learner wants to use: CAMERA, SCREEN or CAMERA_AND_SCREEN (case-
     * insensitive).
     *
     * <p>Empty is valid and is the most common case: the server already knows what the assessment requires
     * ({@code assessment.requiredStreamType}) so the client does not need to say it again. Only when the assessment sets
     * {@code streamTypePermission = ANY} does this field really have an effect, because that is the only
     * configuration that gives the learner more than one valid choice.
     *
     * <p>The choice is fixed on the first token issued for the assessment session; later requests may only send
     * that same value again or leave it empty.
     */
    AssessmentRequiredStreamType streamType
) {

    public static IssueStudentStreamTokenCommand toCommand(IssueStudentStreamTokenRequest request) {
        return new IssueStudentStreamTokenCommand(
            request.assessmentSessionId(),
            request.streamType()
        );
    }
}
