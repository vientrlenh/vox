package com.sep.vox.application.port.input.usecase.exam;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sep.vox.application.common.CacheKey;
import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.CreateExamSessionCommand;
import com.sep.vox.application.port.input.command.UpdateExamSessionStatusCommand;
import com.sep.vox.application.port.input.command.VerifyExamScheduleOtpCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.input.usecase.examsession.CreateExamSessionUseCase;
import com.sep.vox.application.port.input.usecase.examsession.UpdateExamSessionStatusUseCase;
import com.sep.vox.application.port.input.service.SchoolSubscriptionDebtGuardService;
import com.sep.vox.application.port.output.CacheManagerPort;
import com.sep.vox.application.port.output.HealthCheckPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.exam.ExamEntryTicketResponse;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamCandidateStatus;
import com.sep.vox.domain.model.exam.ExamRequiredStreamType;
import com.sep.vox.domain.model.exam.ExamSession;
import com.sep.vox.domain.model.exam.ExamSessionStatus;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamScheduleRepository;
import com.sep.vox.domain.repository.ExamSessionRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;

@Service
public class VerifyExamScheduleOtpUseCase implements IUseCase<VerifyExamScheduleOtpCommand, ExamEntryTicketResponse> {

    private static final Duration ENTRY_TICKET_TTL = Duration.ofHours(2);

    private final ExamCandidateRepository examCandidateRepository;
    private final ExamCandidateResultRepository examCandidateResultRepository;
    private final ExamRepository examRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final ExamSessionRepository examSessionRepository;
    private final CacheManagerPort cacheManagerPort;
    private final UserContextPort userContextPort;
    private final CreateExamSessionUseCase createExamSessionUseCase;
    private final UpdateExamSessionStatusUseCase updateExamSessionStatusUseCase;
    private final HealthCheckPort healthCheckPort;
    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final SchoolSubscriptionDebtGuardService schoolSubscriptionDebtGuardService;

    public VerifyExamScheduleOtpUseCase(
            ExamCandidateRepository examCandidateRepository,
            ExamCandidateResultRepository examCandidateResultRepository,
            ExamRepository examRepository,
            ExamScheduleRepository examScheduleRepository,
            ExamSessionRepository examSessionRepository,
            CacheManagerPort cacheManagerPort,
            UserContextPort userContextPort,
            CreateExamSessionUseCase createExamSessionUseCase,
            UpdateExamSessionStatusUseCase updateExamSessionStatusUseCase, HealthCheckPort healthCheckPort,
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            SchoolSubscriptionDebtGuardService schoolSubscriptionDebtGuardService) {
        this.examCandidateRepository = examCandidateRepository;
        this.examCandidateResultRepository = examCandidateResultRepository;
        this.examRepository = examRepository;
        this.examScheduleRepository = examScheduleRepository;
        this.examSessionRepository = examSessionRepository;
        this.cacheManagerPort = cacheManagerPort;
        this.userContextPort = userContextPort;
        this.createExamSessionUseCase = createExamSessionUseCase;
        this.updateExamSessionStatusUseCase = updateExamSessionStatusUseCase;
        this.healthCheckPort = healthCheckPort;
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.schoolSubscriptionDebtGuardService = schoolSubscriptionDebtGuardService;
    }

    @Override
    public ExamEntryTicketResponse execute(VerifyExamScheduleOtpCommand input) {
        var studentId = userContextPort.getCurrentAuthenticatedUserId();
        var candidate = examCandidateRepository.findByExamIdAndStudentId(input.examId(), studentId)
            .orElseThrow(() -> new NotFoundException("Bạn không phải thí sinh của kỳ thi này"));

        var exam = examRepository.findById(input.examId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));

        // Chặn ngay ở cổng vào: vào thi được nhưng không ghi được dữ liệu giám sát thì bài thi coi
        // như bỏ. Không lọc theo kind — bài kiểm tra trên lớp cũng bật stream được.
        if (exam.getRequiredStreamType() != null) {
            healthCheckPort.checkStreamingOk();
        }

        if (!exam.isRequiresOtp()) {
            throw new IllegalStateException("Bài kiểm tra này không yêu cầu xác thực OTP, vui lòng dùng luồng bắt đầu trực tiếp");
        }

        var now = Instant.now();
        if (candidate.getBlockedAt() != null) {
            throw new IllegalStateException("Bạn đã bị buộc kết thúc bài thi này, không thể vào lại");
        }
        if (ExamCandidateStatus.isBlockedForEntry(candidate.getStatus())) {
            throw new IllegalStateException("Bạn không đủ điều kiện tham gia kỳ thi này");
        }
        if (!ExamCandidateStatus.isAttended(candidate.getStatus())) {
            throw new IllegalStateException("Bạn chưa được điểm danh có mặt, vui lòng liên hệ giám thị");
        }
        if (isExamClosedForEntry(exam, now)) {
            throw new IllegalStateException("Kỳ thi hiện không mở để thi (trạng thái: " + exam.getStatus() + ")");
        }

