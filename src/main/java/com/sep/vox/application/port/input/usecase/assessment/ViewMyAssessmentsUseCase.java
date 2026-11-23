package com.sep.vox.application.port.input.usecase.assessment;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.assessment.ViewMyAssessmentsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.dto.AssessmentAttemptSummary;
import com.sep.vox.application.query.dto.StudentAssessmentRowInfo;
import com.sep.vox.application.query.repository.AssessmentCandidateAttemptsQueryRepository;
import com.sep.vox.application.query.repository.StudentAssessmentQueryRepository;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.assessment.StudentAssessmentSessionSummaryResponse;
import com.sep.vox.application.response.input.assessment.StudentAssessmentSummaryResponse;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.assessment.AssessmentCandidateStatus;
import com.sep.vox.domain.model.assessment.AssessmentPaper;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.repository.AssessmentPaperRepository;
import com.sep.vox.domain.repository.AssessmentScheduleRepository;
import com.sep.vox.domain.model.assessment.AssessmentSessionStatus;

/**
 * The student's assessment list.
 *
 * <p>Filtering, sorting and paging all live in SQL ({@link StudentAssessmentQueryRepository}). The previous version
 * loaded ALL candidates of that student, then every related assessment and schedule, joined/filtered/sorted in
 * memory and only then did {@code skip/limit} -- the cost of opening the screen was proportional to the student's lifetime number of assessments, even though
 * the screen only shows 20 rows.
 *
 * <p>What remains here is exactly two auxiliary reads, and both run only for EXACTLY the page being returned: the attempts
 * (to count the attempts used) and the assigned papers (to derive the duration).
 */
@Service
@RequiredArgsConstructor
public class ViewMyAssessmentsUseCase implements IUseCase<ViewMyAssessmentsQuery, PageResult<StudentAssessmentSummaryResponse>> {

    /** Default duration when neither the paper nor the schedule specifies one. */
    private static final int FALLBACK_DURATION_MINUTES = 30;

    private final StudentAssessmentQueryRepository studentAssessmentQueryRepository;
    private final AssessmentPaperRepository assessmentPaperRepository;
    private final AssessmentScheduleRepository assessmentScheduleRepository;
    private final AssessmentCandidateAttemptsQueryRepository assessmentCandidateAttemptsQueryRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional(readOnly = true)
    public PageResult<StudentAssessmentSummaryResponse> execute(ViewMyAssessmentsQuery input) {
        // MỘT mốc "bây giờ" cho cả câu đếm lẫn câu lấy dòng: lấy hai lần thì một bài đúng lúc
        // chuyển trạng thái có thể lọt vào câu này mà rơi khỏi câu kia.
        Instant now = Instant.now();
        UUID studentId = userContextPort.getCurrentAuthenticatedUserId();

        PageResult<StudentAssessmentRowInfo> page = studentAssessmentQueryRepository.findMyAssessments(
            studentId,
            input.kind() == null ? null : input.kind().name(),
            input.status(),
            input.sortDescending(),
            input.page(),
            input.size(),
            now);

        return new PageResult<>(
            toResponses(page.content(), now),
            page.page(),
            page.size(),
            page.totalElements(),
            page.totalPages());
    }

    private List<StudentAssessmentSummaryResponse> toResponses(List<StudentAssessmentRowInfo> rows, Instant now) {
        Map<UUID, List<AssessmentAttemptSummary>> attemptsByCandidateId = groupAttemptsByCandidateId(rows);
        Map<UUID, AssessmentPaper> papersById = papersByIds(rows);

        return rows.stream()
            .map(row -> {
                AssessmentPaper assignedPaper = papersById.get(row.assignedPaperId());
                List<AssessmentAttemptSummary> attempts = attemptsByCandidateId.getOrDefault(row.candidateId(), List.of());
                int attemptsUsed = countAttemptsTowardLimit(attempts);
                EntryAvailability entryAvailability = resolveEntryAvailability(row, now, attemptsUsed);

                return new StudentAssessmentSummaryResponse(
                    row.assessmentId(),
                    row.assessmentName(),
                    StudentAssessmentViewSupport.subjectOf(row.assessmentKind()),
                    row.assessmentDescription(),
                    durationMinutesOf(assignedPaper == null ? null : assignedPaper.getTimeDurationSeconds(), row),
                    row.assessmentDate() == null ? null : row.assessmentDate().toString(),
                    row.derivedStatus(),
                    row.assessmentKind(),
                    row.requiresOtp(),
                    toSessionSummaries(attempts),
                    row.maxAttempt(),
                    attemptsUsed,
                    entryAvailability.canEnter(),
                    entryAvailability.message()
                );
            })
            .toList();
    }

    private Map<UUID, AssessmentPaper> papersByIds(List<StudentAssessmentRowInfo> rows) {
        List<UUID> paperIds = rows.stream()
            .map(row -> row.assignedPaperId())
            .filter(Objects::nonNull)
            .distinct()
            .toList();

        return new HashMap<>(assessmentPaperRepository.findByIdIn(paperIds).stream()
            .collect(Collectors.toMap(paper -> paper.getId(), paper -> paper)));
    }

