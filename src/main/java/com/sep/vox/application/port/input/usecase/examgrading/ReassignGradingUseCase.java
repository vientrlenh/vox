package com.sep.vox.application.port.input.usecase.examgrading;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.service.GradingAssignmentNotificationService;
import com.sep.vox.application.port.input.command.ReassignGradingCommand;
import com.sep.vox.application.port.input.service.ExamGradingAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.repository.ExamGradingQueryRepository;
import com.sep.vox.domain.model.exam.GradingRoundType;
import com.sep.vox.domain.repository.ExamGradingAssignmentRepository;

/**
 * Đổi giáo viên chấm một bài. Là UPDATE dòng sẵn có, không phải thêm dòng thứ
 * hai — một bài chỉ có một người chấm.
 *
 * <p>Vòng {@code APPEAL} chịu chung luật xung đột lợi ích với
 * {@code AssignExamAppealReviewerUseCase}: người đã từng ghi phán quyết điểm cho bài
 * này không được ngồi soi lại chính mình. Ở đây KHÔNG có cửa override — muốn phá lệ
 * thì đi qua màn đơn phúc khảo, chỗ duy nhất ghi được lý do ngoại lệ lên đơn.
 */
@Service
public class ReassignGradingUseCase implements IUseCase<ReassignGradingCommand, UUID> {

    private final ExamGradingAssignmentRepository examGradingAssignmentRepository;
    private final ExamGradingQueryRepository examGradingQueryRepository;
    private final ExamGradingAccessService examGradingAccessService;
    private final GradingAssignmentNotificationService gradingAssignmentNotificationService;

    public ReassignGradingUseCase(
            ExamGradingAssignmentRepository examGradingAssignmentRepository,
            ExamGradingQueryRepository examGradingQueryRepository,
            ExamGradingAccessService examGradingAccessService,
            GradingAssignmentNotificationService gradingAssignmentNotificationService) {
        this.examGradingAssignmentRepository = examGradingAssignmentRepository;
        this.examGradingQueryRepository = examGradingQueryRepository;
        this.examGradingAccessService = examGradingAccessService;
        this.gradingAssignmentNotificationService = gradingAssignmentNotificationService;
    }

    @Override
    @Transactional
    public UUID execute(ReassignGradingCommand command) {
        var currentUserId = examGradingAccessService.requireActiveUserId();
        // Khoá: nếu không, giáo viên nộp xong đúng lúc admin bấm đổi người thì bản ghi
        // đè lên sẽ mang ảnh chụp cũ (ASSIGNED, chưa có outcome) và làm bài đã chấm
        // sống lại thành chưa chấm.
        var context = examGradingAccessService.loadForUpdate(command.assignmentId());
        examGradingAccessService.authorizeSchoolAdmin(context.schoolId(), currentUserId);

        var assignment = context.assignment();
        if (assignment.isCompleted()) {
            throw new IllegalStateException("Không thể đổi giáo viên cho bài đã chấm xong.");
        }
        if (command.teacherId() == null) {
            throw new IllegalArgumentException("Thiếu giáo viên cần phân công.");
        }
        examGradingAccessService.rejectClassTestCoordination(assignment.getCandidateResultId());
        if (!examGradingAccessService.isTeacherOfSchool(command.teacherId(), context.schoolId())) {
            throw new IllegalArgumentException("Người chấm phải là giáo viên thuộc cùng trường với bài thi.");
        }
        if (assignment.getRoundType() == GradingRoundType.APPEAL) {
            var conflicted = examGradingQueryRepository
                .findTeacherIdsWithHumanEvaluation(assignment.getCandidateResultId());
            if (conflicted.contains(command.teacherId())) {
                throw new IllegalArgumentException(
                    "Giáo viên này đã từng chấm bài thi này nên không được chấm phúc khảo. "
                        + "Đổi người chấm ở màn đơn phúc khảo nếu cần ghi lý do ngoại lệ.");
            }
        }

        // assignedAt chạy lại theo người mới: với người nhận, việc này bắt đầu từ
        // bây giờ, và hàng đợi của họ sắp theo mốc đó.
        assignment.setTeacherId(command.teacherId());
        assignment.setAssignedBy(currentUserId);
        assignment.setAssignedAt(Instant.now());
        // Hạn cũ nhưng người mới: nếu không xoá dấu đã-nhắc thì findDueForReminder
        // (lọc reminded_at IS NULL) bỏ qua dòng này mãi mãi và người mới không bao giờ
        // nhận mail nhắc hạn. Cùng lý lẽ với SetGradingDeadlineUseCase khi đổi hạn.
        assignment.setRemindedAt(null);
        examGradingAssignmentRepository.save(assignment);

        // Chỉ báo cho người MỚI. Người cũ mất việc là chuyện của điều phối viên, và một
        // thông báo "việc đã bị lấy đi" không giúp họ làm gì cả.
        gradingAssignmentNotificationService.publishAssigned(List.of(assignment), Instant.now());

        return assignment.getId();
    }
}
