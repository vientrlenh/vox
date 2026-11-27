package com.sep.vox.domain.model.notification;

import java.util.Map;
import java.util.Set;

import com.sep.vox.domain.common.EventTypeConstant;

public enum NotificationCategory {
    ASSESSMENT_RESULT,
    ASSESSMENT_APPEAL,
    GRADING,
    ASSESSMENT_SCHEDULE,
    ASSESSMENT_BLUEPRINT,
    BILLING,
    SYSTEM;

    /**
     * Bridge between the outbox {@code eventType} and the notification preference group.
     *
     * <p>Contains only the eventTypes that really produce a notification. The absent ones are absent on purpose:
     * {@code UserCreated} and {@code RegisterFormRejected} go to people who have never logged in, and the two OTP
     * events go to people who are outside a login session. None of the four has a device to push to -- see
     * consumer-groups.notification.topic in application.yaml, where those topics are not registered either.
     */
    private static final Map<String, NotificationCategory> BY_EVENT_TYPE = Map.ofEntries(
        Map.entry(EventTypeConstant.ASSESSMENT_APPEAL_PUBLISHED, ASSESSMENT_APPEAL),
        Map.entry(EventTypeConstant.ASSESSMENT_APPEAL_REJECTED, ASSESSMENT_APPEAL),
        Map.entry(EventTypeConstant.ASSESSMENT_APPEAL_APPROVED, ASSESSMENT_APPEAL),

        Map.entry(EventTypeConstant.ASSESSMENT_RESULT_RELEASED, ASSESSMENT_RESULT),
        Map.entry(EventTypeConstant.ASSESSMENT_RESULT_REGRADED, ASSESSMENT_RESULT),
        Map.entry(EventTypeConstant.ASSESSMENT_RESULT_INVALIDATED, ASSESSMENT_RESULT),
        Map.entry(EventTypeConstant.ASSESSMENT_RESULT_INVALID_CLEARED, ASSESSMENT_RESULT),
        Map.entry(EventTypeConstant.ASSESSMENT_RESULT_OUTCOME_DECIDED, ASSESSMENT_RESULT),

        Map.entry(EventTypeConstant.GRADING_DEADLINE_REMINDER, GRADING),
        Map.entry(EventTypeConstant.GRADING_ASSIGNMENT_DECLINED, GRADING),
        Map.entry(EventTypeConstant.ASSESSMENT_HUMAN_GRADING_REQUIRED, GRADING),
        Map.entry(EventTypeConstant.GRADING_ASSIGNMENT_OPENED, GRADING),

        Map.entry(EventTypeConstant.ASSESSMENT_BLUEPRINT_VERSION_PUBLISHED, ASSESSMENT_BLUEPRINT),

        Map.entry(EventTypeConstant.INVOICE_PAID, BILLING),

        Map.entry(EventTypeConstant.SCHOOL_DEBT_CAP_EXCEEDED, SYSTEM),
        Map.entry(EventTypeConstant.SCHOOL_LOCKED_DUE_TO_DEBT, BILLING),
        Map.entry(EventTypeConstant.SCHOOL_DEBT_CLEARED, BILLING),
        Map.entry(EventTypeConstant.SCHOOL_QUOTA_USAGE_WARNING, BILLING),

        Map.entry(EventTypeConstant.SCHOOL_SUBSCRIPTION_SUSPENDED, BILLING),
        Map.entry(EventTypeConstant.SCHOOL_SUBSCRIPTION_UNSUSPENDED, BILLING)
    );

    /**
     * @throws IllegalArgumentException when the eventType is not declared. Thrown instead of returning SYSTEM: an
     *         unknown eventType means someone added a new event and forgot the mapping, and swallowing the error here
     *         would turn it into a notification of the wrong group -- users who turned that group off would still be
     *         disturbed, and the developer would never know.
     */
    public static NotificationCategory of(String eventType) {
        NotificationCategory category = BY_EVENT_TYPE.get(eventType);
        if (category == null) {
            throw new IllegalArgumentException("No NotificationCategory mapping for eventType=" + eventType);
        }
        return category;
    }

    public static boolean isMapped(String eventType) {
        return BY_EVENT_TYPE.containsKey(eventType);
    }

    /** Used by the validator at startup, see NotificationCategoryMappingValidator. */
    public static Set<String> mappedEventTypes() {
        return BY_EVENT_TYPE.keySet();
    }
}
