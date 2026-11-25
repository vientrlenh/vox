package com.sep.vox.application.port.input.usecase.assessment;

import com.sep.vox.application.response.input.assessmentsession.AssessmentSessionResponse;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentSchedule;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.assessmentsession.CreateAssessmentSessionCommand;
import com.sep.vox.application.port.input.command.assessment.StartClassTestSessionCommand;
import com.sep.vox.application.port.input.command.assessmentsession.UpdateAssessmentSessionStatusCommand;
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
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentScheduleRepository;
import com.sep.vox.domain.repository.AssessmentSessionRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;

@Service
@RequiredArgsConstructor
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

    @Override
    public AssessmentEntryTicketResponse execute(StartClassTestSessionCommand input) {
        UUID studentId = userContextPort.getCurrentAuthenticatedUserId();
        AssessmentCandidate candidate = assessmentCandidateRepository.findByAssessmentIdAndStudentId(input.assessmentId(), studentId)
            .orElseThrow(() -> new ResourceNotFoundException("Bạn không phải thí sinh của bài kiểm tra này"));

        Assessment assessment = assessmentRepository.findById(input.assessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài kiểm tra"));

        requireStreamingAndNoOtp(assessment);

        Instant now = Instant.now();
        requireCandidateMayEnter(candidate, assessment, now);

        Instant scheduleEndAt = requireScheduleEndInWindow(candidate, now);

        AssessmentSession resumableSession = findResumableSession(candidate.getId());
        if (resumableSession != null) {
            return resumeEntryTicket(resumableSession, now, scheduleEndAt, assessment);
        }

        requireAttemptsLeft(assessment, candidate);
        return startNewSession(input, candidate, assessment, now, scheduleEndAt);
    }

    private AssessmentEntryTicketResponse startNewSession(
            StartClassTestSessionCommand input,
            AssessmentCandidate candidate,
            Assessment assessment,
            Instant now,
            Instant scheduleEndAt) {
        // Only block creating a NEW session while the school is in debt -- the resume branch above is not blocked so that a
        // candidate who is halfway through is not cut off. Does not throw when the school has no active subscription (keeps
        // the old behaviour for that case, outside the scope of this change).
        schoolSubscriptionRepository.findActiveBySchoolId(assessment.getSchoolId())
            .ifPresent(subscription -> schoolSubscriptionDebtGuardService.requireSchoolNotLocked(subscription.getSchoolId()));

        AssessmentSessionResponse session = createAssessmentSessionUseCase.execute(new CreateAssessmentSessionCommand(
            input.assessmentId(),
            candidate.getId(),
            candidate.getAssignedPaperId()
        ));
        // The session was just created: no token has been issued yet, so nothing is fixed.
        return buildEntryTicket(session.id(), now, scheduleEndAt, assessment, null);
    }

    private Instant requireScheduleEndInWindow(AssessmentCandidate candidate, Instant now) {
        // Take the session that is inside its time window, with no fallback to the assessment's closeAt: this is exactly the
        // condition IssueStudentStreamTokenUseCase checks right after when it issues the stream token.
        AssessmentSchedule schedule = assessmentScheduleRepository.findByIdAndInSchedule(candidate.getScheduleId(), now)
            .orElseThrow(() -> new ResourceNotFoundException("Ca thi không hợp lệ hoặc đã hết hạn"));
        return schedule.getEndDate();
    }

    private void requireStreamingAndNoOtp(Assessment assessment) {
        // Blocked right at the entrance: being able to enter the attempt but not being able to record proctoring data makes the attempt
        // worthless. The same rule as the OTP flow in VerifyAssessmentScheduleOtpUseCase.
        if (assessment.getRequiredStreamType() != null) {
            healthCheckPort.checkStreamingOk();
        }

        if (assessment.isRequiresOtp()) {
            throw new IllegalArgumentException("Bài kiểm tra này yêu cầu xác thực OTP, vui lòng dùng luồng xác thực OTP");
        }
    }

    private void requireCandidateMayEnter(AssessmentCandidate candidate, Assessment assessment, Instant now) {
        if (candidate.getBlockedAt() != null) {
            throw new IllegalArgumentException("Bạn đã bị buộc kết thúc bài thi này, không thể vào lại");
        }
        if (AssessmentCandidateStatus.isBlockedForEntry(candidate.getStatus())) {
            throw new IllegalArgumentException("Bạn không đủ điều kiện tham gia kỳ thi này");
        }
        // A class test is also taken in a room with a proctor, so the candidate must be marked present before entering.
        if (!AssessmentCandidateStatus.isAttended(candidate.getStatus())) {
            throw new IllegalArgumentException("Bạn chưa được điểm danh có mặt, vui lòng liên hệ giám thị");
        }
        if (isAssessmentClosedForEntry(assessment, now)) {
            throw new IllegalArgumentException("Bài kiểm tra hiện không mở để làm bài (trạng thái: " + assessment.getStatus() + ")");
        }
        if (candidate.getScheduleId() == null) {
            throw new IllegalArgumentException("Bạn chưa được xếp ca thi");
        }
        if (candidate.getAssignedPaperId() == null) {
            throw new IllegalArgumentException("Bạn chưa được gán đề bài kiểm tra");
        }
    }

    private AssessmentEntryTicketResponse resumeEntryTicket(
            AssessmentSession resumableSession,
            Instant now,
            Instant scheduleEndAt,
            Assessment assessment) {
        if (resumableSession.getStatus() == AssessmentSessionStatus.INTERRUPTED) {
            updateAssessmentSessionStatusUseCase.execute(
                new UpdateAssessmentSessionStatusCommand(resumableSession.getId(), AssessmentSessionStatus.IN_PROGRESS)
            );
            resumableSession = assessmentSessionRepository.findById(resumableSession.getId()).orElse(resumableSession);
        }
        // A session in progress: the stream choice may already have been fixed on the earlier entry -- see the doc of
        // AssessmentEntryTicketResponse.of.
        return buildEntryTicket(
            resumableSession.getId(), now, scheduleEndAt, assessment,
            resumableSession.getChosenStreamType());
    }

    private void requireAttemptsLeft(Assessment assessment, AssessmentCandidate candidate) {
        if (assessment.getMaxAttempt() != null) {
            long usedAttempts = countUsedAttempts(candidate.getId());
            if (usedAttempts >= assessment.getMaxAttempt()) {
                throw new ResourceDuplicatedException("Đã hết số lượt làm bài cho phép (" + assessment.getMaxAttempt() + " lượt)");
            }
        }
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
