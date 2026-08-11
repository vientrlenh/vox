package com.sep.vox.domain.common;

import java.util.List;

public final class EventTypeConstant {

    private EventTypeConstant() {}
    
    public static final String REGISTER_FORM_REJECTED = "RegisterFormRejected";
    public static final String USER_CREATED = "UserCreated";

    public static final String EXAM_APPEAL_PUBLISHED = "ExamAppealPublished";
    public static final String EXAM_APPEAL_REJECTED = "ExamAppealRejected";
    public static final String EXAM_APPEAL_APPROVED = "ExamAppealApproved";

    public static final String EXAM_RESULT_RELEASED = "ExamResultReleased";
    public static final String EXAM_RESULT_REGRADED = "ExamResultRegraded";
    public static final String EXAM_RESULT_INVALIDATED = "ExamResultInvalidated";
    public static final String EXAM_RESULT_INVALID_CLEARED = "ExamResultInvalidCleared";
    public static final String EXAM_RESULT_OUTCOME_DECIDED = "ExamResultOutcomeDecided";

    public static final String GRADING_DEADLINE_REMINDER = "GradingDeadlineReminder";
    public static final String GRADING_ASSIGNMENT_DECLINED = "GradingAssignmentDeclined";
    public static final String EXAM_BLUEPRINT_VERSION_PUBLISHED = "ExamBlueprintVersionPublished";

    public static List<String> all() {
        return List.of(
            REGISTER_FORM_REJECTED,
            USER_CREATED,
            EXAM_APPEAL_PUBLISHED,
            EXAM_APPEAL_REJECTED,
            EXAM_APPEAL_APPROVED,
            EXAM_RESULT_RELEASED,
            EXAM_RESULT_REGRADED,
            EXAM_RESULT_INVALIDATED,
            EXAM_RESULT_INVALID_CLEARED,
            EXAM_RESULT_OUTCOME_DECIDED,
            GRADING_DEADLINE_REMINDER,
            GRADING_ASSIGNMENT_DECLINED, 
            EXAM_BLUEPRINT_VERSION_PUBLISHED
        );
    }
}
