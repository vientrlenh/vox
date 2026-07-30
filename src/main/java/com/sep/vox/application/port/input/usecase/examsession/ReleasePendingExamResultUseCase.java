package com.sep.vox.application.port.input.usecase.examsession;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.ReleasePendingExamResultCommand;
import com.sep.vox.application.port.input.service.ExamSessionModerationAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamSessionRepository;

/**
 * State diagram: PENDING_REVIEW chỉ có 1 lối ra là RELEASED - giáo viên xem lại (dù do AI
 * không tự tin hay do session bị đánh dấu nghi vấn), thấy ổn thì xác nhận. Nếu xác nhận ĐÚNG
 * là vi phạm thì dùng "Buộc kết thúc" (ForceEndExamSessionUseCase, tự chốt INVALID ngay,
 * không qua đây); nếu nghi ngờ lỗi hệ thống/muốn chấm lại thì đã có phúc khảo lo, không phát
 * minh thêm nhánh RETAKE_REQUIRED ở bước duyệt này.
 */
@Service
public class ReleasePendingExamResultUseCase implements IUseCase<ReleasePendingExamResultCommand, UUID> {

    private final ExamCandidateResultRepository examCandidateResultRepository;
    private final ExamSessionRepository examSessionRepository;
    private final ExamCandidateRepository examCandidateRepository;
    private final ExamRepository examRepository;
    private final ExamSessionModerationAccessService moderationAccessService;

    public ReleasePendingExamResultUseCase(
            ExamCandidateResultRepository examCandidateResultRepository,
            ExamSessionRepository examSessionRepository,
            ExamCandidateRepository examCandidateRepository,
            ExamRepository examRepository,
            ExamSessionModerationAccessService moderationAccessService) {
        this.examCandidateResultRepository = examCandidateResultRepository;
        this.examSessionRepository = examSessionRepository;
        this.examCandidateRepository = examCandidateRepository;
        this.examRepository = examRepository;
        this.moderationAccessService = moderationAccessService;
    }

    @Override
    @Transactional
    public UUID execute(ReleasePendingExamResultCommand input) {
        var result = examCandidateResultRepository.findBySessionId(input.sessionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy kết quả bài thi"));
        var session = examSessionRepository.findById(result.getSessionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên thi của kết quả này"));
        var candidate = examCandidateRepository.findById(session.getCandidateId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy thí sinh của kết quả này"));
        var exam = examRepository.findById(session.getExamId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy kỳ thi của kết quả này"));

        moderationAccessService.authorize(exam, candidate);
        if (result.getStatus() != ExamCandidateResultStatus.PENDING_REVIEW) {
            throw new IllegalStateException("Kết quả này không còn ở trạng thái chờ duyệt");
        }

        var now = Instant.now();
        result.setStatus(ExamCandidateResultStatus.RELEASED);
        result.setReleasedAt(now);
        result.setUpdatedAt(now);
        result.setUpdatedBy(moderationAccessService.getCurrentUserId());
        examCandidateResultRepository.save(result);
        return result.getId();
    }
}
