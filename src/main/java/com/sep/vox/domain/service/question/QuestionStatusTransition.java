package com.sep.vox.domain.service.question;

import java.util.UUID;

import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.model.question.QuestionBank;
import com.sep.vox.domain.model.question.QuestionBankOwnerType;
import com.sep.vox.domain.model.question.QuestionStatus;

/**
 * Question status transition rules — checks permission and valid status for each action.
 *
 * <p>Deliberately a pure static class, no dependencies, and it does NOT throw: every reason for rejection is
 * <em>returned</em> as a {@link Rejection}. This is the crux, not a coding preference.
 * The bulk update path must handle each faulty question and move on; if this rule lived
 * in a Spring-proxied bean and threw, the transaction interceptor would mark the
 * bulk use case's transaction rollback-only at the first faulty question — the loop catches the
 * exception and carries on normally, but at commit time {@code UnexpectedRollbackException} blows up
 * (HTTP 500) and drags down the questions that were updated successfully. Without a proxy that failure cannot
 * occur.
 *
 * <p>The message strings here are user-facing text: they are returned straight to the client in
 * {@code failed[].reason} of the bulk response. Because the bulk review screen often rejects dozens of questions
 * at once, each reason must say on its own <em>why this question was skipped</em> (which status it is in,
 * which status is needed) and not just "invalid" — otherwise the user does not know what to fix.
 * Accompanied by {@link RejectionCode} so the client can group questions with the same reason without matching
 * strings.
 */
public final class QuestionStatusTransition {

    /**
     * Message for data that does not exist — shared so the single endpoint and the bulk endpoint say
     * the same sentence, even though one throws an exception and the other collects it into {@code failed[]}.
     */
    public static final String QUESTION_NOT_FOUND = "Question not found";
    public static final String QUESTION_BANK_NOT_FOUND = "Question bank not found";

    private QuestionStatusTransition() {
    }

    /**
     * Context of the actor, already resolved once for the whole batch.
     *
     * @param schoolAdmin excludes system admin (exactly the way the use case computed it before)
     */
    public record Actor(UUID userId, UUID schoolId, boolean systemAdmin, boolean schoolAdmin) {
    }

    /** Classification of the rejection reason so the application layer maps it to the right HTTP status for the single endpoint. */
    public enum RejectionKind {
        FORBIDDEN, INVALID_STATE
    }

    /**
     * Stable reason code for clients. Used to group / re-translate on the UI side; the text in
     * {@link Rejection#reason()} can change without breaking clients.
     */
    public enum RejectionCode {
        /** Question not found (only used on the bulk path). */
        QUESTION_NOT_FOUND,
        /** The question bank of this question was not found. */
        QUESTION_BANK_NOT_FOUND,
        /** The action sent is not in the supported list. */
        INVALID_ACTION,
        /** Not enough permission on the bank / question. */
        NO_PERMISSION,
        /** The author reviewing their own work. */
        SELF_REVIEW,
        /** The action is only for the admin of the question bank. */
        ADMIN_ONLY,
        /** The current status does not allow the action. */
        INVALID_STATUS,
        /** The action requires an accompanying reason. */
        NOTE_REQUIRED,
        /** Locking a question that is already locked. */
        ALREADY_LOCKED,
        /** Unlocking a question that is not locked. */
        NOT_LOCKED
    }

    public record Rejection(RejectionKind kind, RejectionCode code, String reason) {
    }

    /**
     * Checks whether an action is allowed on a question that already exists.
     *
     * <p>{@code question} and {@code bank} must not be {@code null}: "does the data exist" is the
     * caller's concern (only the caller knows whether to throw {@code ResourceNotFoundException} or collect
     * the failure into {@code failed[]}), so what remains here is purely the business rule.
     *
     * @return {@code null} if the action is allowed, otherwise the reason it is rejected.
     */
    public static Rejection rejectionFor(
            Question question,
            QuestionBank bank,
            boolean editorCollaborator,
            Actor actor,
            String action,
            String note) {
        if (action == null) {
            return invalidAction();
        }
        Standing standing = standingOf(question, bank, editorCollaborator, actor);
        return switch (action) {
            case "SUBMIT" -> submitRejection(question, standing, action);
            case "APPROVE" -> reviewRejection(question, standing, action);
            case "REJECT", "REQUEST_REVISION" -> firstRejection(
                reviewRejection(question, standing, action),
                requireNote(note, action));
            case "PUBLISH" -> publishRejection(question, standing, action);
            case "ARCHIVE" -> adminRejection(standing, action, question.getStatus(), QuestionStatus.PUBLISHED);
            case "REOPEN" -> adminRejection(standing, action, question.getStatus(), QuestionStatus.ARCHIVED);
            case "LOCK" -> lockRejection(question, standing, action);
            case "UNLOCK" -> unlockRejection(question, standing, action);
            default -> invalidAction();
        };
    }

