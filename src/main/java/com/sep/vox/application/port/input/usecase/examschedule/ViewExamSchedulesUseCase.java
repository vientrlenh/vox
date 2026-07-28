package com.sep.vox.application.port.input.usecase.examschedule;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.query.ViewExamSchedulesQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.ExamScheduleDto;
import com.sep.vox.domain.mapper.ExamScheduleDtoMapper;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.exam.ExamSchedule;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamScheduleProctorRepository;
import com.sep.vox.domain.repository.ExamScheduleRepository;

@Service
public class ViewExamSchedulesUseCase implements IUseCase<ViewExamSchedulesQuery, List<ExamScheduleDto>> {

    private final ExamRepository examRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final ExamMemberRepository examMemberRepository;
    private final ExamScheduleProctorRepository examScheduleProctorRepository;
    private final UserContextPort userContextPort;

    public ViewExamSchedulesUseCase(
            ExamRepository examRepository,
            ExamScheduleRepository examScheduleRepository,
            ExamMemberRepository examMemberRepository,
            ExamScheduleProctorRepository examScheduleProctorRepository,
            UserContextPort userContextPort) {
        this.examRepository = examRepository;
        this.examScheduleRepository = examScheduleRepository;
        this.examMemberRepository = examMemberRepository;
        this.examScheduleProctorRepository = examScheduleProctorRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExamScheduleDto> execute(ViewExamSchedulesQuery input) {
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentSchoolId = userContextPort.getCurrentSchoolId();
        List<ExamSchedule> schedules;
        if (input.examId() != null) {
            var exam = examRepository.findById(input.examId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));
            schedules = resolveExamSchedules(exam, currentUserId, currentSchoolId);
        } else {
            schedules = examScheduleRepository.findBySchoolId(authorizeSchoolWide());
        }

        var filtered = schedules.stream()
            .filter(schedule -> input.status() == null
                || input.status() == schedule.getStatus())
            .filter(schedule -> input.startDate() == null
                || (schedule.getStartDate() != null && !schedule.getStartDate().isBefore(input.startDate())))
            .filter(schedule -> input.endDate() == null
                || (schedule.getStartDate() != null && !schedule.getStartDate().isAfter(input.endDate())))
            .toList();
        return ExamScheduleDtoMapper.toDtoList(filtered);
    }

    /**
     * Nhánh liệt kê ca thi toàn trường (khi không truyền examId): chỉ SCHOOL_ADMIN của một trường
     * được phép; trả về schoolId để giới hạn kết quả theo đúng trường của người dùng.
     */
    private UUID authorizeSchoolWide() {
        var currentSchoolId = userContextPort.getCurrentSchoolId();
        var schoolAdmin = userContextPort.isSchoolAdmin();
        if (schoolAdmin && currentSchoolId != null) {
            return currentSchoolId;
        }
        throw new ForbiddenException("Quyền truy cập bị từ chối");
    }

    private List<ExamSchedule> resolveExamSchedules(Exam exam, UUID currentUserId, UUID currentSchoolId) {
        var schedules = examScheduleRepository.findByExamId(exam.getId());
        if (userContextPort.isSchoolAdmin()
                && currentSchoolId != null
                && currentSchoolId.equals(exam.getSchoolId())) {
            return schedules;
        }
        if (userContextPort.isTeacher()) {
            var isChair = examMemberRepository.existsByExamIdAndUserIdAndRole(exam.getId(), currentUserId, ExamMemberRole.CHAIR);
            if (isChair) {
                return schedules;
            }
            var scheduleIds = schedules.stream().map(s -> s.getId()).toList();
            var proctoredScheduleIds = examScheduleProctorRepository.findScheduleIdsByTeacherIdAndScheduleIdIn(currentUserId, scheduleIds);
            return examScheduleRepository.findByIdIn(proctoredScheduleIds);
        }
        throw new ForbiddenException("Quyền truy cập bị từ chối");
    }
}
