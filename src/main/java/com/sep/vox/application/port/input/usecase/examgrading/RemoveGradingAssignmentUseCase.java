package com.sep.vox.application.port.input.usecase.examgrading;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.command.RemoveGradingAssignmentCommand;
import com.sep.vox.application.port.input.service.ExamGradingAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.repository.ExamGradingAssignmentRepository;

/**
 * Gỡ phân công = xoá dòng. Không có trạng thái "đã gỡ": bài quay lại đúng tình
 * trạng chưa gán, và unique index trên candidate_result_id lại trống để gán người
 * khác.
 */
@Service
public class RemoveGradingAssignmentUseCase implements IUseCase<RemoveGradingAssignmentCommand, UUID> {

    private final ExamGradingAssignmentRepository examGradingAssignmentRepository;
    private final ExamGradingAccessService examGradingAccessService;

    public RemoveGradingAssignmentUseCase(
            ExamGradingAssignmentRepository examGradingAssignmentRepository,
            ExamGradingAccessService examGradingAccessService) {
        this.examGradingAssignmentRepository = examGradingAssignmentRepository;
        this.examGradingAccessService = examGradingAccessService;
    }

    @Override
    @Transactional
    public UUID execute(RemoveGradingAssignmentCommand command) {
        var currentUserId = examGradingAccessService.requireActiveUserId();
        var context = examGradingAccessService.load(command.assignmentId());
        examGradingAccessService.authorizeSchoolAdmin(context.schoolId(), currentUserId);

        // Đã chấm xong thì điểm đã ghi và bài đã công bố — xoá dòng phân công lúc
        // này chỉ xoá mất vết ai chấm, không hoàn tác được gì.
        if (context.assignment().isCompleted()) {
            throw new IllegalStateException("Không thể gỡ phân công của bài đã chấm xong.");
        }

        examGradingAssignmentRepository.deleteById(command.assignmentId());
        return command.assignmentId();
    }
}
