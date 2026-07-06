package com.sep.vox.application.port.input.usecase.examschedule;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateExamScheduleStatusCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.dto.ExamScheduleDto;
import com.sep.vox.domain.mapper.ExamScheduleDtoMapper;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.exam.ExamSchedule;
import com.sep.vox.domain.model.exam.ExamScheduleStatus;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamScheduleProctorRepository;
import com.sep.vox.domain.repository.ExamScheduleRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
public class UpdateExamScheduleStatusUseCase implements IUseCase<UpdateExamScheduleStatusCommand, ExamScheduleDto> {

    private final ExamRepository examRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final ExamScheduleProctorRepository examScheduleProctorRepository;
    private final ExamMemberRepository examMemberRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final UserContextPort userContextPort;

    public UpdateExamScheduleStatusUseCase(
            ExamRepository examRepository,
            ExamScheduleRepository examScheduleRepository,
            ExamScheduleProctorRepository examScheduleProctorRepository,
            ExamMemberRepository examMemberRepository,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository,
            UserContextPort userContextPort) {
        this.examRepository = examRepository;
        this.examScheduleRepository = examScheduleRepository;
        this.examScheduleProctorRepository = examScheduleProctorRepository;
        this.examMemberRepository = examMemberRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public ExamScheduleDto execute(UpdateExamScheduleStatusCommand input) {
        var schedule = examScheduleRepository.findById(input.scheduleId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy ca thi"));
        if (!schedule.getExamId().equals(input.examId())) {
            throw new NotFoundException("Không tìm thấy ca thi");
        }
        var exam = examRepository.findById(schedule.getExamId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));
        var currentUserId = authorize(exam);

        var action = input.action() == null ? "" : input.action().trim().toUpperCase();
        switch (action) {
            case "PUBLISH" -> publish(schedule);
            case "MOVE" -> move(schedule, input.targetScheduleId());
            case "COMPLETE" -> complete(schedule);
            case "CANCEL" -> cancel(schedule);
            default -> throw new IllegalArgumentException("Hành động không hợp lệ");
        }

        schedule.setUpdatedAt(OffsetDateTime.now());
        schedule.setUpdatedBy(currentUserId);
        return ExamScheduleDtoMapper.toDto(examScheduleRepository.save(schedule));
    }

    private void publish(ExamSchedule schedule) {
        if (schedule.getStatus() != ExamScheduleStatus.DRAFT) {
            throw new IllegalStateException("Chỉ có thể công bố ca thi ở trạng thái nháp");
        }
        if (examScheduleProctorRepository.countByScheduleId(schedule.getId()) < 1) {
            throw new IllegalStateException("Cần ít nhất 1 giám thị trước khi công bố ca thi");
        }
        schedule.setStatus(ExamScheduleStatus.PUBLISHED);
    }

    private void move(ExamSchedule schedule, UUID targetScheduleId) {
        if (schedule.getStatus() != ExamScheduleStatus.DRAFT && schedule.getStatus() != ExamScheduleStatus.PUBLISHED) {
            throw new IllegalStateException("Chỉ có thể chuyển ca thi ở trạng thái nháp hoặc đã công bố");
        }
        if (targetScheduleId == null) {
            throw new IllegalArgumentException("Ca thi đích là bắt buộc khi chuyển");
        }
        var target = examScheduleRepository.findById(targetScheduleId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy ca thi đích"));
        if (!target.getExamId().equals(schedule.getExamId())) {
            throw new IllegalStateException("Ca thi đích không thuộc cùng bài kiểm tra");
        }
        if (target.getStatus() != ExamScheduleStatus.DRAFT && target.getStatus() != ExamScheduleStatus.PUBLISHED) {
            throw new IllegalStateException("Ca thi đích không ở trạng thái hợp lệ");
        }
        schedule.setStatus(ExamScheduleStatus.MOVED);
        schedule.setMovedToScheduleId(targetScheduleId);
    }

    private void complete(ExamSchedule schedule) {
        if (schedule.getStatus() != ExamScheduleStatus.PUBLISHED) {
            throw new IllegalStateException("Chỉ có thể hoàn thành ca thi đã công bố");
        }
        schedule.setStatus(ExamScheduleStatus.COMPLETED);
    }

    private void cancel(ExamSchedule schedule) {
        if (schedule.getStatus() != ExamScheduleStatus.DRAFT && schedule.getStatus() != ExamScheduleStatus.PUBLISHED) {
            throw new IllegalStateException("Chỉ có thể huỷ ca thi ở trạng thái nháp hoặc đã công bố");
        }
        schedule.setStatus(ExamScheduleStatus.CANCELLED);
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
