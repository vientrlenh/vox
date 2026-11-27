package com.sep.vox.application.port.input.usecase.assessmentschedule;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.query.assessmentschedule.ViewAssessmentSchedulesQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.AssessmentScheduleDto;
import com.sep.vox.domain.mapper.AssessmentScheduleDtoMapper;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.assessment.AssessmentSchedule;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentScheduleProctorRepository;
import com.sep.vox.domain.repository.AssessmentScheduleRepository;

@Service
@RequiredArgsConstructor
public class ViewAssessmentSchedulesUseCase implements IUseCase<ViewAssessmentSchedulesQuery, List<AssessmentScheduleDto>> {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentScheduleRepository assessmentScheduleRepository;
    private final AssessmentMemberRepository assessmentMemberRepository;
    private final AssessmentScheduleProctorRepository assessmentScheduleProctorRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional(readOnly = true)
    public List<AssessmentScheduleDto> execute(ViewAssessmentSchedulesQuery input) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        UUID currentSchoolId = userContextPort.getCurrentSchoolId();
        List<AssessmentSchedule> schedules;
        if (input.assessmentId() != null) {
            Assessment assessment = assessmentRepository.findById(input.assessmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Assessment not found"));
            schedules = resolveAssessmentSchedules(assessment, currentUserId, currentSchoolId);
        } else {
            schedules = assessmentScheduleRepository.findBySchoolId(authorizeSchoolWide());
        }

        List<AssessmentSchedule> filtered = schedules.stream()
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
     * The branch that lists sessions of the whole school (when no assessmentId is passed): only the SCHOOL_ADMIN of a school
     * is allowed; returns schoolId to limit the result to exactly the user's school.
     */
    private UUID authorizeSchoolWide() {
        UUID currentSchoolId = userContextPort.getCurrentSchoolId();
        boolean schoolAdmin = userContextPort.isSchoolAdmin();
        if (schoolAdmin && currentSchoolId != null) {
            return currentSchoolId;
        }
        throw new ForbiddenException("Access denied");
    }

    private List<AssessmentSchedule> resolveAssessmentSchedules(Assessment assessment, UUID currentUserId, UUID currentSchoolId) {
        List<AssessmentSchedule> schedules = assessmentScheduleRepository.findByAssessmentId(assessment.getId());
        if (userContextPort.isSchoolAdmin()
                && currentSchoolId != null
                && currentSchoolId.equals(assessment.getSchoolId())) {
            return schedules;
        }
        if (userContextPort.isTeacher()) {
            boolean isChair = assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessment.getId(), currentUserId, AssessmentMemberRole.CHAIR);
            if (isChair) {
                return schedules;
            }
            List<UUID> scheduleIds = schedules.stream().map(s -> s.getId()).toList();
            List<UUID> proctoredScheduleIds = assessmentScheduleProctorRepository.findScheduleIdsByTeacherIdAndScheduleIdIn(currentUserId, scheduleIds);
            return assessmentScheduleRepository.findByIdIn(proctoredScheduleIds);
        }
        throw new ForbiddenException("Access denied");
    }
}
