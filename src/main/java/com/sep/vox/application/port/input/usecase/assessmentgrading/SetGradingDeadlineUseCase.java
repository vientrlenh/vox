package com.sep.vox.application.port.input.usecase.assessmentgrading;

import com.sep.vox.domain.model.assessment.AssessmentGradingAssignment;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.command.SetGradingDeadlineCommand;
import com.sep.vox.application.port.input.service.AssessmentGradingAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.repository.AssessmentGradingAssignmentRepository;

/**
 * Admin đặt hoặc sửa hạn chấm, một hay nhiều bài cùng lúc.
 *
 * <p>Sửa hạn thì {@code reminded_at} được xoá: hạn mới là một cam kết mới, và giáo
 * viên xứng đáng được nhắc lại theo hạn đó thay vì bị coi như đã nhắc rồi.
 */
@Service
@RequiredArgsConstructor
public class SetGradingDeadlineUseCase implements IUseCase<SetGradingDeadlineCommand, List<UUID>> {

    private final AssessmentGradingAssignmentRepository assessmentGradingAssignmentRepository;
    private final AssessmentGradingAccessService assessmentGradingAccessService;

    @Override
    @Transactional
    public List<UUID> execute(SetGradingDeadlineCommand command) {
        UUID currentUserId = assessmentGradingAccessService.requireActiveUserId();
        List<UUID> assignmentIds = command.assignmentIds() == null ? List.<UUID>of() : command.assignmentIds();
        if (assignmentIds.isEmpty()) {
            throw new IllegalArgumentException("Phải chọn ít nhất một phân công để đặt hạn.");
        }
        if (command.deadlineAt() != null && command.deadlineAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Hạn chấm phải ở tương lai.");
        }

        // Validate hết rồi mới ghi, và phân quyền một lần cho mỗi trường phân biệt —
        // cùng khuôn với AssignGradingUseCase.
        //
        // Khoá theo THỨ TỰ ID đã sắp, không theo thứ tự client gửi lên: đây là khoá
        // theo lô, nên hai admin gửi hai danh sách giao nhau theo thứ tự ngược nhau sẽ
        // khoá chéo và deadlock. Sắp trước cho mọi transaction đi cùng một chiều.
        // Thứ tự đầu vào không ảnh hưởng kết quả trả về nên sắp lại là vô hại.
        List<UUID> lockOrderedIds = assignmentIds.stream().sorted().toList();
        List<AssessmentGradingAccessService.GradingContext> contexts = new ArrayList<AssessmentGradingAccessService.GradingContext>();
        for (UUID assignmentId : lockOrderedIds) {
            AssessmentGradingAccessService.GradingContext context = assessmentGradingAccessService.loadForUpdate(assignmentId);
            if (context.assignment().isCompleted()) {
                throw new IllegalArgumentException("Không thể đặt hạn cho phân công đã chốt.");
            }
            contexts.add(context);
        }
        contexts.stream()
            .map(context -> context.schoolId())
            .distinct()
            .forEach(schoolId -> assessmentGradingAccessService.authorizeSchoolAdmin(schoolId, currentUserId));
        // Bài trên lớp không có hạn chấm hành chính — nhà trường không đặt hạn hộ giáo viên.
        assessmentGradingAccessService.rejectClassTestCoordination(
            contexts.stream().map(context -> context.candidateResult().getId()).toList());

        List<AssessmentGradingAssignment> updated = contexts.stream()
            .map(context -> {
                AssessmentGradingAssignment assignment = context.assignment();
                assignment.setDeadlineAt(command.deadlineAt());
                assignment.setRemindedAt(null);
                return assignment;
            })
            .toList();
        return assessmentGradingAssignmentRepository.saveAll(updated).stream()
            .map(assignment -> assignment.getId())
            .toList();
    }
}
