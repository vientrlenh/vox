package com.sep.vox.application.port.input.usecase.exam;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sep.vox.application.common.CacheKey;
import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.CreateExamSessionCommand;
import com.sep.vox.application.port.input.command.VerifyExamScheduleOtpCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.input.usecase.examsession.CreateExamSessionUseCase;
import com.sep.vox.application.port.output.CacheManagerPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.exam.ExamEntryTicketResponse;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamScheduleRepository;
import com.sep.vox.domain.repository.ExamSessionRepository;

@Service
public class VerifyExamScheduleOtpUseCase implements IUseCase<VerifyExamScheduleOtpCommand, ExamEntryTicketResponse> {

    private static final Duration ENTRY_TICKET_TTL = Duration.ofHours(2);

    private final ExamCandidateRepository examCandidateRepository;
    private final ExamRepository examRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final ExamSessionRepository examSessionRepository;
    private final CacheManagerPort cacheManagerPort;
    private final UserContextPort userContextPort;
    private final CreateExamSessionUseCase createExamSessionUseCase;

    public VerifyExamScheduleOtpUseCase(
            ExamCandidateRepository examCandidateRepository,
            ExamRepository examRepository,
            ExamScheduleRepository examScheduleRepository,
            ExamSessionRepository examSessionRepository,
            CacheManagerPort cacheManagerPort,
            UserContextPort userContextPort,
            CreateExamSessionUseCase createExamSessionUseCase) {
        this.examCandidateRepository = examCandidateRepository;
        this.examRepository = examRepository;
        this.examScheduleRepository = examScheduleRepository;
        this.examSessionRepository = examSessionRepository;
        this.cacheManagerPort = cacheManagerPort;
        this.userContextPort = userContextPort;
        this.createExamSessionUseCase = createExamSessionUseCase;
    }

    @Override
    public ExamEntryTicketResponse execute(VerifyExamScheduleOtpCommand input) {
        var studentId = userContextPort.getCurrentAuthenticatedUserId();
        var candidate = examCandidateRepository.findByExamIdAndStudentId(input.examId(), studentId)
            .orElseThrow(() -> new NotFoundException("Bạn không phải thí sinh của kỳ thi này"));

        var exam = examRepository.findById(input.examId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));
        if (exam.getKind() == ExamKind.CLASS_TEST || !exam.isRequiresOtp()) {
            throw new IllegalStateException("Bài kiểm tra này không yêu cầu xác thực OTP, vui lòng dùng luồng bắt đầu trực tiếp");
        }
        if (exam.getStatus() != ExamStatus.IN_PROGRESS) {
            throw new IllegalStateException("Kỳ thi hiện không mở để thi (trạng thái: " + exam.getStatus() + ")");
        }

        if (candidate.getScheduleId() == null) {
            throw new IllegalStateException("Bạn chưa được xếp ca thi");
        }
        if (candidate.getAssignedPaperId() == null) {
            throw new IllegalStateException("Bạn chưa được gán đề thi");
        }

        var now = OffsetDateTime.now();
        var schedule = examScheduleRepository.findByIdAndInSchedule(candidate.getScheduleId(), now)
            .orElseThrow(() -> new NotFoundException("Ca thi không hợp lệ hoặc đã hết hạn"));

        if (!schedule.getExamId().equals(input.examId())) {
            throw new IllegalArgumentException("Ca thi không thuộc bài kiểm tra đã chọn");
        }

        var expectedOtp = cacheManagerPort.get(CacheKey.examScheduleOtpKey(candidate.getScheduleId()));
        if (expectedOtp == null || !expectedOtp.equals(input.otp())) {
            throw new UnauthorizedException("Mã OTP không đúng hoặc đã hết hạn");
        }

        if (exam.getMaxAttempt() != null) {
            var existingAttempts = examSessionRepository.findAllByCandidateId(candidate.getId());
            if (existingAttempts.size() >= exam.getMaxAttempt()) {
                throw new DuplicatedException("Đã hết số lượt thi cho phép (" + exam.getMaxAttempt() + " lượt)");
            }
        }

        var session = createExamSessionUseCase.execute(new CreateExamSessionCommand(
            input.examId(),
            candidate.getId(),
            candidate.getAssignedPaperId()
        ));
        var expiresAt = now.plus(ENTRY_TICKET_TTL);
        return new ExamEntryTicketResponse(
            session.id(),
            UUID.randomUUID().toString(),
            expiresAt.toString()
        );
    }
}