        if (candidate.getScheduleId() == null) {
            throw new IllegalStateException("Bạn chưa được xếp ca thi");
        }
        if (candidate.getAssignedPaperId() == null) {
            throw new IllegalStateException("Bạn chưa được gán đề thi");
        }

        var schedule = examScheduleRepository.findByIdAndInSchedule(candidate.getScheduleId(), now)
            .orElseThrow(() -> new NotFoundException("Ca thi không hợp lệ hoặc đã hết hạn"));

        if (!schedule.getExamId().equals(input.examId())) {
            throw new IllegalArgumentException("Ca thi không thuộc bài kiểm tra đã chọn");
        }

        var expectedOtp = cacheManagerPort.get(CacheKey.examScheduleOtpKey(candidate.getScheduleId()));
        if (expectedOtp == null || !expectedOtp.equals(input.otp())) {
            throw new UnauthorizedException("Mã OTP không đúng hoặc đã hết hạn");
        }

        var resumableSession = findResumableSession(candidate.getId());
        if (resumableSession != null) {
            if (resumableSession.getStatus() == ExamSessionStatus.INTERRUPTED) {
                updateExamSessionStatusUseCase.execute(
                    new UpdateExamSessionStatusCommand(resumableSession.getId(), ExamSessionStatus.IN_PROGRESS)
                );
                resumableSession = examSessionRepository.findById(resumableSession.getId()).orElse(resumableSession);
            }
            // Phiên đang dở: lựa chọn stream có thể đã bị chốt ở lần vào trước, và client phải biết
            // để không hiện lại màn chọn rồi ăn 403.
            return buildEntryTicket(
                resumableSession.getId(), now, schedule.getEndDate(), exam,
                resumableSession.getChosenStreamType());
        }

        if (exam.getMaxAttempt() != null) {
            var usedAttempts = countUsedAttempts(candidate.getId());
            if (usedAttempts >= exam.getMaxAttempt()) {
                throw new DuplicatedException("Đã hết số lượt thi cho phép (" + exam.getMaxAttempt() + " lượt)");
            }
        }

        // Chỉ chặn tạo phiên MỚI khi trường đang nợ -- không chặn nhánh resume ở trên để không cắt
        // ngang thí sinh đang thi dở. Không throw nếu trường không có subscription active (giữ
        // nguyên hành vi cũ cho case đó, ngoài phạm vi sửa lần này).
        schoolSubscriptionRepository.findActiveBySchoolId(exam.getSchoolId())
            .ifPresent(subscription -> schoolSubscriptionDebtGuardService.requireSchoolNotLocked(subscription.getSchoolId()));

        var session = createExamSessionUseCase.execute(new CreateExamSessionCommand(
            input.examId(),
            candidate.getId(),
            candidate.getAssignedPaperId()
        ));
        // Phiên vừa tạo: chưa phát token lần nào nên chưa chốt gì.
        return buildEntryTicket(session.id(), now, schedule.getEndDate(), exam, null);
    }

    private ExamSession findResumableSession(UUID candidateId) {
        return examSessionRepository.findLatestByCandidateIdAndStatuses(
            candidateId,
            ExamSessionStatus.RESUMABLE
        ).orElse(null);
    }

    private long countUsedAttempts(UUID candidateId) {
        return examSessionRepository.findByCandidateId(candidateId).stream()
            .filter(session -> session.getStatus() != ExamSessionStatus.IN_PROGRESS)
            .filter(session -> session.getStatus() != ExamSessionStatus.INTERRUPTED)
            .filter(session -> examCandidateResultRepository.findBySessionId(session.getId())
                .map(result -> result.getStatus() != com.sep.vox.domain.model.exam.ExamCandidateResultStatus.RETAKE_REQUIRED)
                .orElse(true))
            .count();
    }

    private boolean isExamClosedForEntry(Exam exam, Instant now) {
        return exam.getStatus() != ExamStatus.IN_PROGRESS
            || exam.getStatus() == ExamStatus.CLOSED
            || exam.getStatus() == ExamStatus.CANCELLED
            || (exam.getCloseAt() != null && exam.getCloseAt().isBefore(now));
    }

    private ExamEntryTicketResponse buildEntryTicket(
            UUID sessionId,
            Instant now,
            Instant scheduleEndAt,
            Exam exam,
            ExamRequiredStreamType chosenStreamType) {
        return ExamEntryTicketResponse.of(
            sessionId,
            UUID.randomUUID().toString(),
            now.plus(ENTRY_TICKET_TTL).toString(),
            scheduleEndAt,
            exam,
            chosenStreamType
        );
    }
}
