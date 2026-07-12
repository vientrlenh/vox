package com.sep.vox.application.port.input.usecase.exam;

import java.time.Duration;
import java.time.OffsetDateTime;

import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamSchedule;
import com.sep.vox.domain.model.exam.ExamStatus;

final class StudentExamViewSupport {

    private StudentExamViewSupport() {
    }

    static String subjectOf(Exam exam) {
        if (exam == null || exam.getKind() == null) {
            return "Exam";
        }
        return exam.getKind().name().replace('_', ' ');
    }

    static String statusOf(Exam exam, ExamSchedule schedule, OffsetDateTime now) {
        if (exam != null && exam.getKind() == ExamKind.CLASS_TEST) {
            if (exam.getStatus() == ExamStatus.IN_PROGRESS) {
                return "in_progress";
            }
            if (exam.getStatus() == ExamStatus.DRAFT || exam.getStatus() == ExamStatus.SCHEDULED) {
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

    static String statusOf(ExamSchedule schedule, OffsetDateTime now) {
        return statusOf(null, schedule, now);
    }

    static int durationMinutesOf(ExamSchedule schedule, int fallbackMinutes) {
        if (schedule == null || schedule.getStartDate() == null || schedule.getEndDate() == null) {
            return fallbackMinutes;
        }
        return Math.max(1, Math.toIntExact(Duration.between(schedule.getStartDate(), schedule.getEndDate()).toMinutes()));
    }

    static String examDateOf(ExamSchedule schedule, OffsetDateTime fallback) {
        if (schedule != null && schedule.getStartDate() != null) {
            return schedule.getStartDate().toString();
        }
        return fallback == null ? null : fallback.toString();
    }
}
