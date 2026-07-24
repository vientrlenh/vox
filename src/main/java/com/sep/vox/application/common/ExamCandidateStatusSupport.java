package com.sep.vox.application.common;

import java.util.EnumSet;

import com.sep.vox.domain.model.exam.ExamCandidateStatus;

public final class ExamCandidateStatusSupport {

    private static final EnumSet<ExamCandidateStatus> NON_SCORABLE_STATUSES = EnumSet.of(
        ExamCandidateStatus.EXEMPTED,
        ExamCandidateStatus.CANCELLED
    );

    private static final EnumSet<ExamCandidateStatus> ENTRY_BLOCKED_STATUSES = EnumSet.of(
        ExamCandidateStatus.ABSENT,
        ExamCandidateStatus.EXEMPTED,
        ExamCandidateStatus.CANCELLED
    );

    private ExamCandidateStatusSupport() {
    }

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
