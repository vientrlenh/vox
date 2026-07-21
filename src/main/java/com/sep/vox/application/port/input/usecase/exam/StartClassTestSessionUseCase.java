package com.sep.vox.application.port.input.usecase.exam;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sep.vox.application.common.ExamCandidateStatusSupport;
import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.CreateExamSessionCommand;
import com.sep.vox.application.port.input.command.StartClassTestSessionCommand;
import com.sep.vox.application.port.input.command.UpdateExamSessionStatusCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.input.usecase.examsession.CreateExamSessionUseCase;
import com.sep.vox.application.port.input.usecase.examsession.UpdateExamSessionStatusUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.exam.ExamEntryTicketResponse;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamSession;
import com.sep.vox.domain.model.exam.ExamSessionStatus;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamScheduleRepository;
import com.sep.vox.domain.repository.ExamSessionRepository;

@Service
public class StartClassTestSessionUseCase implements IUseCase<StartClassTestSessionCommand, ExamEntryTicketResponse> {

    private static final Duration ENTRY_TICKET_TTL = Duration.ofHours(2);

    private final ExamCandidateRepository examCandidateRepository;
    private final ExamCandidateResultRepository examCandidateResultRepository;
    private final ExamRepository examRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final ExamSessionRepository examSessionRepository;
    private final UserContextPort userContextPort;
    private final CreateExamSessionUseCase createExamSessionUseCase;
    private final UpdateExamSessionStatusUseCase updateExamSessionStatusUseCase;

    public StartClassTestSessionUseCase(
            ExamCandidateRepository examCandidateRepository,
            ExamCandidateResultRepository examCandidateResultRepository,
            ExamRepository examRepository,
            ExamScheduleRepository examScheduleRepository,
            ExamSessionRepository examSessionRepository,
            UserContextPort userContextPort,
            CreateExamSessionUseCase createExamSessionUseCase,
            UpdateExamSessionStatusUseCase updateExamSessionStatusUseCase) {
        this.examCandidateRepository = examCandidateRepository;
        this.examCandidateResultRepository = examCandidateResultRepository;
        this.examRepository = examRepository;
        this.examScheduleRepository = examScheduleRepository;
        this.examSessionRepository = examSessionRepository;
        this.userContextPort = userContextPort;
        this.createExamSessionUseCase = createExamSessionUseCase;
        this.updateExamSessionStatusUseCase = updateExamSessionStatusUseCase;
    }

    @Override
    public ExamEntryTicketResponse execute(StartClassTestSessionCommand input) {
        var studentId = userContextPort.getCurrentAuthenticatedUserId();
        var candidate = examCandidateRepository.findByExamIdAndStudentId(input.examId(), studentId)
            .orElseThrow(() -> new NotFoundException("Bạn không phải thí sinh của bài kiểm tra này"));

        var exam = examRepository.findById(input.examId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));
        if (exam.getKind() != ExamKind.CLASS_TEST && exam.isRequiresOtp()) {
            throw new IllegalStateException("Bài kiểm tra này yêu cầu xác thực OTP, vui lòng dùng luồng xác thực OTP");
        }

        var now = OffsetDateTime.now();
        if (candidate.getBlockedAt() != null) {
            throw new IllegalStateException("Bạn đã bị buộc kết thúc bài thi này, không thể vào lại");
        }
        if (ExamCandidateStatusSupport.isNonScorable(candidate.getStatus())) {
            throw new IllegalStateException("Bạn không đủ điều kiện tham gia kỳ thi này");
        }
        if (isExamClosedForEntry(exam, now)) {
            throw new IllegalStateException("Bài kiểm tra hiện không mở để làm bài (trạng thái: " + exam.getStatus() + ")");
        }
        if (candidate.getAssignedPaperId() == null) {
            throw new IllegalStateException("Bạn chưa được gán đề bài kiểm tra");
        }

        var scheduleEndAt = candidate.getScheduleId() == null
            ? exam.getCloseAt()
            : examScheduleRepository.findById(candidate.getScheduleId())
                .map(schedule -> schedule.getEndDate())
                .orElse(exam.getCloseAt());

        var resumableSession = findResumableSession(candidate.getId());
        if (resumableSession != null) {
            if (resumableSession.getStatus() == ExamSessionStatus.INTERRUPTED) {
                updateExamSessionStatusUseCase.execute(
                    new UpdateExamSessionStatusCommand(resumableSession.getId(), ExamSessionStatus.IN_PROGRESS)
                );
                resumableSession = examSessionRepository.findById(resumableSession.getId()).orElse(resumableSession);
            }
            return buildEntryTicket(resumableSession, now, scheduleEndAt);
        }

        if (exam.getMaxAttempt() != null) {
            var usedAttempts = countUsedAttempts(candidate.getId());
            if (usedAttempts >= exam.getMaxAttempt()) {
                throw new DuplicatedException("Đã hết số lượt làm bài cho phép (" + exam.getMaxAttempt() + " lượt)");
            }
        }

        var session = createExamSessionUseCase.execute(new CreateExamSessionCommand(
            input.examId(),
            candidate.getId(),
            candidate.getAssignedPaperId()
        ));
        return new ExamEntryTicketResponse(
            session.id(),
            UUID.randomUUID().toString(),
            now.plus(ENTRY_TICKET_TTL).toString(),
            scheduleEndAt == null ? null : scheduleEndAt.toString()
        );
    }

    private ExamSession findResumableSession(UUID candidateId) {
        return examSessionRepository.findLatestByCandidateIdAndStatuses(
            candidateId,
            EnumSet.of(ExamSessionStatus.IN_PROGRESS, ExamSessionStatus.INTERRUPTED)
        ).orElse(null);
    }

    private long countUsedAttempts(UUID candidateId) {
        return examSessionRepository.findAllByCandidateId(candidateId).stream()
            .filter(session -> session.getStatus() != ExamSessionStatus.IN_PROGRESS)
            .filter(session -> session.getStatus() != ExamSessionStatus.INTERRUPTED)
            .filter(session -> examCandidateResultRepository.findBySessionId(session.getId())
                .map(result -> result.getStatus() != com.sep.vox.domain.model.exam.ExamCandidateResultStatus.RETAKE_REQUIRED)
                .orElse(true))
            .count();
    }

    private boolean isExamClosedForEntry(com.sep.vox.domain.model.exam.Exam exam, OffsetDateTime now) {
        return exam.getStatus() != ExamStatus.IN_PROGRESS
            || exam.getStatus() == ExamStatus.CLOSED
            || exam.getStatus() == ExamStatus.CANCELLED
            || (exam.getCloseAt() != null && exam.getCloseAt().isBefore(now));
    }

    private ExamEntryTicketResponse buildEntryTicket(ExamSession session, OffsetDateTime now, OffsetDateTime scheduleEndAt) {
        return new ExamEntryTicketResponse(
            session.getId(),
            UUID.randomUUID().toString(),
            now.plus(ENTRY_TICKET_TTL).toString(),
            scheduleEndAt == null ? null : scheduleEndAt.toString()
        );
    }
}
