package com.sep.vox.application.port.input.usecase.assessment;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.CreateAssessmentSessionCommand;
import com.sep.vox.application.port.input.command.StartClassTestSessionCommand;
import com.sep.vox.application.port.input.command.UpdateAssessmentSessionStatusCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.input.usecase.assessmentsession.CreateAssessmentSessionUseCase;
import com.sep.vox.application.port.input.usecase.assessmentsession.UpdateAssessmentSessionStatusUseCase;
import com.sep.vox.application.port.input.service.SchoolSubscriptionDebtGuardService;
import com.sep.vox.application.port.output.HealthCheckPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.assessment.AssessmentEntryTicketResponse;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidateStatus;
import com.sep.vox.domain.model.assessment.AssessmentRequiredStreamType;
import com.sep.vox.domain.model.assessment.AssessmentSession;
import com.sep.vox.domain.model.assessment.AssessmentSessionStatus;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.assessment.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.assessment.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.assessment.AssessmentRepository;
import com.sep.vox.domain.repository.assessment.AssessmentScheduleRepository;
import com.sep.vox.domain.repository.assessment.AssessmentSessionRepository;

@Service
public class StartClassTestSessionUseCase implements IUseCase<StartClassTestSessionCommand, AssessmentEntryTicketResponse> {

    private static final Duration ENTRY_TICKET_TTL = Duration.ofHours(2);

    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentScheduleRepository assessmentScheduleRepository;
    private final AssessmentSessionRepository assessmentSessionRepository;
    private final UserContextPort userContextPort;
    private final CreateAssessmentSessionUseCase createAssessmentSessionUseCase;
    private final UpdateAssessmentSessionStatusUseCase updateAssessmentSessionStatusUseCase;
    private final HealthCheckPort healthCheckPort;
    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final SchoolSubscriptionDebtGuardService schoolSubscriptionDebtGuardService;

    public StartClassTestSessionUseCase(
            AssessmentCandidateRepository assessmentCandidateRepository,
            AssessmentCandidateResultRepository assessmentCandidateResultRepository,
            AssessmentRepository assessmentRepository,
            AssessmentScheduleRepository assessmentScheduleRepository,
            AssessmentSessionRepository assessmentSessionRepository,
            UserContextPort userContextPort,
            CreateAssessmentSessionUseCase createAssessmentSessionUseCase,
            UpdateAssessmentSessionStatusUseCase updateAssessmentSessionStatusUseCase,
            HealthCheckPort healthCheckPort,
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            SchoolSubscriptionDebtGuardService schoolSubscriptionDebtGuardService) {
        this.assessmentCandidateRepository = assessmentCandidateRepository;
        this.assessmentCandidateResultRepository = assessmentCandidateResultRepository;
        this.assessmentRepository = assessmentRepository;
        this.assessmentScheduleRepository = assessmentScheduleRepository;
        this.assessmentSessionRepository = assessmentSessionRepository;
        this.userContextPort = userContextPort;
        this.createAssessmentSessionUseCase = createAssessmentSessionUseCase;
        this.updateAssessmentSessionStatusUseCase = updateAssessmentSessionStatusUseCase;
        this.healthCheckPort = healthCheckPort;
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.schoolSubscriptionDebtGuardService = schoolSubscriptionDebtGuardService;
    }

