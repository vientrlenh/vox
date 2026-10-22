package com.sep.vox.application.port.input.usecase.assessmentschedule;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.CreateAssessmentScheduleCommand;
import com.sep.vox.application.port.input.service.AssessmentScheduleRoomValidator;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.assessment.AssessmentScheduleDto;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.assessment.AssessmentSchedule;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.assessment.AssessmentMemberRepository;
import com.sep.vox.domain.repository.assessment.AssessmentRepository;
import com.sep.vox.domain.repository.assessment.AssessmentScheduleRepository;
import com.sep.vox.domain.service.assessment.AssessmentEditingGuard;
import com.sep.vox.domain.service.assessment.AssessmentScheduleWindowMessages;

@Service
public class CreateAssessmentScheduleUseCase implements IUseCase<CreateAssessmentScheduleCommand, AssessmentScheduleDto> {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentScheduleRepository assessmentScheduleRepository;
    private final AssessmentScheduleRoomValidator assessmentScheduleRoomValidator;
    private final AssessmentMemberRepository assessmentMemberRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserContextPort userContextPort;

    public CreateAssessmentScheduleUseCase(
            AssessmentRepository assessmentRepository,
            AssessmentScheduleRepository assessmentScheduleRepository,
            AssessmentScheduleRoomValidator assessmentScheduleRoomValidator,
            AssessmentMemberRepository assessmentMemberRepository,
            SchoolUserRepository schoolUserRepository,
            UserContextPort userContextPort) {
        this.assessmentRepository = assessmentRepository;
        this.assessmentScheduleRepository = assessmentScheduleRepository;
        this.assessmentScheduleRoomValidator = assessmentScheduleRoomValidator;
        this.assessmentMemberRepository = assessmentMemberRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public AssessmentScheduleDto execute(CreateAssessmentScheduleCommand input) {
        var assessment = assessmentRepository.findById(input.assessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài kiểm tra"));
        var currentUserId = authorize(assessment);
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

        var schedule = AssessmentSchedule.createFresh(
            assessment.getId(), input.schoolRoomId(), input.startDate(), input.endDate(),
            currentUserId, Instant.now());
        return AssessmentScheduleDto.toDto(assessmentScheduleRepository.save(schedule));
    }

    private UUID authorize(Assessment assessment) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId()
            .orElseThrow(() -> new UnauthorizedException("User is not logged in"));
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        var schoolAdmin = userContextPort.isSchoolAdmin();
        if (schoolAdmin && currentSchoolId != null && currentSchoolId.equals(assessment.getSchoolId())) {
            return currentUserId;
        }
        if (assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessment.getId(), currentUserId, AssessmentMemberRole.CHAIR)) {
            return currentUserId;
        }
        throw new ForbiddenException("Quyền truy cập bị từ chối");
    }
}
