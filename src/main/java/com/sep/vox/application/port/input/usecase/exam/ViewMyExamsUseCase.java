package com.sep.vox.application.port.input.usecase.exam;

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

import com.sep.vox.application.port.input.query.ViewMyExamsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.dto.ExamAttemptSummary;
import com.sep.vox.application.query.dto.StudentExamRowInfo;
import com.sep.vox.application.query.repository.ExamCandidateAttemptsQueryRepository;
import com.sep.vox.application.query.repository.StudentExamQueryRepository;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.exam.StudentExamSessionSummaryResponse;
import com.sep.vox.application.response.input.exam.StudentExamSummaryResponse;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;
import com.sep.vox.domain.model.exam.ExamCandidateStatus;
import com.sep.vox.domain.model.exam.ExamPaper;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.repository.ExamPaperRepository;
import com.sep.vox.domain.repository.ExamScheduleRepository;
import com.sep.vox.domain.model.exam.ExamSessionStatus;

/**
 * Danh sách bài thi của học sinh.
 *
 * <p>Lọc, sắp xếp và phân trang đều nằm trong SQL ({@link StudentExamQueryRepository}). Bản trước
 * nạp TOÀN BỘ candidate của em đó, nạp tiếp mọi exam và schedule liên quan, ghép/lọc/sắp trong bộ
 * nhớ rồi mới {@code skip/limit} -- chi phí một lần mở màn tỉ lệ với số bài thi cả đời của em, dù
 * màn hình chỉ hiện 20 dòng.
 *
 * <p>Còn lại ở đây đúng hai lần đọc phụ, và cả hai chỉ chạy cho ĐÚNG trang đang trả về: lượt thi
 * (để đếm số lượt đã dùng) và đề đã gán (để suy thời lượng).
 */
@Service
public class ViewMyExamsUseCase implements IUseCase<ViewMyExamsQuery, PageResult<StudentExamSummaryResponse>> {

    /** Thời lượng mặc định khi cả đề lẫn ca thi đều không nói gì. */
    private static final int FALLBACK_DURATION_MINUTES = 30;

    private final StudentExamQueryRepository studentExamQueryRepository;
    private final ExamPaperRepository examPaperRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final ExamCandidateAttemptsQueryRepository examCandidateAttemptsQueryRepository;
    private final UserContextPort userContextPort;

    public ViewMyExamsUseCase(
            StudentExamQueryRepository studentExamQueryRepository,
            ExamPaperRepository examPaperRepository,
            ExamScheduleRepository examScheduleRepository,
            ExamCandidateAttemptsQueryRepository examCandidateAttemptsQueryRepository,
            UserContextPort userContextPort) {
        this.studentExamQueryRepository = studentExamQueryRepository;
        this.examPaperRepository = examPaperRepository;
        this.examScheduleRepository = examScheduleRepository;
        this.examCandidateAttemptsQueryRepository = examCandidateAttemptsQueryRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<StudentExamSummaryResponse> execute(ViewMyExamsQuery input) {
        // MỘT mốc "bây giờ" cho cả câu đếm lẫn câu lấy dòng: lấy hai lần thì một bài đúng lúc
        // chuyển trạng thái có thể lọt vào câu này mà rơi khỏi câu kia.
        var now = Instant.now();
        var studentId = userContextPort.getCurrentAuthenticatedUserId();

        var page = studentExamQueryRepository.findMyExams(
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

    private List<StudentExamSummaryResponse> toResponses(List<StudentExamRowInfo> rows, Instant now) {
        var attemptsByCandidateId = groupAttemptsByCandidateId(rows);
        var papersById = papersByIds(rows);

        return rows.stream()
            .map(row -> {
                ExamPaper assignedPaper = papersById.get(row.assignedPaperId());
                var attempts = attemptsByCandidateId.getOrDefault(row.candidateId(), List.of());
                var attemptsUsed = countAttemptsTowardLimit(attempts);
                var entryAvailability = resolveEntryAvailability(row, now, attemptsUsed);

                return new StudentExamSummaryResponse(
                    row.examId(),
                    row.examName(),
                    StudentExamViewSupport.subjectOf(row.examKind()),
                    row.examDescription(),
                    durationMinutesOf(assignedPaper == null ? null : assignedPaper.getTimeDurationSeconds(), row),
                    row.examDate() == null ? null : row.examDate().toString(),
                    row.derivedStatus(),
                    row.examKind(),
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

    private Map<UUID, ExamPaper> papersByIds(List<StudentExamRowInfo> rows) {
        var paperIds = rows.stream()
            .map(row -> row.assignedPaperId())
            .filter(Objects::nonNull)
            .distinct()
            .toList();

        return new HashMap<>(examPaperRepository.findByIdIn(paperIds).stream()
            .collect(Collectors.toMap(paper -> paper.getId(), paper -> paper)));
    }

    private Map<UUID, List<ExamAttemptSummary>> groupAttemptsByCandidateId(List<StudentExamRowInfo> rows) {
        var candidateIds = rows.stream()
            .map(row -> row.candidateId())
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

    /**
     * Hai nhánh "chưa xếp ca" / "ca không tồn tại" của bản cũ đã bỏ: câu truy vấn INNER JOIN sang
     * schedule và chỉ nhận ca học sinh được phép thấy, nên mọi dòng tới được đây đều đã có ca.
     */
    private EntryAvailability resolveEntryAvailability(StudentExamRowInfo row, Instant now, int attemptsUsed) {
        if (row.blockedAt() != null) {
            return new EntryAvailability(false, "Bạn đã bị buộc kết thúc bài thi này, không thể vào lại");
        }

        if (ExamCandidateStatus.isBlockedForEntry(row.candidateStatus())) {
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
        if (!ExamCandidateStatus.isAttended(row.candidateStatus())) {
            return new EntryAvailability(false, "Bạn chưa được điểm danh có mặt, vui lòng liên hệ giám thị.");
        }

        if (row.examStatus() != ExamStatus.IN_PROGRESS) {
            return new EntryAvailability(false, "Kỳ thi hiện chưa được mở để vào thi.");
        }

        if (row.scheduleStartDate() != null && now.isBefore(row.scheduleStartDate())) {
            return new EntryAvailability(false, "Chưa đến giờ thi, vui lòng chờ đến khi ca thi bắt đầu.");
        }

        if (row.scheduleEndDate() != null && !now.isBefore(row.scheduleEndDate())) {
            return new EntryAvailability(false, "Ca thi đã hết thời gian vào thi.");
        }

        if (examScheduleRepository.findByIdAndInSchedule(row.scheduleId(), now).isEmpty()) {
            return new EntryAvailability(false, "Ca thi không hợp lệ hoac đã hết hạn.");
        }

        return new EntryAvailability(true, null);
    }

    private record EntryAvailability(boolean canEnter, String message) {
    }

    private static int durationMinutesOf(Integer paperDurationSeconds, StudentExamRowInfo row) {
        if (paperDurationSeconds != null && paperDurationSeconds > 0) {
            return Math.max(1, (int) Math.ceil(paperDurationSeconds / 60.0));
        }
        return StudentExamViewSupport.durationMinutesOf(
            row.scheduleStartDate(), row.scheduleEndDate(), FALLBACK_DURATION_MINUTES);
    }
}
