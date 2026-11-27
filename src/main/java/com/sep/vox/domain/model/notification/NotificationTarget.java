package com.sep.vox.domain.model.notification;

import java.util.Map;
import java.util.Set;

import com.sep.vox.domain.common.EventTypeConstant;

/**
 * The screen a notification leads to when the user taps it.
 *
 * <p>The names here are deliberately NOT URLs: web, Flutter and desktop have three very different route tables,
 * and the {@code notifications.payload} column lives forever -- embedding a real path in it would mean every old
 * row lies the day a route is renamed. The server decides "what to open", and the client translates it to
 * "which path to open".
 *
 * <p>Many eventTypes converging on one target is normal, and is exactly why this class exists: the five
 * grading-group events all lead to the same result screen. So adding a new event type for an existing screen only
 * requires editing the table below, and the client does not have to be released again
 * -- for the Flutter app that is the difference between one deploy and a store review cycle.
 *
 * <p>The prefix states the recipient's role, because the id alone cannot tell them apart:
 * {@code assignmentId} accompanies both {@code GradingDeadlineReminder} (sent to the teacher) and
 * {@code GradingAssignmentDeclined} (sent to the admin who assigned the work), and {@code schoolId} accompanies
 * both the event sent to school admins and the event sent to system admins. A client guessing by key name would
 * lead the wrong person into a screen they are not allowed to see.
 */
public enum NotificationTarget {

    /** Student: the result of an assessment. */
    ASSESSMENT_RESULT_DETAIL,

    /** Student: the detail of an appeal. */
    ASSESSMENT_APPEAL_DETAIL,

    /** Teacher: a specific grading assignment. */
    TEACHER_GRADING_TASK,

    /** School admin: an assignment that a teacher just handed back, to be given to someone else. */
    ADMIN_GRADING_ASSIGNMENT,

    /**
     * Manual grading queue of ONE assessment.
     *
     * <p>The only target for both recipient roles: a class test notifies the teacher who owns the paper, a centralized
     * assessment notifies both the chair of the committee and the school admin. The semantic destination is
     * identical -- "go grade the pending results" -- only the path differs by the role of whoever opens it, and that
     * is the job of the client route table.
     */
    ASSESSMENT_HUMAN_GRADING_REQUIRED,

    /** School admin: an assessment blueprint that was just published. */
    SCHOOL_BLUEPRINT_DETAIL,

    /** School admin: an invoice of the school. */
    SCHOOL_INVOICE_DETAIL,

    /** School admin: the school's debt/AI quota page. */
    SCHOOL_BILLING_OVERVIEW,

    /** School admin: the school's subscription plan. */
    SCHOOL_SUBSCRIPTION_DETAIL,

    /** System admin: a school that needs attention. */
    SYSTEM_SCHOOL_ATTENTION;

