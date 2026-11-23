package com.sep.vox.application.port.input.usecase.assessmentschedule;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.assessmentschedule.CreateAssessmentScheduleCommand;
import com.sep.vox.application.port.input.service.AssessmentScheduleRoomValidator;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.dto.AssessmentScheduleDto;
import com.sep.vox.domain.mapper.AssessmentScheduleDtoMapper;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.assessment.AssessmentSchedule;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentScheduleRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.service.assessment.AssessmentEditingGuard;
import com.sep.vox.domain.service.assessment.AssessmentScheduleWindowMessages;

@Service
@RequiredArgsConstructor
public class CreateAssessmentScheduleUseCase implements IUseCase<CreateAssessmentScheduleCommand, AssessmentScheduleDto> {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentScheduleRepository assessmentScheduleRepository;
    private final AssessmentScheduleRoomValidator assessmentScheduleRoomValidator;
    private final AssessmentMemberRepository assessmentMemberRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public AssessmentScheduleDto execute(CreateAssessmentScheduleCommand input) {
        Assessment assessment = assessmentRepository.findById(input.assessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài kiểm tra"));
        authorize(assessment);
        AssessmentEditingGuard.requireScheduleEditable(assessment);

        if (input.startDate() == null || input.endDate() == null) {
            throw new IllegalArgumentException("Thời gian bắt đầu và kết thúc là bắt buộc");
        }
        if (!input.endDate().isAfter(input.startDate())) {
            throw new IllegalArgumentException("Thời gian kết thúc phải sau thời gian bắt đầu");
        }
        if (assessment.isScheduleWindowShorterThanAssessmentTime(input.startDate(), input.endDate())) {
            throw new IllegalArgumentException(AssessmentScheduleWindowMessages.tooShortForAssessmentTime(assessment));
        }
        if (assessment.isScheduleWindowOutsideAssessmentWindow(input.startDate(), input.endDate())) {
            throw new IllegalArgumentException(AssessmentScheduleWindowMessages.outsideAssessmentWindow(assessment));
        }

        assessmentScheduleRoomValidator.validateForNewSchedule(
            input.schoolRoomId(), assessment, input.startDate(), input.endDate());

        AssessmentSchedule schedule = AssessmentSchedule.createFresh(
            assessment.getId(), input.schoolRoomId(), input.startDate(), input.endDate());
        return AssessmentScheduleDtoMapper.toDto(assessmentScheduleRepository.save(schedule));
    }

    private UUID authorize(Assessment assessment) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        UUID currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        boolean schoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));
        if (schoolAdmin && currentSchoolId != null && currentSchoolId.equals(assessment.getSchoolId())) {
            return currentUserId;
        }
        if (assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessment.getId(), currentUserId, AssessmentMemberRole.CHAIR)) {
            return currentUserId;
        }
        throw new ForbiddenException("Quyền truy cập bị từ chối");
    }
}
