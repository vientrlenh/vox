package com.sep.vox.application.port.input.usecase.assessment;

import java.time.Duration;
import java.time.Instant;

import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentSchedule;
import com.sep.vox.domain.model.assessment.AssessmentStatus;

final class StudentAssessmentViewSupport {

    private StudentAssessmentViewSupport() {
    }

    static String subjectOf(Assessment assessment) {
        return subjectOf(assessment == null || assessment.getKind() == null ? null : assessment.getKind().name());
    }

    /** Bản nhận thẳng cột {@code kind} dạng chuỗi, cho nhánh đọc đi qua query repository. */
    static String subjectOf(String kind) {
        if (kind == null) {
            return "Assessment";
        }
        return kind.replace('_', ' ');
    }

    static String statusOf(Assessment assessment, AssessmentSchedule schedule, Instant now) {
        if (assessment != null && assessment.getKind() == AssessmentKind.CLASS_TEST) {
            if (assessment.getStatus() == AssessmentStatus.IN_PROGRESS) {
                return "in_progress";
            }
            if (assessment.getStatus() == AssessmentStatus.DRAFT || assessment.getStatus() == AssessmentStatus.SCHEDULED) {
                return "upcoming";
            }
            return "completed";
        }

        if (schedule == null || schedule.getStartDate() == null || schedule.getEndDate() == null) {
            return "upcoming";
        }
        if (!now.isBefore(schedule.getStartDate()) && now.isBefore(schedule.getEndDate())) {
            return "in_progress";
        }
        if (now.isBefore(schedule.getStartDate())) {
            return "upcoming";
        }
        return "completed";
    }

    static String statusOf(AssessmentSchedule schedule, Instant now) {
        return statusOf(null, schedule, now);
    }

    static int durationMinutesOf(AssessmentSchedule schedule, int fallbackMinutes) {
        return schedule == null
            ? fallbackMinutes
            : durationMinutesOf(schedule.getStartDate(), schedule.getEndDate(), fallbackMinutes);
    }

    /** Bản nhận thẳng hai mốc thời gian, cho nhánh đọc đi qua query repository. */
    static int durationMinutesOf(Instant startDate, Instant endDate, int fallbackMinutes) {
        if (startDate == null || endDate == null) {
            return fallbackMinutes;
        }
        return Math.max(1, Math.toIntExact(Duration.between(startDate, endDate).toMinutes()));
    }

    static Instant assessmentDateInstantOf(AssessmentSchedule schedule, Instant fallback) {
        if (schedule != null && schedule.getStartDate() != null) {
            return schedule.getStartDate();
        }
        return fallback;
    }

    static String assessmentDateOf(AssessmentSchedule schedule, Instant fallback) {
        var assessmentDate = assessmentDateInstantOf(schedule, fallback);
        return assessmentDate == null ? null : assessmentDate.toString();
    }
}