    /**
     * Bridge between the outbox {@code eventType} and the destination screen.
     *
     * <p>Must cover exactly the set of eventTypes that {@link NotificationCategory} declares: the category decides
     * which events produce a notification, and this table decides where that notification opens. A mismatch between
     * the two tables is blocked at startup, see
     * {@code NotificationCategoryMappingValidator}.
     */
    private static final Map<String, NotificationTarget> BY_EVENT_TYPE = Map.ofEntries(
        Map.entry(EventTypeConstant.ASSESSMENT_RESULT_RELEASED, ASSESSMENT_RESULT_DETAIL),
        Map.entry(EventTypeConstant.ASSESSMENT_RESULT_REGRADED, ASSESSMENT_RESULT_DETAIL),
        Map.entry(EventTypeConstant.ASSESSMENT_RESULT_INVALIDATED, ASSESSMENT_RESULT_DETAIL),
        Map.entry(EventTypeConstant.ASSESSMENT_RESULT_INVALID_CLEARED, ASSESSMENT_RESULT_DETAIL),
        Map.entry(EventTypeConstant.ASSESSMENT_RESULT_OUTCOME_DECIDED, ASSESSMENT_RESULT_DETAIL),

        Map.entry(EventTypeConstant.ASSESSMENT_APPEAL_PUBLISHED, ASSESSMENT_APPEAL_DETAIL),
        Map.entry(EventTypeConstant.ASSESSMENT_APPEAL_REJECTED, ASSESSMENT_APPEAL_DETAIL),
        Map.entry(EventTypeConstant.ASSESSMENT_APPEAL_APPROVED, ASSESSMENT_APPEAL_DETAIL),

        Map.entry(EventTypeConstant.GRADING_DEADLINE_REMINDER, TEACHER_GRADING_TASK),
        // Same destination as the deadline reminder: both lead the teacher to their own assignment, so
        // no new target is needed -- this is where the target table pays off.
        Map.entry(EventTypeConstant.GRADING_ASSIGNMENT_OPENED, TEACHER_GRADING_TASK),
        // Carries the same assignmentId as the event above, but the recipient is the admin who assigned the work, not
        // the teacher -- so it is a different target, a different screen.
        Map.entry(EventTypeConstant.GRADING_ASSIGNMENT_DECLINED, ADMIN_GRADING_ASSIGNMENT),

        Map.entry(EventTypeConstant.ASSESSMENT_HUMAN_GRADING_REQUIRED, ASSESSMENT_HUMAN_GRADING_REQUIRED),

        Map.entry(EventTypeConstant.ASSESSMENT_BLUEPRINT_VERSION_PUBLISHED, SCHOOL_BLUEPRINT_DETAIL),

        Map.entry(EventTypeConstant.INVOICE_PAID, SCHOOL_INVOICE_DETAIL),

        // The three debt/quota events sent to school admins all lead to the same overview page: they do not
        // point at any specific entity, they only say the school's situation just changed.
        Map.entry(EventTypeConstant.SCHOOL_LOCKED_DUE_TO_DEBT, SCHOOL_BILLING_OVERVIEW),
        Map.entry(EventTypeConstant.SCHOOL_DEBT_CLEARED, SCHOOL_BILLING_OVERVIEW),
        Map.entry(EventTypeConstant.SCHOOL_QUOTA_USAGE_WARNING, SCHOOL_BILLING_OVERVIEW),

        // Carries the same schoolId as the three events above, but the recipient is a system admin looking at
        // someone else's school, not a school admin looking at their own.
        Map.entry(EventTypeConstant.SCHOOL_DEBT_CAP_EXCEEDED, SYSTEM_SCHOOL_ATTENTION),

        Map.entry(EventTypeConstant.SCHOOL_SUBSCRIPTION_SUSPENDED, SCHOOL_SUBSCRIPTION_DETAIL),
        Map.entry(EventTypeConstant.SCHOOL_SUBSCRIPTION_UNSUSPENDED, SCHOOL_SUBSCRIPTION_DETAIL)
    );

    /**
     * @throws IllegalArgumentException when the eventType is not declared. Thrown instead of returning
     *         a default target: guessing would push the user into an unrelated screen, and nobody reports an
     *         error of that kind. In practice this branch cannot run --
     *         the consumer only calls here after {@link NotificationCategory#isMapped} returned
     *         true, and the startup validator requires both tables to cover the same set of eventTypes.
     */
    public static NotificationTarget of(String eventType) {
        NotificationTarget target = BY_EVENT_TYPE.get(eventType);
        if (target == null) {
            throw new IllegalArgumentException("No NotificationTarget mapping for eventType=" + eventType);
        }
        return target;
    }

    public static boolean isMapped(String eventType) {
        return BY_EVENT_TYPE.containsKey(eventType);
    }

    /** Used by the validator at startup, see NotificationCategoryMappingValidator. */
    public static Set<String> mappedEventTypes() {
        return BY_EVENT_TYPE.keySet();
    }
}
