package com.sep.vox.application.port.input.usecase.examappeal;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.command.ApproveExamAppealCommand;
import com.sep.vox.application.port.input.service.ExamAppealAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.model.exam.ExamAppealStatus;
import com.sep.vox.domain.repository.ExamResultAppealRepository;

@Service
public class ApproveExamAppealUseCase implements IUseCase<ApproveExamAppealCommand, UUID> {

    private final ExamResultAppealRepository examResultAppealRepository;
    private final ExamAppealAccessService examAppealAccessService;

    public ApproveExamAppealUseCase(
            ExamResultAppealRepository examResultAppealRepository,
            ExamAppealAccessService examAppealAccessService) {
        this.examResultAppealRepository = examResultAppealRepository;
        this.examAppealAccessService = examAppealAccessService;
    }

    @Override
    @Transactional
    public UUID execute(ApproveExamAppealCommand command) {
        var currentUserId = examAppealAccessService.requireActiveUserId();
        var context = examAppealAccessService.load(command.appealId());
        examAppealAccessService.authorizeSchoolAdminOrClassTestChair(context, currentUserId);

        var appeal = context.appeal();
        if (appeal.getStatus() != ExamAppealStatus.PENDING) {
            throw new IllegalStateException("Chỉ có thể duyệt đơn phúc khảo đang ở trạng thái Chờ duyệt.");
        }

        var now = Instant.now();
        if (command.deadline() == null) {
            throw new IllegalArgumentException("Phải đặt hạn xử lý cho đơn phúc khảo.");
        }
        if (!command.deadline().isAfter(now)) {
            throw new IllegalArgumentException("Hạn xử lý phải ở tương lai.");
        }

        appeal.setStatus(ExamAppealStatus.APPROVED);
        appeal.setApprovedAt(now);
        appeal.setDeadline(command.deadline());
        examResultAppealRepository.save(appeal);

        return appeal.getId();
    }
}
