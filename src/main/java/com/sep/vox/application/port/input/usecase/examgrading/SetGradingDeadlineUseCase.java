package com.sep.vox.application.port.input.usecase.examgrading;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.command.SetGradingDeadlineCommand;
import com.sep.vox.application.port.input.service.ExamGradingAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.repository.ExamGradingAssignmentRepository;

/**
 * Admin đặt hoặc sửa hạn chấm, một hay nhiều bài cùng lúc.
 *
 * <p>Sửa hạn thì {@code reminded_at} được xoá: hạn mới là một cam kết mới, và giáo
 * viên xứng đáng được nhắc lại theo hạn đó thay vì bị coi như đã nhắc rồi.
 */
@Service
public class SetGradingDeadlineUseCase implements IUseCase<SetGradingDeadlineCommand, List<UUID>> {

    private final ExamGradingAssignmentRepository examGradingAssignmentRepository;
    private final ExamGradingAccessService examGradingAccessService;

    public SetGradingDeadlineUseCase(
            ExamGradingAssignmentRepository examGradingAssignmentRepository,
            ExamGradingAccessService examGradingAccessService) {
        this.examGradingAssignmentRepository = examGradingAssignmentRepository;
        this.examGradingAccessService = examGradingAccessService;
    }

    @Override
    @Transactional
    public List<UUID> execute(SetGradingDeadlineCommand command) {
        var currentUserId = examGradingAccessService.requireActiveUserId();
        var assignmentIds = command.assignmentIds() == null ? List.<UUID>of() : command.assignmentIds();
        if (assignmentIds.isEmpty()) {
            throw new IllegalArgumentException("Phải chọn ít nhất một phân công để đặt hạn.");
        }
        if (command.deadlineAt() != null && command.deadlineAt().isBefore(OffsetDateTime.now())) {
            throw new IllegalArgumentException("Hạn chấm phải ở tương lai.");
        }

        // Validate hết rồi mới ghi, và phân quyền một lần cho mỗi trường phân biệt —
        // cùng khuôn với AssignGradingUseCase.
        var contexts = new ArrayList<ExamGradingAccessService.GradingContext>();
        for (var assignmentId : assignmentIds) {
            var context = examGradingAccessService.load(assignmentId);
            if (context.assignment().isCompleted()) {
                throw new IllegalStateException("Không thể đặt hạn cho phân công đã chốt.");
            }
            contexts.add(context);
        }
        contexts.stream()
            .map(context -> context.schoolId())
            .distinct()
            .forEach(schoolId -> examGradingAccessService.authorizeSchoolAdmin(schoolId, currentUserId));

        var updated = contexts.stream()
            .map(context -> {
                var assignment = context.assignment();
                assignment.setDeadlineAt(command.deadlineAt());
                assignment.setRemindedAt(null);
                return assignment;
            })
            .toList();
        return examGradingAssignmentRepository.saveAll(updated).stream()
            .map(assignment -> assignment.getId())
            .toList();
    }
}
