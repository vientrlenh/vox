package com.sep.vox.application.port.input.usecase.examgrading;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.command.ReassignGradingCommand;
import com.sep.vox.application.port.input.service.ExamGradingAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.repository.ExamGradingAssignmentRepository;

/**
 * Đổi giáo viên chấm một bài. Là UPDATE dòng sẵn có, không phải thêm dòng thứ
 * hai — một bài chỉ có một người chấm.
 */
@Service
public class ReassignGradingUseCase implements IUseCase<ReassignGradingCommand, UUID> {

    private final ExamGradingAssignmentRepository examGradingAssignmentRepository;
    private final ExamGradingAccessService examGradingAccessService;

    public ReassignGradingUseCase(
            ExamGradingAssignmentRepository examGradingAssignmentRepository,
            ExamGradingAccessService examGradingAccessService) {
        this.examGradingAssignmentRepository = examGradingAssignmentRepository;
        this.examGradingAccessService = examGradingAccessService;
    }

    @Override
    @Transactional
    public UUID execute(ReassignGradingCommand command) {
        var currentUserId = examGradingAccessService.requireActiveUserId();
        var context = examGradingAccessService.load(command.assignmentId());
        examGradingAccessService.authorizeSchoolAdmin(context.schoolId(), currentUserId);

        var assignment = context.assignment();
        if (assignment.isCompleted()) {
            throw new IllegalStateException("Không thể đổi giáo viên cho bài đã chấm xong.");
        }
        if (command.teacherId() == null) {
            throw new IllegalArgumentException("Thiếu giáo viên cần phân công.");
        }
        if (!examGradingAccessService.isTeacherOfSchool(command.teacherId(), context.schoolId())) {
            throw new IllegalArgumentException("Người chấm phải là giáo viên thuộc cùng trường với bài thi.");
        }

        // assignedAt chạy lại theo người mới: với người nhận, việc này bắt đầu từ
        // bây giờ, và hàng đợi của họ sắp theo mốc đó.
        assignment.setTeacherId(command.teacherId());
        assignment.setAssignedBy(currentUserId);
        assignment.setAssignedAt(OffsetDateTime.now());
        examGradingAssignmentRepository.save(assignment);
        return assignment.getId();
    }
}
