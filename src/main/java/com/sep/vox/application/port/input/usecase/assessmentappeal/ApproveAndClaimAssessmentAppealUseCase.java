package com.sep.vox.application.port.input.usecase.assessmentappeal;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.command.ApproveAssessmentAppealCommand;
import com.sep.vox.application.port.input.command.AssignAssessmentAppealReviewerCommand;
import com.sep.vox.application.port.input.service.AssessmentAppealAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;

/**
 * Approves an appeal and claims the regrading in ONE operation.
 *
 * <p>For a class test, the approver and the grader are always the same person -- the owner of the
 * test. Splitting it into two steps (approve, then assign) only creates a redundant operation and leaves the appeal stuck at
 * {@code APPROVED} when the teacher forgets the second step.
 *
 * <p>This is a <em>compose</em> use case: it calls {@link ApproveAssessmentAppealUseCase} and
 * {@link AssignAssessmentAppealReviewerUseCase} again instead of stuffing flags into them -- the school administrator flow
 * still needs exactly two separate steps (ask for the deadline, then choose a grader). The two child use cases
 * are injected beans so they run in the same {@code REQUIRED} transaction: if the assignment step
 * fails the approval step rolls back with it and the appeal stays {@code PENDING} to be retried -- very different from
 * the FE calling two APIs in a row and leaving the appeal stuck in the middle.
 */
@Service
@RequiredArgsConstructor
public class ApproveAndClaimAssessmentAppealUseCase implements IUseCase<UUID, UUID> {

    /** Fixed reason for the conflict-of-interest rule: a class test can only be graded by its owner. */
    static final String SELF_CLAIM_OVERRIDE_REASON =
        "Bài kiểm tra trên lớp: giáo viên phụ trách bài tự nhận chấm phúc khảo.";
    static final int DEFAULT_DEADLINE_DAYS = 3;
    static final LocalTime DEFAULT_DEADLINE_TIME = LocalTime.of(17, 0);

    private final ApproveAssessmentAppealUseCase approveAssessmentAppealUseCase;
    private final AssignAssessmentAppealReviewerUseCase assignAssessmentAppealReviewerUseCase;
    private final AssessmentAppealAccessService assessmentAppealAccessService;

    /** @return id DÒNG PHÂN CÔNG vừa mở (giống {@code /reviewer}), không phải id đơn. */
    @Override
    @Transactional
    public UUID execute(UUID appealId) {
        UUID currentUserId = assessmentAppealAccessService.requireActiveUserId();
        AssessmentAppealAccessService.AppealContext context = assessmentAppealAccessService.load(appealId);
        // Chỉ chủ bài trên lớp tự nhận chấm được. Không chặn ở đây thì school admin sẽ
        // rơi xuống tận Assign và nhận thông báo "người chấm phải là giáo viên cùng
        // trường" — đọc không ra vấn đề thật.
        if (!assessmentAppealAccessService.isClassTestChair(context, currentUserId)) {
            throw new ForbiddenException(
                "Chỉ giáo viên phụ trách bài mới tự nhận chấm được. "
                    + "Quản trị trường hãy duyệt rồi phân công người chấm.");
        }

        Instant deadline = defaultDeadline();
        approveAssessmentAppealUseCase.execute(new ApproveAssessmentAppealCommand(appealId, deadline));
        return assignAssessmentAppealReviewerUseCase.execute(new AssignAssessmentAppealReviewerCommand(
            appealId, currentUserId, SELF_CLAIM_OVERRIDE_REASON, deadline));
    }

    /** Cùng mặc định với hộp thoại duyệt cũ: T+3 lúc 17:00 giờ Việt Nam. */
    private Instant defaultDeadline() {
        return LocalDate.now(DateMapper.DEFAULT_INPUT_ZONE)
            .plusDays(DEFAULT_DEADLINE_DAYS)
            .atTime(DEFAULT_DEADLINE_TIME)
            .atZone(DateMapper.DEFAULT_INPUT_ZONE)
            .toInstant();
    }
}