    private record Standing(
            boolean owner,
            boolean editorCollaborator,
            boolean systemAdminOnSystemBank,
            boolean schoolAdminOnSchoolBank) {
    }

    private static Standing standingOf(Question question, QuestionBank bank, boolean editorCollaborator, Actor actor) {
        boolean owner = question.isCreatedBy(actor.userId());
        boolean systemAdminOnSystemBank = actor.systemAdmin()
            && bank.getOwnerType() == QuestionBankOwnerType.SYSTEM;
        boolean schoolAdminOnSchoolBank = bank.getOwnerType() == QuestionBankOwnerType.SCHOOL
            && actor.schoolAdmin()
            && actor.schoolId() != null
            && actor.schoolId().equals(bank.getSchoolId());
        return new Standing(owner, editorCollaborator, systemAdminOnSystemBank, schoolAdminOnSchoolBank);
    }

    private static Rejection submitRejection(Question question, Standing standing, String action) {
        return firstRejection(
            standing.owner() || standing.editorCollaborator()
                ? null
                : forbidden(RejectionCode.NO_PERMISSION, action,
                    "you are not the creator or a collaborator with edit rights on this question"),
            requireStatus(question.getStatus(), action,
                QuestionStatus.DRAFT, QuestionStatus.REVISION_REQUESTED));
    }

    private static Rejection reviewRejection(Question question, Standing standing, String action) {
        return firstRejection(
            reviewPermission(standing, action),
            requireStatus(question.getStatus(), action, QuestionStatus.SUBMITTED_FOR_REVIEW));
    }

    private static Rejection adminRejection(
            Standing standing, String action, QuestionStatus actual, QuestionStatus required) {
        return firstRejection(
            adminPermission(standing, action),
            requireStatus(actual, action, required));
    }

    private static Rejection publishRejection(Question question, Standing standing, String action) {
        // Re-publishing an archived question is an admin action; for a first publication the
        // owner / a collaborator can also do it, as long as the question has been approved.
        if (question.getStatus() == QuestionStatus.ARCHIVED) {
            return adminPermission(standing, action);
        }
        Rejection statusRejection = requireStatus(question.getStatus(), action, QuestionStatus.APPROVED);
        if (statusRejection != null) {
            return statusRejection;
        }
        return standing.owner() || standing.editorCollaborator()
                || standing.systemAdminOnSystemBank() || standing.schoolAdminOnSchoolBank()
            ? null
            : forbidden(RejectionCode.NO_PERMISSION, action, "you do not have permission to publish this question");
    }

    private static Rejection lockRejection(Question question, Standing standing, String action) {
        Rejection permission = adminPermission(standing, action);
        if (permission != null) {
            return permission;
        }
        return question.isLocked()
            ? new Rejection(RejectionKind.INVALID_STATE, RejectionCode.ALREADY_LOCKED,
                prefix(action) + "the question is locked")
            : null;
    }

    private static Rejection unlockRejection(Question question, Standing standing, String action) {
        Rejection permission = adminPermission(standing, action);
        if (permission != null) {
            return permission;
        }
        return question.isLocked()
            ? null
            : new Rejection(RejectionKind.INVALID_STATE, RejectionCode.NOT_LOCKED,
                prefix(action) + "the question is not locked");
    }

