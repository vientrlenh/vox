package com.sep.vox.application.common;

import java.util.EnumSet;

import com.sep.vox.domain.model.exam.ExamCandidateStatus;

public final class ExamCandidateStatusSupport {

    private static final EnumSet<ExamCandidateStatus> NON_SCORABLE_STATUSES = EnumSet.of(
        ExamCandidateStatus.ABSENT,
        ExamCandidateStatus.EXEMPTED,
        ExamCandidateStatus.CANCELLED
    );

    private ExamCandidateStatusSupport() {
    }

    public static boolean isNonScorable(ExamCandidateStatus status) {
        return status != null && NON_SCORABLE_STATUSES.contains(status);
    }
}
