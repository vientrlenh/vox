package com.sep.vox.domain.model.exam;

import java.util.EnumSet;

public enum ExamCandidateStatus {
    ASSIGNED,  
    ATTENDED,
    ABSENT, 
    COMPLETED, 
    EXEMPTED, // được miễn/hoãn thi
    CANCELLED; 

    private static final EnumSet<ExamCandidateStatus> NON_SCORABLE_STATUSES = EnumSet.of(
        ExamCandidateStatus.EXEMPTED,
        ExamCandidateStatus.CANCELLED
    );

    private static final EnumSet<ExamCandidateStatus> ENTRY_BLOCKED_STATUSES = EnumSet.of(
        ExamCandidateStatus.ABSENT,
        ExamCandidateStatus.EXEMPTED,
        ExamCandidateStatus.CANCELLED
    );

    public static boolean isNonScorable(ExamCandidateStatus status) {
        return status != null && NON_SCORABLE_STATUSES.contains(status);
    }

    public static boolean isBlockedForEntry(ExamCandidateStatus status) {
        return status != null && ENTRY_BLOCKED_STATUSES.contains(status);
    }

    public static boolean isAttended(ExamCandidateStatus status) {
        return status == ExamCandidateStatus.ATTENDED;
    }
}