    /**
     * Applies the action to the question. Only call when {@link #rejectionFor} has returned {@code null} —
     * this function does not re-check permission or status.
     */
    public static void apply(Question question, String action) {
        switch (action) {
            case "SUBMIT" -> question.setStatus(QuestionStatus.SUBMITTED_FOR_REVIEW);
            case "APPROVE" -> question.setStatus(QuestionStatus.APPROVED);
            case "REJECT" -> question.setStatus(QuestionStatus.REJECTED);
            case "REQUEST_REVISION" -> question.setStatus(QuestionStatus.REVISION_REQUESTED);
            case "PUBLISH" -> question.setStatus(QuestionStatus.PUBLISHED);
            case "ARCHIVE" -> question.setStatus(QuestionStatus.ARCHIVED);
            case "REOPEN" -> question.setStatus(QuestionStatus.DRAFT);
            case "LOCK" -> question.setLocked(true);
            case "UNLOCK" -> question.setLocked(false);
            default -> throw new IllegalArgumentException("Invalid action: " + action);
        }
    }

    /** English label of the status — matches the label on the UI so the two sides do not say different things. */
    public static String statusLabel(QuestionStatus status) {
        return switch (status) {
            case DRAFT -> "Draft";
            case SUBMITTED_FOR_REVIEW -> "Awaiting review";
            case REVISION_REQUESTED -> "Revision requested";
            case APPROVED -> "Approved";
            case REJECTED -> "Rejected";
            case PUBLISHED -> "Published";
            case ARCHIVED -> "Archived";
        };
    }

    private static Rejection firstRejection(Rejection... rejections) {
        for (Rejection rejection : rejections) {
            if (rejection != null) {
                return rejection;
            }
        }
        return null;
    }

    private static Rejection adminPermission(Standing standing, String action) {
        return standing.systemAdminOnSystemBank() || standing.schoolAdminOnSchoolBank()
            ? null
            : forbidden(RejectionCode.ADMIN_ONLY, action,
                "only an admin of the question bank can do this");
    }

    /**
     * A question's author may not approve their own work, even when they are a collaborator with
     * edit rights: only an admin or another collaborator can review it.
     */
    private static Rejection reviewPermission(Standing standing, String action) {
        if (standing.systemAdminOnSystemBank() || standing.schoolAdminOnSchoolBank()) {
            return null;
        }
        if (standing.editorCollaborator() && !standing.owner()) {
            return null;
        }
        // The author reviewing their own question is a separate case: it is the most common rejection
        // reason on the bulk review screen, and "you have no permission" makes users think a role is missing.
        return standing.owner()
            ? forbidden(RejectionCode.SELF_REVIEW, action, "you are the creator of this question, someone else must review it")
            : forbidden(RejectionCode.NO_PERMISSION, action, "you do not have permission to review questions in this question bank");
    }

    private static Rejection requireStatus(QuestionStatus actual, String action, QuestionStatus... allowed) {
        for (QuestionStatus status : allowed) {
            if (actual == status) {
                return null;
            }
        }
        return new Rejection(RejectionKind.INVALID_STATE, RejectionCode.INVALID_STATUS,
            prefix(action) + "the question is in status \"" + statusLabel(actual)
                + "\", this action only applies to questions in status " + allowedLabels(allowed));
    }

    private static Rejection requireNote(String note, String action) {
        return note == null || note.isBlank()
            ? new Rejection(RejectionKind.INVALID_STATE, RejectionCode.NOTE_REQUIRED,
                prefix(action) + "this action requires a reason")
            : null;
    }

    private static Rejection forbidden(RejectionCode code, String action, String detail) {
        return new Rejection(RejectionKind.FORBIDDEN, code, prefix(action) + detail);
    }

    private static Rejection invalidAction() {
        return new Rejection(RejectionKind.INVALID_STATE, RejectionCode.INVALID_ACTION, "Invalid action");
    }

    private static String prefix(String action) {
        return "Cannot " + actionLabel(action) + ": ";
    }

    private static String actionLabel(String action) {
        return switch (action) {
            case "SUBMIT" -> "submit for review";
            case "APPROVE" -> "approve";
            case "REJECT" -> "reject";
            case "REQUEST_REVISION" -> "request revision";
            case "PUBLISH" -> "publish";
            case "ARCHIVE" -> "archive";
            case "REOPEN" -> "reopen";
            case "LOCK" -> "lock";
            case "UNLOCK" -> "unlock";
            default -> action;
        };
    }

    private static String allowedLabels(QuestionStatus... allowed) {
        StringBuilder labels = new StringBuilder();
        for (int index = 0; index < allowed.length; index++) {
            if (index > 0) {
                labels.append(index == allowed.length - 1 ? " or " : ", ");
            }
            labels.append('"').append(statusLabel(allowed[index])).append('"');
        }
        return labels.toString();
    }
}
