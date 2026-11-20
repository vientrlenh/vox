package com.sep.vox.domain.model.assessment;

import java.util.EnumSet;

public enum AssessmentCandidateStatus {
    ASSIGNED,  
    ATTENDED,
    ABSENT, 
    COMPLETED, 
    EXEMPTED, // được miễn/hoãn thi
    CANCELLED; 

    private static final EnumSet<AssessmentCandidateStatus> NON_SCORABLE_STATUSES = EnumSet.of(
        AssessmentCandidateStatus.EXEMPTED,
        AssessmentCandidateStatus.CANCELLED
    );

    private static final EnumSet<AssessmentCandidateStatus> ENTRY_BLOCKED_STATUSES = EnumSet.of(
        AssessmentCandidateStatus.ABSENT,
        AssessmentCandidateStatus.EXEMPTED,
        AssessmentCandidateStatus.CANCELLED
    );

    public static boolean isNonScorable(AssessmentCandidateStatus status) {
        return status != null && NON_SCORABLE_STATUSES.contains(status);
    }

    public static boolean isBlockedForEntry(AssessmentCandidateStatus status) {
        return status != null && ENTRY_BLOCKED_STATUSES.contains(status);
    }

    public static boolean isAttended(AssessmentCandidateStatus status) {
        return status == AssessmentCandidateStatus.ATTENDED;
    }
}
