package com.sep.vox.application.port.input.usecase.assessmentschedule;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.query.ViewAssessmentSchedulesQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.AssessmentScheduleDto;
import com.sep.vox.domain.mapper.AssessmentScheduleDtoMapper;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.assessment.AssessmentSchedule;
import com.sep.vox.domain.repository.assessment.AssessmentMemberRepository;
import com.sep.vox.domain.repository.assessment.AssessmentRepository;
import com.sep.vox.domain.repository.assessment.AssessmentScheduleProctorRepository;
import com.sep.vox.domain.repository.assessment.AssessmentScheduleRepository;

@Service
public class ViewAssessmentSchedulesUseCase implements IUseCase<ViewAssessmentSchedulesQuery, List<AssessmentScheduleDto>> {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentScheduleRepository assessmentScheduleRepository;
    private final AssessmentMemberRepository assessmentMemberRepository;
    private final AssessmentScheduleProctorRepository assessmentScheduleProctorRepository;
    private final UserContextPort userContextPort;

    public ViewAssessmentSchedulesUseCase(
            AssessmentRepository assessmentRepository,
            AssessmentScheduleRepository assessmentScheduleRepository,
            AssessmentMemberRepository assessmentMemberRepository,
            AssessmentScheduleProctorRepository assessmentScheduleProctorRepository,
            UserContextPort userContextPort) {
        this.assessmentRepository = assessmentRepository;
        this.assessmentScheduleRepository = assessmentScheduleRepository;
        this.assessmentMemberRepository = assessmentMemberRepository;
        this.assessmentScheduleProctorRepository = assessmentScheduleProctorRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssessmentScheduleDto> execute(ViewAssessmentSchedulesQuery input) {
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentSchoolId = userContextPort.getCurrentSchoolId();
        List<AssessmentSchedule> schedules;
        if (input.assessmentId() != null) {
            var assessment = assessmentRepository.findById(input.assessmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài kiểm tra"));
            schedules = resolveAssessmentSchedules(assessment, currentUserId, currentSchoolId);
        } else {
            schedules = assessmentScheduleRepository.findBySchoolId(authorizeSchoolWide());
        }

        var filtered = schedules.stream()
            .filter(schedule -> input.status() == null
                || input.status() == schedule.getStatus())
            .filter(schedule -> input.startDate() == null
                || (schedule.getStartDate() != null && !schedule.getStartDate().isBefore(input.startDate())))
            .filter(schedule -> input.endDate() == null
                || (schedule.getStartDate() != null && !schedule.getStartDate().isAfter(input.endDate())))
            .toList();
        return AssessmentScheduleDtoMapper.toDtoList(filtered);
    }

    /**
     * Nhánh liệt kê ca thi toàn trường (khi không truyền assessmentId): chỉ SCHOOL_ADMIN của một trường
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

    private List<AssessmentSchedule> resolveAssessmentSchedules(Assessment assessment, UUID currentUserId, UUID currentSchoolId) {
        var schedules = assessmentScheduleRepository.findByAssessmentId(assessment.getId());
        if (userContextPort.isSchoolAdmin()
                && currentSchoolId != null
                && currentSchoolId.equals(assessment.getSchoolId())) {
            return schedules;
        }
        if (userContextPort.isTeacher()) {
            var isChair = assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessment.getId(), currentUserId, AssessmentMemberRole.CHAIR);
            if (isChair) {
                return schedules;
            }
            var scheduleIds = schedules.stream().map(s -> s.getId()).toList();
            var proctoredScheduleIds = assessmentScheduleProctorRepository.findScheduleIdsByTeacherIdAndScheduleIdIn(currentUserId, scheduleIds);
            return assessmentScheduleRepository.findByIdIn(proctoredScheduleIds);
        }
        throw new ForbiddenException("Quyền truy cập bị từ chối");
    }
}
