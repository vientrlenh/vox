package com.sep.vox.application.port.input.usecase.assessmentgrading;

import com.sep.vox.domain.model.assessment.AssessmentGradingAssignment;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.service.GradingAssignmentNotificationService;
import com.sep.vox.application.port.input.command.ReassignGradingCommand;
import com.sep.vox.application.port.input.service.AssessmentGradingAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.repository.AssessmentGradingQueryRepository;
import com.sep.vox.domain.model.assessment.GradingRoundType;
import com.sep.vox.domain.repository.AssessmentGradingAssignmentRepository;

/**
 * Changes the teacher who grades an attempt. It is an UPDATE of the existing row, not adding a second
 * row -- an attempt has only one grader.
 *
 * <p>The {@code APPEAL} round shares the conflict-of-interest rule with
 * {@code AssignAssessmentAppealReviewerUseCase}: a person who has ever written a score verdict for this
 * attempt must not sit and re-inspect themself. There is NO override here -- to make an exception
 * go through the appeal screen, the only place where the reason for the exception can be written onto the appeal.
 */
@Service
@RequiredArgsConstructor
public class ReassignGradingUseCase implements IUseCase<ReassignGradingCommand, UUID> {

    private final AssessmentGradingAssignmentRepository assessmentGradingAssignmentRepository;
    private final AssessmentGradingQueryRepository assessmentGradingQueryRepository;
    private final AssessmentGradingAccessService assessmentGradingAccessService;
    private final GradingAssignmentNotificationService gradingAssignmentNotificationService;

    @Override
    @Transactional
    public UUID execute(ReassignGradingCommand command) {
        UUID currentUserId = assessmentGradingAccessService.requireActiveUserId();
        // Khoá: nếu không, giáo viên nộp xong đúng lúc admin bấm đổi người thì bản ghi
        // đè lên sẽ mang ảnh chụp cũ (ASSIGNED, chưa có outcome) và làm bài đã chấm
        // sống lại thành chưa chấm.
        AssessmentGradingAccessService.GradingContext context = assessmentGradingAccessService.loadForUpdate(command.assignmentId());
        assessmentGradingAccessService.authorizeSchoolAdmin(context.schoolId(), currentUserId);

        AssessmentGradingAssignment assignment = context.assignment();
        if (assignment.isCompleted()) {
            throw new IllegalStateException("Không thể đổi giáo viên cho bài đã chấm xong.");
        }
        if (command.teacherId() == null) {
            throw new IllegalArgumentException("Thiếu giáo viên cần phân công.");
        }
        assessmentGradingAccessService.rejectClassTestCoordination(assignment.getCandidateResultId());
        if (!assessmentGradingAccessService.isTeacherOfSchool(command.teacherId(), context.schoolId())) {
            throw new IllegalArgumentException("Người chấm phải là giáo viên thuộc cùng trường với bài thi.");
        }
        if (assignment.getRoundType() == GradingRoundType.APPEAL) {
            Set<UUID> conflicted = assessmentGradingQueryRepository
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
        assessmentGradingAssignmentRepository.save(assignment);

        // Chỉ báo cho người MỚI. Người cũ mất việc là chuyện của điều phối viên, và một
        // thông báo "việc đã bị lấy đi" không giúp họ làm gì cả.
        gradingAssignmentNotificationService.publishAssigned(List.of(assignment), Instant.now());

        return assignment.getId();
    }
}
