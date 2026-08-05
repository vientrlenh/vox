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
import com.sep.vox.application.port.input.query.ViewMyExamsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.dto.ExamAttemptSummary;
import com.sep.vox.application.query.repository.ExamCandidateAttemptsQueryRepository;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.exam.StudentExamSessionSummaryResponse;
import com.sep.vox.application.response.input.exam.StudentExamSummaryResponse;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamCandidate;
import com.sep.vox.domain.model.exam.ExamPaper;
import com.sep.vox.domain.model.exam.ExamSchedule;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamPaperRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamScheduleRepository;
import com.sep.vox.domain.model.exam.ExamSessionStatus;

@Service
public class ViewMyExamsUseCase implements IUseCase<ViewMyExamsQuery, PageResult<StudentExamSummaryResponse>> {

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
    public PageResult<StudentExamSummaryResponse> execute(ViewMyExamsQuery input) {
        var now = Instant.now();
        var studentId = userContextPort.getCurrentAuthenticatedUserId();
        var candidates = examCandidateRepository.findByStudentId(studentId);
        var examsById = new HashMap<>(examRepository.findByIdIn(candidates.stream()
            .map(candidate -> candidate.getExamId())
            .distinct()
            .toList()).stream().collect(Collectors.toMap(exam -> exam.getId(), exam -> exam)));
        var schedulesById = new HashMap<>(examScheduleRepository.findByIdIn(
            candidates.stream().map(candidate -> candidate.getScheduleId()).filter(Objects::nonNull).distinct().toList()
        ).stream().collect(Collectors.toMap(schedule -> schedule.getId(), schedule -> schedule)));

        var rows = candidates.stream()
            .map(candidate -> toRow(candidate, examsById.get(candidate.getExamId()),
                schedulesById.get(candidate.getScheduleId()), now))
            .filter(Objects::nonNull)
            .filter(row -> input.kind() == null || row.exam().getKind() == input.kind())
            .filter(row -> input.status() == null || input.status().equals(row.derivedStatus()))
            .sorted(orderBy(input.sortDescending()))
            .toList();

        var pageRows = rows.stream()
            .skip((long) input.page() * input.size())
            .limit(input.size())
            .toList();
        var content = toResponses(pageRows, now);
        var totalPages = (int) Math.ceil(rows.size() / (double) input.size());

        return new PageResult<>(content, input.page(), input.size(), rows.size(), totalPages);
    }

    /**
     * Học sinh chỉ được thấy bài thi đã thực sự xếp lịch xong: kỳ thi còn DRAFT là giáo viên chưa
     * bấm SCHEDULE, còn thí sinh chưa có ca (hoặc ca chưa publish/đã dời/đã xoá) thì lịch của em đó
     * vẫn đang được sắp. CANCELLED vẫn giữ lại để học sinh biết kỳ thi đã bị huỷ.
     */
    private Row toRow(ExamCandidate candidate, Exam exam, ExamSchedule schedule, Instant now) {
        if (exam == null || exam.getStatus() == ExamStatus.DRAFT) {
            return null;
        }
        if (candidate.getScheduleId() == null || schedule == null || schedule.getStatus() == null
                || !schedule.getStatus().isVisibleToStudent()) {
            return null;
        }

        return new Row(
            candidate,
            exam,
            schedule,
            StudentExamViewSupport.examDateInstantOf(schedule, exam.getOpenAt()),
            StudentExamViewSupport.statusOf(exam, schedule, now)
        );
    }

    private static Comparator<Row> orderBy(boolean descending) {
        // Bài chưa có ngày thi luôn xếp cuối ở cả hai chiều -- đảo chiều mà kéo chúng lên đầu thì
        // danh sách mở ra toàn dòng trống.
        Comparator<Instant> byInstant = descending ? Comparator.reverseOrder() : Comparator.naturalOrder();
        return Comparator.comparing((Row row) -> row.examDate(), Comparator.nullsLast(byInstant));
    }

    /**
     * Lượt thi và đề chỉ cần cho đúng trang đang trả về, nên nạp sau khi đã lọc/phân trang thay vì
     * nạp cho toàn bộ bài thi của học sinh.
     */
    private List<StudentExamSummaryResponse> toResponses(List<Row> rows, Instant now) {
        var attemptsByCandidateId = groupAttemptsByCandidateId(rows.stream().map(row -> row.candidate()).toList());
        var papersById = new HashMap<>(examPaperRepository.findByIdIn(
            rows.stream().map(row -> row.candidate().getAssignedPaperId()).filter(Objects::nonNull).distinct().toList()
        ).stream().collect(Collectors.toMap(paper -> paper.getId(), paper -> paper)));

        return rows.stream()
            .map(row -> {
                var candidate = row.candidate();
                var exam = row.exam();
                var schedule = row.schedule();
                ExamPaper assignedPaper = papersById.get(candidate.getAssignedPaperId());
                var attempts = attemptsByCandidateId.getOrDefault(candidate.getId(), List.of());
                var attemptsUsed = countAttemptsTowardLimit(attempts);
                var entryAvailability = resolveEntryAvailability(exam, candidate, schedule, now, attemptsUsed);

                return new StudentExamSummaryResponse(
                    exam.getId(),
                    exam.getName(),
                    StudentExamViewSupport.subjectOf(exam),
                    exam.getDescription(),
                    durationMinutesOf(assignedPaper == null ? null : assignedPaper.getTimeDurationSeconds(), schedule),
                    StudentExamViewSupport.examDateOf(schedule, exam.getOpenAt()),
                    row.derivedStatus(),
                    exam.getKind() == null ? null : exam.getKind().name(),
                    exam.isRequiresOtp(),
                    toSessionSummaries(attempts),
                    exam.getMaxAttempt(),
                    attemptsUsed,
                    entryAvailability.canEnter(),
                    entryAvailability.message()
                );
            })
            .toList();
    }

    private record Row(
        ExamCandidate candidate,
        Exam exam,
        ExamSchedule schedule,
        Instant examDate,
        String derivedStatus) {
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

        // Không còn nhánh tắt riêng cho bài trên lớp: bài trên lớp giờ cũng thi trong phòng có
        // giám khảo và ca thi thật, nên đi chung đường kiểm tra với kỳ thi tập trung.
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
