package com.sep.vox.application.port.input.usecase.exam;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.CreateExamSessionCommand;
import com.sep.vox.application.port.input.command.StartClassTestSessionCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.input.usecase.examsession.CreateExamSessionUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.exam.ExamEntryTicketResponse;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamSessionRepository;

@Service
public class StartClassTestSessionUseCase implements IUseCase<StartClassTestSessionCommand, ExamEntryTicketResponse> {

    private static final Duration ENTRY_TICKET_TTL = Duration.ofHours(2);

    private final ExamCandidateRepository examCandidateRepository;
    private final ExamRepository examRepository;
    private final ExamSessionRepository examSessionRepository;
    private final UserContextPort userContextPort;
    private final CreateExamSessionUseCase createExamSessionUseCase;

    public StartClassTestSessionUseCase(
            ExamCandidateRepository examCandidateRepository,
            ExamRepository examRepository,
            ExamSessionRepository examSessionRepository,
            UserContextPort userContextPort,
            CreateExamSessionUseCase createExamSessionUseCase) {
        this.examCandidateRepository = examCandidateRepository;
        this.examRepository = examRepository;
        this.examSessionRepository = examSessionRepository;
        this.userContextPort = userContextPort;
        this.createExamSessionUseCase = createExamSessionUseCase;
    }

    @Override
    public ExamEntryTicketResponse execute(StartClassTestSessionCommand input) {
        var studentId = userContextPort.getCurrentAuthenticatedUserId();
        var candidate = examCandidateRepository.findByExamIdAndStudentId(input.examId(), studentId)
            .orElseThrow(() -> new NotFoundException("Bạn không phải thí sinh của bài kiểm tra này"));

        var exam = examRepository.findById(input.examId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));
        if (exam.getKind() != ExamKind.CLASS_TEST) {
            throw new IllegalStateException("Bài kiểm tra này không phải bài kiểm tra trên lớp, vui lòng dùng luồng xác thực OTP");
        }
        if (exam.getStatus() != ExamStatus.IN_PROGRESS) {
            throw new IllegalStateException("Bài kiểm tra hiện không mở để làm bài (trạng thái: " + exam.getStatus() + ")");
        }
        if (candidate.getAssignedPaperId() == null) {
            throw new IllegalStateException("Bạn chưa được gán đề bài kiểm tra");
        }

        if (exam.getMaxAttempt() != null) {
            var existingAttempts = examSessionRepository.findAllByCandidateId(candidate.getId());
            if (existingAttempts.size() >= exam.getMaxAttempt()) {
                throw new DuplicatedException("Đã hết số lượt làm bài cho phép (" + exam.getMaxAttempt() + " lượt)");
            }
        }

        var session = createExamSessionUseCase.execute(new CreateExamSessionCommand(
            input.examId(),
            candidate.getId(),
            candidate.getAssignedPaperId()
        ));
        var expiresAt = OffsetDateTime.now().plus(ENTRY_TICKET_TTL);
        return new ExamEntryTicketResponse(
            session.id(),
            UUID.randomUUID().toString(),
            expiresAt.toString()
        );
    }
}
