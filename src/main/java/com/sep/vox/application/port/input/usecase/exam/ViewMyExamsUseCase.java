package com.sep.vox.application.port.input.usecase.exam;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.sep.vox.application.common.ExamCandidateStatusSupport;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.dto.ExamAttemptSummary;
import com.sep.vox.application.query.repository.ExamCandidateAttemptsQueryRepository;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.exam.StudentExamSessionSummaryResponse;
import com.sep.vox.application.response.input.exam.StudentExamSummaryResponse;
import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamCandidate;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamSchedule;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamPaperRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamScheduleRepository;
import com.sep.vox.domain.model.exam.ExamSessionStatus;

@Service
public class ViewMyExamsUseCase implements IUseCase<Void, List<StudentExamSummaryResponse>> {

    private final ExamCandidateRepository examCandidateRepository;
    private final ExamRepository examRepository;
    private final ExamPaperRepository examPaperRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final ExamCandidateAttemptsQueryRepository examCandidateAttemptsQueryRepository;
    private final UserContextPort userContextPort;

    public ViewMyExamsUseCase(
            ExamCandidateRepository examCandidateRepository,
            ExamRepository examRepository,
            ExamPaperRepository examPaperRepository,
            ExamScheduleRepository examScheduleRepository,
            ExamCandidateAttemptsQueryRepository examCandidateAttemptsQueryRepository,
            UserContextPort userContextPort) {
        this.examCandidateRepository = examCandidateRepository;
        this.examRepository = examRepository;
        this.examPaperRepository = examPaperRepository;
        this.examScheduleRepository = examScheduleRepository;
        this.examCandidateAttemptsQueryRepository = examCandidateAttemptsQueryRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    public List<StudentExamSummaryResponse> execute(Void input) {
        var now = Instant.now();
        var studentId = userContextPort.getCurrentAuthenticatedUserId();
        var candidates = examCandidateRepository.findByStudentId(studentId);
        var attemptsByCandidateId = groupAttemptsByCandidateId(candidates);
        var examsById = new HashMap<>(examRepository.findByIdIn(candidates.stream()
            .map(candidate -> candidate.getExamId())
            .distinct()
            .toList()).stream().collect(java.util.stream.Collectors.toMap(exam -> exam.getId(), exam -> exam)));
        var schedulesById = new HashMap<>(examScheduleRepository.findByIdIn(
            candidates.stream().map(candidate -> candidate.getScheduleId()).filter(java.util.Objects::nonNull).distinct().toList()
        ).stream().collect(java.util.stream.Collectors.toMap(schedule -> schedule.getId(), schedule -> schedule)));
        var papersById = new HashMap<>(examPaperRepository.findByIdIn(
            candidates.stream().map(candidate -> candidate.getAssignedPaperId()).filter(java.util.Objects::nonNull).distinct().toList()
        ).stream().collect(java.util.stream.Collectors.toMap(paper -> paper.getId(), paper -> paper)));

        return candidates.stream()
            .map(candidate -> {
                var exam = examsById.get(candidate.getExamId());
                var schedule = schedulesById.get(candidate.getScheduleId());
                var assignedPaper = papersById.get(candidate.getAssignedPaperId());
                if (exam == null) {
                    return null;
                }

                var attempts = attemptsByCandidateId.getOrDefault(candidate.getId(), List.of());
                var attemptsUsed = countAttemptsTowardLimit(attempts);
                var entryAvailability = resolveEntryAvailability(exam, candidate, schedule, now, attemptsUsed);
                var sessions = toSessionSummaries(attempts);

                return new StudentExamSummaryResponse(
                    exam.getId(),
                    exam.getName(),
                    StudentExamViewSupport.subjectOf(exam),
                    exam.getDescription(),
                    durationMinutesOf(assignedPaper == null ? null : assignedPaper.getTimeDurationSeconds(), schedule),
                    StudentExamViewSupport.examDateOf(schedule, exam.getOpenAt()),
                    StudentExamViewSupport.statusOf(exam, schedule, now),
                    exam.getKind() == null ? null : exam.getKind().name(),
                    exam.isRequiresOtp(),
                    sessions,
                    exam.getMaxAttempt(),
                    attemptsUsed,
                    entryAvailability.canEnter(),
                    entryAvailability.message()
                );
            })
            .filter(java.util.Objects::nonNull)
            .sorted(Comparator.comparing((StudentExamSummaryResponse response) -> response.examDate(), Comparator.nullsLast(Comparator.naturalOrder())))
            .toList();
    }

    private Map<java.util.UUID, List<ExamAttemptSummary>> groupAttemptsByCandidateId(List<ExamCandidate> candidates) {
        var candidateIds = candidates.stream()
            .map(candidate -> candidate.getId())
            .filter(Objects::nonNull)
            .toList();

        return examCandidateAttemptsQueryRepository.findByCandidateIds(candidateIds).stream()
            .collect(Collectors.groupingBy(summary -> summary.candidateId()));
    }

    private int countAttemptsTowardLimit(List<ExamAttemptSummary> attempts) {
        return (int) attempts.stream()
            .filter(this::countsTowardAttemptLimit)
            .count();
    }

    private boolean countsTowardAttemptLimit(ExamAttemptSummary attempt) {
        // Khớp đúng loại trừ của VerifyExamScheduleOtpUseCase.countUsedAttempts: session còn đang dở
        // (IN_PROGRESS/INTERRUPTED) là phiên có thể vào lại tiếp tục, không phải một lượt đã dùng --
        // thiếu điều kiện này khiến học sinh bị báo "hết lượt thi" ngay ở màn danh sách, trước khi
        // kịp vào màn OTP để vào lại đúng phiên đang dở.
        return attempt.status() != ExamSessionStatus.IN_PROGRESS
            && attempt.status() != ExamSessionStatus.INTERRUPTED
            && attempt.resultStatus() != ExamCandidateResultStatus.RETAKE_REQUIRED;
    }

    private List<StudentExamSessionSummaryResponse> toSessionSummaries(List<ExamAttemptSummary> attempts) {
        var orderedAttempts = attempts.stream()
            .sorted(Comparator.comparing((ExamAttemptSummary summary) -> summary.startedAt(), Comparator.nullsLast(Comparator.naturalOrder())))
            .toList();

        var out = new java.util.ArrayList<StudentExamSessionSummaryResponse>(orderedAttempts.size());
        for (int index = 0; index < orderedAttempts.size(); index++) {
            var attempt = orderedAttempts.get(index);
            out.add(new StudentExamSessionSummaryResponse(
                attempt.sessionId(),
                index + 1,
                attempt.status().name(),
                attempt.flagged()
            ));
        }
        return out;
    }

    private EntryAvailability resolveEntryAvailability(
            Exam exam,
            ExamCandidate candidate,
            ExamSchedule schedule,
            Instant now,
            int attemptsUsed) {
        if (candidate.getBlockedAt() != null) {
            return new EntryAvailability(false, "Bạn đã bị buộc kết thúc bài thi này, không thể vào lại");
        }

        if (ExamCandidateStatusSupport.isBlockedForEntry(candidate.getStatus())) {
            return new EntryAvailability(false, "Bạn không đủ điều kiện tham gia kỳ thi này");
        }

        if (exam.getMaxAttempt() != null && attemptsUsed >= exam.getMaxAttempt()) {
            return new EntryAvailability(false, "Bạn đã hết số lượt vào thi cho bài này.");
        }

        if (candidate.getAssignedPaperId() == null) {
            return new EntryAvailability(false, "Bạn chưa được gán đề thi.");
        }

        if (exam.getKind() == ExamKind.CLASS_TEST || !exam.isRequiresOtp()) {
            if (exam.getStatus() != ExamStatus.IN_PROGRESS) {
                return new EntryAvailability(false, "Bài kiểm tra chưa được giáo viên mở.");
            }
            return new EntryAvailability(true, null);
        }

        if (!ExamCandidateStatusSupport.isAttended(candidate.getStatus())) {
            return new EntryAvailability(false, "Bạn chưa được điểm danh có mặt, vui lòng liên hệ giám thị.");
        }

        if (exam.getStatus() != ExamStatus.IN_PROGRESS) {
            return new EntryAvailability(false, "Kỳ thi hiện chưa được mở để vào thi.");
        }

        if (candidate.getScheduleId() == null) {
            return new EntryAvailability(false, "Bạn chưa được xếp ca thi.");
        }

        if (schedule == null) {
            return new EntryAvailability(false, "Ca thi không hợp lệ hoac đã hết hạn.");
        }

        if (schedule.getStartDate() != null && now.isBefore(schedule.getStartDate())) {
            return new EntryAvailability(false, "Chưa đến giờ thi, vui lòng chờ đến khi ca thi bắt đầu.");
        }

        if (schedule.getEndDate() != null && !now.isBefore(schedule.getEndDate())) {
            return new EntryAvailability(false, "Ca thi đã hết thời gian vào thi.");
        }

        if (examScheduleRepository.findByIdAndInSchedule(schedule.getId(), now).isEmpty()) {
            return new EntryAvailability(false, "Ca thi không hợp lệ hoac đã hết hạn.");
        }

        return new EntryAvailability(true, null);
    }

    private record EntryAvailability(boolean canEnter, String message) {
    }

    private static int durationMinutesOf(Integer paperDurationSeconds, ExamSchedule schedule) {
        if (paperDurationSeconds != null && paperDurationSeconds > 0) {
            return Math.max(1, (int) Math.ceil(paperDurationSeconds / 60.0));
        }
        return StudentExamViewSupport.durationMinutesOf(schedule, 30);
    }
}
