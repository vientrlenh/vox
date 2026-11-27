package com.sep.vox.application.port.input.usecase.assessmentschedule;

import com.sep.vox.domain.model.assessment.AssessmentSchedule;
import java.time.Instant;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.assessmentschedule.UpdateAssessmentScheduleCommand;
import com.sep.vox.application.port.input.service.AssessmentScheduleCandidateConflictValidator;
import com.sep.vox.application.port.input.service.AssessmentScheduleProctorConflictValidator;
import com.sep.vox.application.port.input.service.AssessmentScheduleRoomValidator;
import com.sep.vox.application.port.input.service.SubscriptionPeriodGuardService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentScheduleRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.service.assessment.AssessmentEditingGuard;
import com.sep.vox.domain.service.assessment.AssessmentScheduleWindowMessages;

@Service
@RequiredArgsConstructor
public class UpdateAssessmentScheduleUseCase implements IUseCase<UpdateAssessmentScheduleCommand, UUID> {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentScheduleRepository assessmentScheduleRepository;
    private final AssessmentScheduleRoomValidator assessmentScheduleRoomValidator;
    private final AssessmentScheduleProctorConflictValidator assessmentScheduleProctorConflictValidator;
    private final AssessmentScheduleCandidateConflictValidator assessmentScheduleCandidateConflictValidator;
    private final AssessmentMemberRepository assessmentMemberRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final SubscriptionPeriodGuardService subscriptionPeriodGuardService;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public UUID execute(UpdateAssessmentScheduleCommand input) {
        // Lock the schedule row first: the three overlap rules below all check the NEW window, so if another
        // thread is assigning people to this very schedule the two must run one after the other instead of
        // both reading the same stale state.
        AssessmentSchedule schedule = assessmentScheduleRepository.findByIdForUpdate(input.id())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ca thi"));
        Assessment assessment = assessmentRepository.findById(schedule.getAssessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài kiểm tra"));
        authorize(assessment);
        AssessmentEditingGuard.requireScheduleEditable(assessment);

        boolean isClassTest = assessment.getKind() == AssessmentKind.CLASS_TEST;
        requireEditableForKind(schedule, assessment, isClassTest);

        ScheduleWindow window = effectiveWindowOf(input, schedule);
        requireWindowValid(assessment, window, isClassTest);
        requireNoOverlapConflicts(input, schedule, assessment, window, isClassTest);
        persistUpdate(input, schedule, assessment, window, isClassTest);
        return schedule.getId();
    }

    /** Effective values: the new value when provided (a provided null room clears it), otherwise the current one. */
    private record ScheduleWindow(UUID roomId, Instant start, Instant end) {
    }

    private void requireEditableForKind(AssessmentSchedule schedule, Assessment assessment, boolean isClassTest) {
        if (isClassTest && assessment.getStatus() != AssessmentStatus.DRAFT && assessment.getStatus() != AssessmentStatus.SCHEDULED) {
            throw new IllegalArgumentException("Chỉ có thể sửa lịch bài kiểm tra trên lớp trước khi bắt đầu");
        }
        if (!isClassTest && !schedule.isModifiable()) {
            throw new IllegalArgumentException("Chỉ có thể sửa ca thi khi đang ở trạng thái nháp");
        }
    }

    private ScheduleWindow effectiveWindowOf(UpdateAssessmentScheduleCommand input, AssessmentSchedule schedule) {
        return new ScheduleWindow(
            input.schoolRoomIdProvided() ? input.schoolRoomId() : schedule.getSchoolRoomId(),
            input.startDate() != null ? input.startDate() : schedule.getStartDate(),
            input.endDate() != null ? input.endDate() : schedule.getEndDate());
    }

    private void requireWindowValid(Assessment assessment, ScheduleWindow window, boolean isClassTest) {
        if (!window.end().isAfter(window.start())) {
            throw new IllegalArgumentException("Thời gian kết thúc phải sau thời gian bắt đầu");
        }
        if (assessment.isScheduleWindowShorterThanAssessmentTime(window.start(), window.end())) {
            throw new IllegalArgumentException(AssessmentScheduleWindowMessages.tooShortForAssessmentTime(assessment));
        }
        // Only for a regular assessment: the schedule must lie inside the assessment's open/close window.
        // CLASS_TEST goes the other way -- openAt/closeAt are written back from the schedule below, so
        // comparing with the old window and blocking would make it impossible to move a class test.
        if (!isClassTest && assessment.isScheduleWindowOutsideAssessmentWindow(window.start(), window.end())) {
            throw new IllegalArgumentException(AssessmentScheduleWindowMessages.outsideAssessmentWindow(assessment));
        }
    }

    private void requireNoOverlapConflicts(
            UpdateAssessmentScheduleCommand input,
            AssessmentSchedule schedule,
            Assessment assessment,
            ScheduleWindow window,
            boolean isClassTest) {
        requireRoomUsable(input, schedule, assessment, window);
        requireParticipantsFree(schedule, assessment, window, isClassTest);
    }

    private void requireRoomUsable(
            UpdateAssessmentScheduleCommand input,
            AssessmentSchedule schedule,
            Assessment assessment,
            ScheduleWindow window) {
        if (input.schoolRoomId() != null) {
            assessmentScheduleRoomValidator.requireRoomOfAssessmentSchool(input.schoolRoomId(), assessment);
        }

        if (window.roomId() != null) {
            assessmentScheduleRoomValidator.requireNoOverlap(
                window.roomId(), window.start(), window.end(), schedule.getId());
        }
    }

    private void requireParticipantsFree(
            AssessmentSchedule schedule,
            Assessment assessment,
            ScheduleWindow window,
            boolean isClassTest) {
        // Moving a schedule must also check proctors, otherwise the "no proctoring two overlapping schedules"
        // rule is bypassed by assigning while the schedules do not touch and then dragging them on top of each other.
        assessmentScheduleProctorConflictValidator.requireProctorsFreeForNewWindow(
            schedule.getId(), window.start(), window.end());
        // Same reason on the candidate axis: assigning students while two schedules do not touch and then moving
        // them to overlap is the identical bypass.
        assessmentScheduleCandidateConflictValidator.requireCandidatesFreeForNewWindow(
            schedule.getId(), window.start(), window.end());

        // For a class test, the session IS the open/close window of the test (written back down to the assessment below),
        // so editing the session is another path that writes openAt/closeAt -- without checking here, changing the time of a
        // session would be a detour around the plan deadline constraint that Create/UpdateAssessmentUseCase apply.
        if (isClassTest) {
            subscriptionPeriodGuardService.requireWithinSubscriptionPeriod(
                assessment.getSchoolId(), window.start(), window.end());
        }
    }

    private void persistUpdate(
            UpdateAssessmentScheduleCommand input,
            AssessmentSchedule schedule,
            Assessment assessment,
            ScheduleWindow window,
            boolean isClassTest) {
        if (isClassTest) {
            schedule.setSchoolRoomId(window.roomId());
            schedule.setStartDate(window.start());
            schedule.setEndDate(window.end());
            assessmentScheduleRepository.save(schedule);
            assessment.setOpenAt(window.start());
            assessment.setCloseAt(window.end());
            assessmentRepository.save(assessment);
        } else {
            updateScheduleAtomically(input, schedule);
        }
    }

    private void updateScheduleAtomically(UpdateAssessmentScheduleCommand input, AssessmentSchedule schedule) {
        int updated = assessmentScheduleRepository.updateAtomic(
            schedule.getId(),
            input.schoolRoomId(),
            input.schoolRoomIdProvided(),
            input.startDate(),
            input.endDate());
        if (updated == 0) {
            throw new IllegalArgumentException("Không thể cập nhật ca thi (đã bị thay đổi trạng thái)");
        }
    }

    private void authorize(Assessment assessment) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        UUID currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        boolean schoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));
        if (schoolAdmin && currentSchoolId != null && currentSchoolId.equals(assessment.getSchoolId())) {
            return;
        }
        if (assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessment.getId(), currentUserId, AssessmentMemberRole.CHAIR)) {
            return;
        }
        throw new ForbiddenException("Quyền truy cập bị từ chối");
    }
}
