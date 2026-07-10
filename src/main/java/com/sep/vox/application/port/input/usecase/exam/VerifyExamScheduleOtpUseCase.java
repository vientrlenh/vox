package com.sep.vox.application.port.input.usecase.exam;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sep.vox.application.common.CacheKey;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.CreateExamSessionCommand;
import com.sep.vox.application.port.input.command.VerifyExamScheduleOtpCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.input.usecase.examsession.CreateExamSessionUseCase;
import com.sep.vox.application.port.output.CacheManagerPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.exam.ExamEntryTicketResponse;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamScheduleRepository;

@Service
public class VerifyExamScheduleOtpUseCase implements IUseCase<VerifyExamScheduleOtpCommand, ExamEntryTicketResponse> {

    private static final Duration ENTRY_TICKET_TTL = Duration.ofHours(2);

    private final ExamCandidateRepository examCandidateRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final CacheManagerPort cacheManagerPort;
    private final UserContextPort userContextPort;
    private final CreateExamSessionUseCase createExamSessionUseCase;

    public VerifyExamScheduleOtpUseCase(
            ExamCandidateRepository examCandidateRepository,
            ExamScheduleRepository examScheduleRepository,
            CacheManagerPort cacheManagerPort,
            UserContextPort userContextPort,
            CreateExamSessionUseCase createExamSessionUseCase) {
        this.examCandidateRepository = examCandidateRepository;
        this.examScheduleRepository = examScheduleRepository;
        this.cacheManagerPort = cacheManagerPort;
        this.userContextPort = userContextPort;
        this.createExamSessionUseCase = createExamSessionUseCase;
    }

    @Override
    public ExamEntryTicketResponse execute(VerifyExamScheduleOtpCommand input) {
        var studentId = userContextPort.getCurrentAuthenticatedUserId();
        var candidate = examCandidateRepository.findByExamIdAndStudentId(input.examId(), studentId)
            .orElseThrow(() -> new NotFoundException("Bạn không phải thí sinh của kỳ thi này"));

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