    @Override
    public AssessmentEntryTicketResponse execute(StartClassTestSessionCommand input) {
        UUID studentId = userContextPort.getCurrentAuthenticatedUserId()
            .orElseThrow(() -> new UnauthorizedException("User is not logged in"));
        var candidate = assessmentCandidateRepository.findByAssessmentIdAndStudentId(input.assessmentId(), studentId)
            .orElseThrow(() -> new ResourceNotFoundException("Bạn không phải thí sinh của bài kiểm tra này"));

        var assessment = assessmentRepository.findById(input.assessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài kiểm tra"));

        // Chặn ngay ở cổng vào: vào làm bài được nhưng không ghi được dữ liệu giám sát thì bài làm
        // coi như bỏ. Cùng luật với luồng OTP ở VerifyAssessmentScheduleOtpUseCase.
        if (assessment.getRequiredStreamType() != null) {
            healthCheckPort.checkStreamingOk();
        }

        if (assessment.isRequiresOtp()) {
            throw new IllegalStateException("Bài kiểm tra này yêu cầu xác thực OTP, vui lòng dùng luồng xác thực OTP");
        }

        var now = Instant.now();
        if (candidate.getBlockedAt() != null) {
            throw new IllegalStateException("Bạn đã bị buộc kết thúc bài thi này, không thể vào lại");
        }
        if (AssessmentCandidateStatus.isBlockedForEntry(candidate.getStatus())) {
            throw new IllegalStateException("Bạn không đủ điều kiện tham gia kỳ thi này");
        }
        // Bài trên lớp cũng thi trong phòng có giám khảo, nên phải được điểm danh có mặt mới vào được.
        if (!AssessmentCandidateStatus.isAttended(candidate.getStatus())) {
            throw new IllegalStateException("Bạn chưa được điểm danh có mặt, vui lòng liên hệ giám thị");
        }
        if (isAssessmentClosedForEntry(assessment, now)) {
            throw new IllegalStateException("Bài kiểm tra hiện không mở để làm bài (trạng thái: " + assessment.getStatus() + ")");
        }
        if (candidate.getScheduleId() == null) {
            throw new IllegalStateException("Bạn chưa được xếp ca thi");
        }
        if (candidate.getAssignedPaperId() == null) {
            throw new IllegalStateException("Bạn chưa được gán đề bài kiểm tra");
        }

        // Lấy ca thi đang trong khung giờ, không fallback về closeAt của bài: đây đúng là điều kiện
        // mà IssueStudentStreamTokenUseCase kiểm tra ngay sau đó khi phát token stream.
        var schedule = assessmentScheduleRepository.findByIdAndInSchedule(candidate.getScheduleId(), now)
            .orElseThrow(() -> new ResourceNotFoundException("Ca thi không hợp lệ hoặc đã hết hạn"));
        var scheduleEndAt = schedule.getEndDate();

        var resumableSession = findResumableSession(candidate.getId());
        if (resumableSession != null) {
            if (resumableSession.getStatus() == AssessmentSessionStatus.INTERRUPTED) {
                updateAssessmentSessionStatusUseCase.execute(
                    new UpdateAssessmentSessionStatusCommand(resumableSession.getId(), AssessmentSessionStatus.IN_PROGRESS)
                );
                resumableSession = assessmentSessionRepository.findById(resumableSession.getId()).orElse(resumableSession);
            }
            // Phiên đang dở: lựa chọn stream có thể đã bị chốt ở lần vào trước -- xem doc của
            // AssessmentEntryTicketResponse.of.
            return buildEntryTicket(
                resumableSession.getId(), now, scheduleEndAt, assessment,
                resumableSession.getChosenStreamType());
        }

        if (assessment.getMaxAttempt() != null) {
            var usedAttempts = countUsedAttempts(candidate.getId());
            if (usedAttempts >= assessment.getMaxAttempt()) {
                throw new ResourceDuplicatedException("Đã hết số lượt làm bài cho phép (" + assessment.getMaxAttempt() + " lượt)");
            }
        }

        // Chỉ chặn tạo phiên MỚI khi trường đang nợ -- không chặn nhánh resume ở trên để không cắt
        // ngang thí sinh đang thi dở. Không throw nếu trường không có subscription active (giữ
        // nguyên hành vi cũ cho case đó, ngoài phạm vi sửa lần này).
        schoolSubscriptionRepository.findActiveBySchoolId(assessment.getSchoolId())
            .ifPresent(subscription -> schoolSubscriptionDebtGuardService.requireSchoolNotLocked(subscription.getSchoolId()));

        var session = createAssessmentSessionUseCase.execute(new CreateAssessmentSessionCommand(
            input.assessmentId(),
            candidate.getId(),
            candidate.getAssignedPaperId()
        ));
        // Phiên vừa tạo: chưa phát token lần nào nên chưa chốt gì.
        return buildEntryTicket(session.id(), now, scheduleEndAt, assessment, null);
    }

    private AssessmentSession findResumableSession(UUID candidateId) {
        return assessmentSessionRepository.findLatestByCandidateIdAndStatuses(
            candidateId,
            EnumSet.of(AssessmentSessionStatus.IN_PROGRESS, AssessmentSessionStatus.INTERRUPTED)
        ).orElse(null);
    }

    private long countUsedAttempts(UUID candidateId) {
        return assessmentSessionRepository.findByCandidateId(candidateId).stream()
            .filter(session -> session.getStatus() != AssessmentSessionStatus.IN_PROGRESS)
            .filter(session -> session.getStatus() != AssessmentSessionStatus.INTERRUPTED)
            .filter(session -> assessmentCandidateResultRepository.findBySessionId(session.getId())
                .map(result -> result.getStatus() != com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus.RETAKE_REQUIRED)
                .orElse(true))
            .count();
    }

    private boolean isAssessmentClosedForEntry(Assessment assessment, Instant now) {
        return assessment.getStatus() != AssessmentStatus.IN_PROGRESS
            || assessment.getStatus() == AssessmentStatus.CLOSED
            || assessment.getStatus() == AssessmentStatus.CANCELLED
            || (assessment.getCloseAt() != null && assessment.getCloseAt().isBefore(now));
    }

    private AssessmentEntryTicketResponse buildEntryTicket(
            UUID sessionId,
            Instant now,
            Instant scheduleEndAt,
            Assessment assessment,
            AssessmentRequiredStreamType chosenStreamType) {
        return AssessmentEntryTicketResponse.of(
            sessionId,
            UUID.randomUUID().toString(),
            now.plus(ENTRY_TICKET_TTL).toString(),
            scheduleEndAt,
            assessment,
            chosenStreamType
        );
    }
}
