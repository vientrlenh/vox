package com.sep.vox.application.port.input.usecase.assessmentschedule;

import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentSchedule;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.assessmentschedule.AddAssessmentScheduleProctorCommand;
import com.sep.vox.application.port.input.service.AssessmentScheduleManageAccessService;
import com.sep.vox.application.port.input.service.AssessmentScheduleProctorConflictValidator;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.dto.AssessmentScheduleProctorDto;
import com.sep.vox.domain.mapper.AssessmentScheduleProctorDtoMapper;
import com.sep.vox.domain.model.assessment.AssessmentScheduleProctor;
import com.sep.vox.domain.model.user.SchoolRoleCodes;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentScheduleProctorRepository;
import com.sep.vox.domain.repository.AssessmentScheduleRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.service.assessment.AssessmentEditingGuard;

@Service
@RequiredArgsConstructor
public class AddAssessmentScheduleProctorUseCase implements IUseCase<AddAssessmentScheduleProctorCommand, AssessmentScheduleProctorDto> {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentScheduleRepository assessmentScheduleRepository;
    private final AssessmentScheduleProctorRepository assessmentScheduleProctorRepository;
    private final AssessmentScheduleProctorConflictValidator assessmentScheduleProctorConflictValidator;
    private final AssessmentScheduleManageAccessService assessmentScheduleManageAccessService;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;

    @Override
    @Transactional
    public AssessmentScheduleProctorDto execute(AddAssessmentScheduleProctorCommand input) {
        AssessmentSchedule schedule = assessmentScheduleRepository.findById(input.scheduleId())
            .orElseThrow(() -> new ResourceNotFoundException("Assessment schedule not found"));
        if (!schedule.getAssessmentId().equals(input.assessmentId())) {
            throw new ResourceNotFoundException("Assessment schedule not found");
        }
        Assessment assessment = assessmentRepository.findById(schedule.getAssessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Assessment not found"));
        assessmentScheduleManageAccessService.requireCanManage(assessment);
        AssessmentEditingGuard.requireScheduleEditable(assessment);

        requireTeacherOfAssessmentSchool(assessment, input.teacherId());

        if (assessmentScheduleProctorRepository.existsByScheduleIdAndTeacherId(schedule.getId(), input.teacherId())) {
            throw new ResourceDuplicatedException("The proctor is already assigned to this assessment schedule");
        }

        // Exclude this session itself from the check: the duplicate check right above already covers that case,
        // here we only ask whether the teacher is busy in any other session (including other assessments).
        assessmentScheduleProctorConflictValidator.requireTeacherFree(
            input.teacherId(), schedule.getStartDate(), schedule.getEndDate(), schedule.getId());

        AssessmentScheduleProctor proctor = AssessmentScheduleProctor.assigned(schedule.getId(), input.teacherId());
        return AssessmentScheduleProctorDtoMapper.toDto(assessmentScheduleProctorRepository.save(proctor));
    }

    private void requireTeacherOfAssessmentSchool(Assessment assessment, UUID teacherId) {
        // The proctor must be a school_user with the TEACHER role in the same school as the assessment.
        if (!schoolUserRepository.existsBySchoolIdAndUserId(assessment.getSchoolId(), teacherId)) {
            throw new IllegalArgumentException("The teacher does not belong to the school of the assessment");
        }
        boolean isTeacher = userRoleQueryRepository.findByUserIdWithRoleInfo(teacherId).stream()
            .anyMatch(role -> SchoolRoleCodes.TEACHER.equals(role.roleCode()));
        if (!isTeacher) {
            throw new IllegalArgumentException("The user is not a teacher");
        }
    }
}
