package com.sep.vox.application.port.input.usecase.examappeal;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.command.ApproveExamAppealCommand;
import com.sep.vox.application.port.input.command.AssignExamAppealReviewerCommand;
import com.sep.vox.application.port.input.service.ExamAppealAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;

/**
 * Duyệt đơn phúc khảo và tự nhận chấm trong MỘT thao tác.
 *
 * <p>Với bài kiểm tra trên lớp, người duyệt và người chấm luôn là cùng một người — chủ
 * bài. Tách làm hai bước (duyệt, rồi phân công) chỉ tạo thao tác thừa và để đơn kẹt ở
 * {@code APPROVED} khi giáo viên quên bước hai.
 *
 * <p>Đây là use case <em>compose</em>: nó gọi lại {@link ApproveExamAppealUseCase} và
 * {@link AssignExamAppealReviewerUseCase} thay vì nhồi cờ vào chúng — luồng của quản trị
 * trường vẫn cần đúng hai bước riêng (hỏi hạn chót, rồi chọn giám khảo). Hai use case con
 * là bean được inject nên chạy trong cùng transaction {@code REQUIRED}: bước giao chấm
 * hỏng thì bước duyệt rollback theo, đơn ở lại {@code PENDING} để thử lại — khác hẳn với
 * việc FE gọi hai API liên tiếp và để đơn kẹt giữa chừng.
 */
@Service
public class ApproveAndClaimExamAppealUseCase implements IUseCase<UUID, UUID> {

    /** Lý do cố định cho luật xung đột lợi ích: bài trên lớp chỉ chủ bài chấm được. */
    static final String SELF_CLAIM_OVERRIDE_REASON =
        "Bài kiểm tra trên lớp: giáo viên phụ trách bài tự nhận chấm phúc khảo.";
    static final int DEFAULT_DEADLINE_DAYS = 3;
    static final LocalTime DEFAULT_DEADLINE_TIME = LocalTime.of(17, 0);

    private final ApproveExamAppealUseCase approveExamAppealUseCase;
    private final AssignExamAppealReviewerUseCase assignExamAppealReviewerUseCase;
    private final ExamAppealAccessService examAppealAccessService;

    public ApproveAndClaimExamAppealUseCase(
            ApproveExamAppealUseCase approveExamAppealUseCase,
            AssignExamAppealReviewerUseCase assignExamAppealReviewerUseCase,
            ExamAppealAccessService examAppealAccessService) {
        this.approveExamAppealUseCase = approveExamAppealUseCase;
        this.assignExamAppealReviewerUseCase = assignExamAppealReviewerUseCase;
        this.examAppealAccessService = examAppealAccessService;
    }

    /** @return id DÒNG PHÂN CÔNG vừa mở (giống {@code /reviewer}), không phải id đơn. */
    @Override
    @Transactional
    public UUID execute(UUID appealId) {
        var currentUserId = examAppealAccessService.requireActiveUserId();
        var context = examAppealAccessService.load(appealId);
        // Chỉ chủ bài trên lớp tự nhận chấm được. Không chặn ở đây thì school admin sẽ
        // rơi xuống tận Assign và nhận thông báo "người chấm phải là giáo viên cùng
        // trường" — đọc không ra vấn đề thật.
        if (!examAppealAccessService.isClassTestChair(context, currentUserId)) {
            throw new ForbiddenException(
                "Chỉ giáo viên phụ trách bài mới tự nhận chấm được. "
                    + "Quản trị trường hãy duyệt rồi phân công người chấm.");
        }

        var deadline = defaultDeadline();
        approveExamAppealUseCase.execute(new ApproveExamAppealCommand(appealId, deadline));
        return assignExamAppealReviewerUseCase.execute(new AssignExamAppealReviewerCommand(
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
