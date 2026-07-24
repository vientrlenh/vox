package com.sep.vox.application.port.input.usecase.examsession;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.ReviewFlaggedExamResultCommand;
import com.sep.vox.application.port.input.service.ExamSessionModerationAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamSessionRepository;

@Service
public class ReviewFlaggedExamResultUseCase implements IUseCase<ReviewFlaggedExamResultCommand, java.util.UUID> {

    private final ExamCandidateResultRepository examCandidateResultRepository;
    private final ExamSessionRepository examSessionRepository;
    private final ExamCandidateRepository examCandidateRepository;
    private final ExamRepository examRepository;
    private final ExamSessionModerationAccessService moderationAccessService;

    public ReviewFlaggedExamResultUseCase(
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
    public java.util.UUID execute(ReviewFlaggedExamResultCommand input) {
        var result = examCandidateResultRepository.findById(input.candidateResultId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy kết quả bài thi"));
        var session = examSessionRepository.findById(result.getSessionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên thi của kết quả này"));
        var candidate = examCandidateRepository.findById(session.getCandidateId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy thí sinh của kết quả này"));
        var exam = examRepository.findById(session.getExamId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy kỳ thi của kết quả này"));

        moderationAccessService.authorize(exam, candidate);
        if (!session.isFlagged()) {
            throw new IllegalStateException("Chỉ có thể duyệt kết quả của phiên thi đã bị đánh dấu");
        }
        if (result.getStatus() != ExamCandidateResultStatus.PENDING_REVIEW) {
            throw new IllegalStateException("Kết quả này không còn ở trạng thái chờ duyệt");
        }
        if (input.decision() != ExamCandidateResultStatus.RELEASED
                && input.decision() != ExamCandidateResultStatus.INVALID) {
            throw new IllegalArgumentException("Quyết định duyệt không hợp lệ");
        }

        var now = OffsetDateTime.now();
        result.setStatus(input.decision());
        result.setFinalizedAt(now);
        result.setUpdatedAt(now);
        result.setUpdatedBy(moderationAccessService.getCurrentUserId());
        examCandidateResultRepository.save(result);
        return result.getId();
    }
}
