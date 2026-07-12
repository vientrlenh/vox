package com.sep.vox.application.port.input.usecase.exam;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.springframework.stereotype.Service;

import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.exam.StudentExamSummaryResponse;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamCandidate;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamSchedule;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamScheduleRepository;
import com.sep.vox.domain.repository.ExamSessionRepository;

@Service
public class ViewMyExamsUseCase implements IUseCase<Void, List<StudentExamSummaryResponse>> {

    private final ExamCandidateRepository examCandidateRepository;
    private final ExamRepository examRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final ExamSessionRepository examSessionRepository;
    private final UserContextPort userContextPort;

    public ViewMyExamsUseCase(
            ExamCandidateRepository examCandidateRepository,
            ExamRepository examRepository,
            ExamScheduleRepository examScheduleRepository,
            ExamSessionRepository examSessionRepository,
            UserContextPort userContextPort) {
        this.examCandidateRepository = examCandidateRepository;
        this.examRepository = examRepository;
        this.examScheduleRepository = examScheduleRepository;
        this.examSessionRepository = examSessionRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    public List<StudentExamSummaryResponse> execute(Void input) {
        var now = OffsetDateTime.now();
        var studentId = userContextPort.getCurrentAuthenticatedUserId();
        var candidates = examCandidateRepository.findByStudentId(studentId);
        var examsById = new HashMap<>(examRepository.findByIdIn(candidates.stream()
            .map(candidate -> candidate.getExamId())
            .distinct()
            .toList()).stream().collect(java.util.stream.Collectors.toMap(exam -> exam.getId(), exam -> exam)));
        var schedulesById = new HashMap<>(examScheduleRepository.findByIdIn(
            candidates.stream().map(candidate -> candidate.getScheduleId()).filter(java.util.Objects::nonNull).distinct().toList()
        ).stream().collect(java.util.stream.Collectors.toMap(schedule -> schedule.getId(), schedule -> schedule)));

        return candidates.stream()
            .map(candidate -> {
                var exam = examsById.get(candidate.getExamId());
                var schedule = schedulesById.get(candidate.getScheduleId());
                if (exam == null) {
                    return null;
                }

                var attemptsUsed = examSessionRepository.findAllByCandidateId(candidate.getId()).size();
                var entryAvailability = resolveEntryAvailability(exam, candidate, schedule, now, attemptsUsed);
                var latestSessionId = examSessionRepository
                    .findLatestByExamIdAndCandidateId(candidate.getExamId(), candidate.getId())
                    .map(session -> session.getId())
                    .orElse(null);

                return new StudentExamSummaryResponse(
                    exam.getId(),
                    exam.getName(),
                    StudentExamViewSupport.subjectOf(exam),
                    exam.getDescription(),
                    StudentExamViewSupport.durationMinutesOf(schedule, 30),
                    StudentExamViewSupport.examDateOf(schedule, exam.getOpenAt()),
                    StudentExamViewSupport.statusOf(exam, schedule, now),
                    exam.getKind() == null ? null : exam.getKind().name(),
                    latestSessionId,
                    exam.getMaxAttempt(),
                    attemptsUsed,
                    entryAvailability.canEnter(),
                    entryAvailability.message()
                );
            })
            .filter(java.util.Objects::nonNull)
            .sorted(Comparator.comparing(StudentExamSummaryResponse::examDate, Comparator.nullsLast(String::compareTo)))
            .toList();
    }

    private EntryAvailability resolveEntryAvailability(
            Exam exam,
            ExamCandidate candidate,
            ExamSchedule schedule,
            OffsetDateTime now,
            int attemptsUsed) {
        if (exam.getMaxAttempt() != null && attemptsUsed >= exam.getMaxAttempt()) {
            return new EntryAvailability(false, "Bạn đã hết số lượt vào thi cho bài này.");
        }

        if (candidate.getAssignedPaperId() == null) {
            return new EntryAvailability(false, "Bạn chưa được gán đề thi.");
        }

        if (exam.getKind() == ExamKind.CLASS_TEST) {
            if (exam.getStatus() != ExamStatus.IN_PROGRESS) {
                return new EntryAvailability(false, "Bài kiểm tra trên lớp chưa được giáo viên mở.");
            }
            return new EntryAvailability(true, null);
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
}
