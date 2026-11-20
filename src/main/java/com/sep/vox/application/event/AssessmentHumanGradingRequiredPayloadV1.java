package com.sep.vox.application.event;

import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentKind;

/**
 * An assessment has just closed and some attempts are still in {@code PENDING_REVIEW} ("Waiting for AI score
 * review") -- somebody has to grade them by hand.
 *
 * <p>Produces an in-app notification ONLY, NO email: this is a recurring work reminder per assessment, not a
 * receipt or a message to someone outside a login session. That is why the event goes on its own topic that only
 * the notification consumer reads -- see
 * {@code consumer-groups.notification.topic.assessment-grading-review} in application.yaml.
 *
 * <p>One event for the WHOLE assessment, not one event per attempt: an assessment has hundreds of candidates,
 * and one notification per attempt to grade is the fastest way to make people turn this notification group
 * off completely.
 *
 * @param recipientIds fixed at publish time, never queried again by the consumer -- same reason as
 *        {@link InvoicePaidPayloadV1}: a rerun (retry, replay from the DLT) must produce exactly the same set of
 *        recipients, otherwise uk_notifications_user_event cannot block the duplicates
 * @param assessmentKind decides whether the client opens the grading queue of a class test or of a centralized assessment
 * @param pendingCount number of attempts waiting for manual grading at the moment of closing
 */
public record AssessmentHumanGradingRequiredPayloadV1(
    List<UUID> recipientIds,
    UUID assessmentId,
    String assessmentName,
    AssessmentKind assessmentKind,
    int pendingCount
) {

}
