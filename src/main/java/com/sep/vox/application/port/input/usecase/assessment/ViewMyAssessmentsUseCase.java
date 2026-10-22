package com.sep.vox.application.port.input.usecase.assessment;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.query.ViewMyAssessmentsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.projection.dto.AssessmentAttemptSummary;
import com.sep.vox.application.projection.dto.StudentAssessmentRowInfo;
import com.sep.vox.application.projection.repository.AssessmentCandidateAttemptsQueryRepository;
import com.sep.vox.application.projection.repository.StudentAssessmentQueryRepository;
import com.sep.vox.application.response.input.assessment.StudentAssessmentSessionSummaryResponse;
import com.sep.vox.application.response.input.assessment.StudentAssessmentSummaryResponse;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.assessment.AssessmentCandidateStatus;
import com.sep.vox.domain.model.assessment.AssessmentPaper;
import com.sep.vox.domain.model.assessment.AssessmentSessionStatus;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.repository.assessment.AssessmentPaperRepository;
import com.sep.vox.domain.repository.assessment.AssessmentScheduleRepository;
import com.sep.vox.domain.shared.PageResult;

/**
 * Danh sách bài thi của học sinh.
 *
 * <p>Lọc, sắp xếp và phân trang đều nằm trong SQL ({@link StudentAssessmentQueryRepository}). Bản trước
 * nạp TOÀN BỘ candidate của em đó, nạp tiếp mọi assessment và schedule liên quan, ghép/lọc/sắp trong bộ
 * nhớ rồi mới {@code skip/limit} -- chi phí một lần mở màn tỉ lệ với số bài thi cả đời của em, dù
 * màn hình chỉ hiện 20 dòng.
 *
 * <p>Còn lại ở đây đúng hai lần đọc phụ, và cả hai chỉ chạy cho ĐÚNG trang đang trả về: lượt thi
 * (để đếm số lượt đã dùng) và đề đã gán (để suy thời lượng).
 */
@Service
public class ViewMyAssessmentsUseCase implements IUseCase<ViewMyAssessmentsQuery, PageResult<StudentAssessmentSummaryResponse>> {

    /** Thời lượng mặc định khi cả đề lẫn ca thi đều không nói gì. */
    private static final int FALLBACK_DURATION_MINUTES = 30;

    private final StudentAssessmentQueryRepository studentAssessmentQueryRepository;
    private final AssessmentPaperRepository assessmentPaperRepository;
    private final AssessmentScheduleRepository assessmentScheduleRepository;
    private final AssessmentCandidateAttemptsQueryRepository assessmentCandidateAttemptsQueryRepository;
    private final UserContextPort userContextPort;

    public ViewMyAssessmentsUseCase(
            StudentAssessmentQueryRepository studentAssessmentQueryRepository,
            AssessmentPaperRepository assessmentPaperRepository,
            AssessmentScheduleRepository assessmentScheduleRepository,
            AssessmentCandidateAttemptsQueryRepository assessmentCandidateAttemptsQueryRepository,
            UserContextPort userContextPort) {
        this.studentAssessmentQueryRepository = studentAssessmentQueryRepository;
        this.assessmentPaperRepository = assessmentPaperRepository;
        this.assessmentScheduleRepository = assessmentScheduleRepository;
        this.assessmentCandidateAttemptsQueryRepository = assessmentCandidateAttemptsQueryRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<StudentAssessmentSummaryResponse> execute(ViewMyAssessmentsQuery input) {
        // MỘT mốc "bây giờ" cho cả câu đếm lẫn câu lấy dòng: lấy hai lần thì một bài đúng lúc
        // chuyển trạng thái có thể lọt vào câu này mà rơi khỏi câu kia.
        var now = Instant.now();
        UUID studentId = userContextPort.getCurrentAuthenticatedUserId()
            .orElseThrow(() -> new UnauthorizedException("User is not logged in"));

        var page = studentAssessmentQueryRepository.findMyAssessments(
            studentId,
            input.kind() == null ? null : input.kind().name(),
            input.status(),
            input.sortDescending(),
            input.pageRequest().repositoryPage(),
            input.pageRequest().size(),
            now);

        return new PageResult<>(
            toResponses(page.content(), now),
            page.page(),
            page.size(),
            page.totalElements(),
            page.totalPages());
    }

    private List<StudentAssessmentSummaryResponse> toResponses(List<StudentAssessmentRowInfo> rows, Instant now) {
        var attemptsByCandidateId = groupAttemptsByCandidateId(rows);
        var papersById = papersByIds(rows);

        return rows.stream()
            .map(row -> {
                AssessmentPaper assignedPaper = papersById.get(row.assignedPaperId());
                var attempts = attemptsByCandidateId.getOrDefault(row.candidateId(), List.of());
                var attemptsUsed = countAttemptsTowardLimit(attempts);
                var entryAvailability = resolveEntryAvailability(row, now, attemptsUsed);

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
        var paperIds = rows.stream()
            .map(row -> row.assignedPaperId())
            .filter(Objects::nonNull)
            .distinct()
            .toList();

        return new HashMap<>(assessmentPaperRepository.findByIdIn(paperIds).stream()
            .collect(Collectors.toMap(paper -> paper.getId(), paper -> paper)));
    }

    private Map<UUID, List<AssessmentAttemptSummary>> groupAttemptsByCandidateId(List<StudentAssessmentRowInfo> rows) {
        var candidateIds = rows.stream()
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
        // Khớp đúng loại trừ của VerifyAssessmentScheduleOtpUseCase.countUsedAttempts: session còn đang dở
        // (IN_PROGRESS/INTERRUPTED) là phiên có thể vào lại tiếp tục, không phải một lượt đã dùng --
        // thiếu điều kiện này khiến học sinh bị báo "hết lượt thi" ngay ở màn danh sách, trước khi
        // kịp vào màn OTP để vào lại đúng phiên đang dở.
        return attempt.status() != AssessmentSessionStatus.IN_PROGRESS
            && attempt.status() != AssessmentSessionStatus.INTERRUPTED
            && attempt.resultStatus() != AssessmentCandidateResultStatus.RETAKE_REQUIRED;
    }

    private List<StudentAssessmentSessionSummaryResponse> toSessionSummaries(List<AssessmentAttemptSummary> attempts) {
        var orderedAttempts = attempts.stream()
            .sorted(Comparator.comparing((AssessmentAttemptSummary summary) -> summary.startedAt(), Comparator.nullsLast(Comparator.naturalOrder())))
            .toList();

        var out = new java.util.ArrayList<StudentAssessmentSessionSummaryResponse>(orderedAttempts.size());
        for (int index = 0; index < orderedAttempts.size(); index++) {
            var attempt = orderedAttempts.get(index);
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
