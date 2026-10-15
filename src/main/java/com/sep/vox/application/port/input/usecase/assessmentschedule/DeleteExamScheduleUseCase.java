package com.sep.vox.application.port.input.usecase.assessmentschedule;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.DeleteExamScheduleCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.projection.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.assessment.Exam;
import com.sep.vox.domain.model.assessment.ExamMemberRole;
import com.sep.vox.domain.model.assessment.ExamScheduleStatus;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.assessment.ExamCandidateRepository;
import com.sep.vox.domain.repository.assessment.ExamMemberRepository;
import com.sep.vox.domain.repository.assessment.ExamRepository;
import com.sep.vox.domain.repository.assessment.ExamScheduleProctorRepository;
import com.sep.vox.domain.repository.assessment.ExamScheduleRepository;

@Service
public class DeleteExamScheduleUseCase implements IUseCase<DeleteExamScheduleCommand, UUID> {

    private final ExamRepository examRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final ExamCandidateRepository examCandidateRepository;
    private final ExamScheduleProctorRepository examScheduleProctorRepository;
    private final ExamMemberRepository examMemberRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final UserContextPort userContextPort;

    public DeleteExamScheduleUseCase(
            ExamRepository examRepository,
            ExamScheduleRepository examScheduleRepository,
            ExamCandidateRepository examCandidateRepository,
            ExamScheduleProctorRepository examScheduleProctorRepository,
            ExamMemberRepository examMemberRepository,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository,
            UserContextPort userContextPort) {
        this.examRepository = examRepository;
        this.examScheduleRepository = examScheduleRepository;
        this.examCandidateRepository = examCandidateRepository;
        this.examScheduleProctorRepository = examScheduleProctorRepository;
        this.examMemberRepository = examMemberRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public UUID execute(DeleteExamScheduleCommand input) {
        var schedule = examScheduleRepository.findById(input.scheduleId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy ca thi"));
        if (!schedule.getExamId().equals(input.examId())) {
            throw new NotFoundException("Không tìm thấy ca thi");
        }
        var exam = examRepository.findById(schedule.getExamId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));
        var currentUserId = authorize(exam);

        if (examCandidateRepository.countByScheduleId(schedule.getId()) > 0) {
            throw new IllegalStateException("Không thể xoá ca thi đang có thí sinh");
        }
        // Ca thi bị xoá mềm nhưng dòng exam_schedule_proctors thì không có FK/cascade nào dọn hộ,
        // nên bắt buộc gỡ hết giám thị trước — nếu không màn điểm danh vẫn join ra ca đã xoá.
        if (examScheduleProctorRepository.countByScheduleId(schedule.getId()) > 0) {
            throw new IllegalStateException("Không thể xoá ca thi đang có giám thị");
        }
        if (exam.isLockedForEditing()) {
            throw new IllegalStateException("Không thể thay đổi lịch thi khi kỳ thi đã bắt đầu");
        }

        schedule.setStatus(ExamScheduleStatus.DELETED);
        schedule.setUpdatedAt(Instant.now());
        schedule.setUpdatedBy(currentUserId);
        examScheduleRepository.save(schedule);
        return schedule.getId();
    }

    private UUID authorize(Exam exam) {
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        var schoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));
        if (schoolAdmin && currentSchoolId != null && currentSchoolId.equals(exam.getSchoolId())) {
            return currentUserId;
        }
        if (examMemberRepository.existsByExamIdAndUserIdAndRole(exam.getId(), currentUserId, ExamMemberRole.CHAIR)) {
            return currentUserId;
        }
        throw new ForbiddenException("Quyền truy cập bị từ chối");
    }
}
