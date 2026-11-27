package com.sep.vox.application.response.input.assessment;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentRequiredStreamType;

/**
 * The entry ticket. The assessment app relies entirely on it for the whole lifecycle of the assessment
 * session, so the ticket must say enough for the client to know what the next step is.
 *
 * <p>An empty {@code requiredStreamType} means the assessment is NOT proctored by stream: the client must skip
 * the step of requesting a token at {@code /streams/student/token}. That endpoint returns 400 for an assessment
 * without stream configuration, so if the ticket does not say so the client keeps calling and the student
 * cannot enter the assessment, even though every entry condition has been met.
 */
public record AssessmentEntryTicketResponse(
    UUID attemptId,
    String ticketId,
    String expiresAt,
    String scheduleEndAt,
    String requiredStreamType,
    String streamTypePermission,
    String chosenStreamType
) {

    /**
     * @param chosenStreamType the stream type the session HAS locked in, null when no token has been issued yet.
     *
     * <p>Only meaningful for an assessment where learners choose for themselves ({@code streamTypePermission = ANY}), and
     * there it is required rather than a convenience: {@code IssueStudentStreamTokenUseCase} locks the choice at the
     * FIRST token issue and rejects every other type with 403. An interrupted session that is re-entered goes
     * through the selection screen a second time -- without this field the client has no way to know what was locked, so it
     * shows the full set of choices again and the learner gets a 403 after sitting through the whole
     * device check.
     */
    public static AssessmentEntryTicketResponse of(
            UUID attemptId,
            String ticketId,
            String expiresAt,
            Instant scheduleEndAt,
            Assessment assessment,
            AssessmentRequiredStreamType chosenStreamType) {
        return new AssessmentEntryTicketResponse(
            attemptId,
            ticketId,
            expiresAt,
            scheduleEndAt == null ? null : scheduleEndAt.toString(),
            assessment.getRequiredStreamType() == null ? null : assessment.getRequiredStreamType().name(),
            assessment.getStreamTypePermission() == null ? null : assessment.getStreamTypePermission().name(),
            chosenStreamType == null ? null : chosenStreamType.name()
        );
    }
}