    private Map<UUID, List<AssessmentAttemptSummary>> groupAttemptsByCandidateId(List<StudentAssessmentRowInfo> rows) {
        List<UUID> candidateIds = rows.stream()
            .map(row -> row.candidateId())
            .filter(Objects::nonNull)
            .toList();

        return assessmentCandidateAttemptsQueryRepository.findByCandidateIds(candidateIds).stream()
            .collect(Collectors.groupingBy(summary -> summary.candidateId()));
    }

    private int countAttemptsTowardLimit(List<AssessmentAttemptSummary> attempts) {
        return (int) attempts.stream()
            .filter(this::countsTowardAttemptLimit)
            .count();
    }

    private boolean countsTowardAttemptLimit(AssessmentAttemptSummary attempt) {
        // Matches exactly the exclusion in VerifyAssessmentScheduleOtpUseCase.countUsedAttempts: a session still in progress
        // (IN_PROGRESS/INTERRUPTED) is a session that can be re-entered and continued, not an attempt already used --
        // without this condition the student is told "no attempts left" right on the list screen, before
        // they get to the OTP screen to re-enter the very session that is in progress.
        return attempt.status() != AssessmentSessionStatus.IN_PROGRESS
            && attempt.status() != AssessmentSessionStatus.INTERRUPTED
            && attempt.resultStatus() != AssessmentCandidateResultStatus.RETAKE_REQUIRED;
    }

    private List<StudentAssessmentSessionSummaryResponse> toSessionSummaries(List<AssessmentAttemptSummary> attempts) {
        List<AssessmentAttemptSummary> orderedAttempts = attempts.stream()
            .sorted(Comparator.comparing((AssessmentAttemptSummary summary) -> summary.startedAt(), Comparator.nullsLast(Comparator.naturalOrder())))
            .toList();

        List<StudentAssessmentSessionSummaryResponse> out = new java.util.ArrayList<StudentAssessmentSessionSummaryResponse>(orderedAttempts.size());
        for (int index = 0; index < orderedAttempts.size(); index++) {
            AssessmentAttemptSummary attempt = orderedAttempts.get(index);
            out.add(new StudentAssessmentSessionSummaryResponse(
                attempt.sessionId(),
                index + 1,
                attempt.status().name(),
                attempt.flagged()
            ));
        }
        return out;
    }

    /**
     * Hai nhánh "chưa xếp ca" / "ca không tồn tại" của bản cũ đã bỏ: câu truy vấn INNER JOIN sang
     * schedule và chỉ nhận ca học sinh được phép thấy, nên mọi dòng tới được đây đều đã có ca.
     */
    private EntryAvailability resolveEntryAvailability(StudentAssessmentRowInfo row, Instant now, int attemptsUsed) {
        if (row.blockedAt() != null) {
            return new EntryAvailability(false, "Bạn đã bị buộc kết thúc bài thi này, không thể vào lại");
        }

        if (AssessmentCandidateStatus.isBlockedForEntry(row.candidateStatus())) {
            return new EntryAvailability(false, "Bạn không đủ điều kiện tham gia kỳ thi này");
        }

        if (row.maxAttempt() != null && attemptsUsed >= row.maxAttempt()) {
            return new EntryAvailability(false, "Bạn đã hết số lượt vào thi cho bài này.");
        }

        if (row.assignedPaperId() == null) {
            return new EntryAvailability(false, "Bạn chưa được gán đề thi.");
        }

        // Không còn nhánh tắt riêng cho bài trên lớp: bài trên lớp giờ cũng thi trong phòng có
        // giám khảo và ca thi thật, nên đi chung đường kiểm tra với kỳ thi tập trung.
        if (!AssessmentCandidateStatus.isAttended(row.candidateStatus())) {
            return new EntryAvailability(false, "Bạn chưa được điểm danh có mặt, vui lòng liên hệ giám thị.");
        }

        if (row.assessmentStatus() != AssessmentStatus.IN_PROGRESS) {
            return new EntryAvailability(false, "Kỳ thi hiện chưa được mở để vào thi.");
        }

        if (row.scheduleStartDate() != null && now.isBefore(row.scheduleStartDate())) {
            return new EntryAvailability(false, "Chưa đến giờ thi, vui lòng chờ đến khi ca thi bắt đầu.");
        }

        if (row.scheduleEndDate() != null && !now.isBefore(row.scheduleEndDate())) {
            return new EntryAvailability(false, "Ca thi đã hết thời gian vào thi.");
        }

        if (assessmentScheduleRepository.findByIdAndInSchedule(row.scheduleId(), now).isEmpty()) {
            return new EntryAvailability(false, "Ca thi không hợp lệ hoac đã hết hạn.");
        }

        return new EntryAvailability(true, null);
    }

    private record EntryAvailability(boolean canEnter, String message) {
    }

    private static int durationMinutesOf(Integer paperDurationSeconds, StudentAssessmentRowInfo row) {
        if (paperDurationSeconds != null && paperDurationSeconds > 0) {
            return Math.max(1, (int) Math.ceil(paperDurationSeconds / 60.0));
        }
        return StudentAssessmentViewSupport.durationMinutesOf(
            row.scheduleStartDate(), row.scheduleEndDate(), FALLBACK_DURATION_MINUTES);
    }
}
