package com.sep.vox.application.port.input.usecase.examappeal;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.RemoveExamAppealReviewerCommand;
import com.sep.vox.application.port.input.service.ExamAppealAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.model.exam.ExamAppealReviewerStatus;
import com.sep.vox.domain.model.exam.ExamAppealStatus;
import com.sep.vox.domain.repository.ExamAppealReviewerRepository;

@Service
public class RemoveExamAppealReviewerUseCase implements IUseCase<RemoveExamAppealReviewerCommand, UUID> {

    private final ExamAppealReviewerRepository examAppealReviewerRepository;
    private final ExamAppealAccessService examAppealAccessService;

    public RemoveExamAppealReviewerUseCase(
            ExamAppealReviewerRepository examAppealReviewerRepository,
            ExamAppealAccessService examAppealAccessService) {
        this.examAppealReviewerRepository = examAppealReviewerRepository;
        this.examAppealAccessService = examAppealAccessService;
    }

    @Override
    @Transactional
    public UUID execute(RemoveExamAppealReviewerCommand command) {
        var currentUserId = examAppealAccessService.requireActiveUserId();
        var context = examAppealAccessService.load(command.appealId());
        examAppealAccessService.authorizeSchoolAdmin(context, currentUserId);

        var appeal = context.appeal();
        if (appeal.getStatus() != ExamAppealStatus.GRADING) {
            throw new IllegalStateException("Chỉ có thể gỡ giám khảo khi đơn phúc khảo đang được chấm lại.");
        }

        var reviewer = examAppealReviewerRepository
            .findByAppealIdAndReviewerId(command.appealId(), command.reviewerId())
            .orElseThrow(() -> new NotFoundException("Giám khảo này chưa được phân công cho đơn phúc khảo."));
        if (reviewer.getStatus() == ExamAppealReviewerStatus.SUBMITTED) {
            throw new IllegalStateException("Không thể gỡ giám khảo đã nộp báo cáo chấm lại.");
        }

        var remaining = examAppealReviewerRepository.findByAppealId(command.appealId()).size() - 1;
        if (remaining < AssignExamAppealReviewersUseCase.MIN_REVIEWERS) {
            throw new IllegalStateException(
                "Đơn phúc khảo phải còn ít nhất " + AssignExamAppealReviewersUseCase.MIN_REVIEWERS + " giám khảo.");
        }

        examAppealReviewerRepository.deleteById(reviewer.getId());
        return appeal.getId();
    }
}
