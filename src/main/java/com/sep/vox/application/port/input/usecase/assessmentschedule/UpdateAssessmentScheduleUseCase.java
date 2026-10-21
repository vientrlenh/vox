package com.sep.vox.application.port.input.usecase.assessmentschedule;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.UpdateAssessmentScheduleCommand;
import com.sep.vox.application.port.input.service.AssessmentScheduleCandidateConflictValidator;
import com.sep.vox.application.port.input.service.AssessmentScheduleProctorConflictValidator;
import com.sep.vox.application.port.input.service.AssessmentScheduleRoomValidator;
import com.sep.vox.application.port.input.service.SubscriptionPeriodGuardService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.projection.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.assessment.AssessmentMemberRepository;
import com.sep.vox.domain.repository.assessment.AssessmentRepository;
import com.sep.vox.domain.repository.assessment.AssessmentScheduleRepository;
import com.sep.vox.domain.service.assessment.AssessmentEditingGuard;
import com.sep.vox.domain.service.assessment.AssessmentScheduleWindowMessages;

@Service
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

    public UpdateAssessmentScheduleUseCase(
            AssessmentRepository assessmentRepository,
            AssessmentScheduleRepository assessmentScheduleRepository,
            AssessmentScheduleRoomValidator assessmentScheduleRoomValidator,
            AssessmentScheduleProctorConflictValidator assessmentScheduleProctorConflictValidator,
            AssessmentScheduleCandidateConflictValidator assessmentScheduleCandidateConflictValidator,
            AssessmentMemberRepository assessmentMemberRepository,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository,
            SubscriptionPeriodGuardService subscriptionPeriodGuardService,
            UserContextPort userContextPort) {
        this.subscriptionPeriodGuardService = subscriptionPeriodGuardService;
        this.assessmentRepository = assessmentRepository;
        this.assessmentScheduleRepository = assessmentScheduleRepository;
        this.assessmentScheduleRoomValidator = assessmentScheduleRoomValidator;
        this.assessmentScheduleProctorConflictValidator = assessmentScheduleProctorConflictValidator;
        this.assessmentScheduleCandidateConflictValidator = assessmentScheduleCandidateConflictValidator;
        this.assessmentMemberRepository = assessmentMemberRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public UUID execute(UpdateAssessmentScheduleCommand input) {
        // Khoá hàng ca ngay từ đầu: ba luật chống trùng dưới đây đều soát khung giờ MỚI, nên nếu
        // một luồng khác đang xếp người vào chính ca này thì hai bên phải nối đuôi chứ không được
        // cùng đọc một trạng thái cũ.
        var schedule = assessmentScheduleRepository.findByIdForUpdate(input.id())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ca thi"));
        var assessment = assessmentRepository.findById(schedule.getAssessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài kiểm tra"));
        var currentUserId = authorize(assessment);
        AssessmentEditingGuard.requireScheduleEditable(assessment);

        var isClassTest = assessment.getKind() == AssessmentKind.CLASS_TEST;
        if (isClassTest && assessment.getStatus() != AssessmentStatus.DRAFT && assessment.getStatus() != AssessmentStatus.SCHEDULED) {
            throw new IllegalStateException("Chỉ có thể sửa lịch bài kiểm tra trên lớp trước khi bắt đầu");
        }
        if (!isClassTest && !schedule.isModifiable()) {
            throw new IllegalStateException("Chỉ có thể sửa ca thi khi đang ở trạng thái nháp");
        }

        // Giá trị hiệu dụng: dùng giá trị mới nếu được cung cấp, ngược lại giữ giá trị hiện tại.
        UUID effectiveRoomId = input.schoolRoomId() != null ? input.schoolRoomId() : schedule.getSchoolRoomId();
        Instant effectiveStart = input.startDate() != null ? input.startDate() : schedule.getStartDate();
        Instant effectiveEnd = input.endDate() != null ? input.endDate() : schedule.getEndDate();

        if (!effectiveEnd.isAfter(effectiveStart)) {
            throw new IllegalArgumentException("Thời gian kết thúc phải sau thời gian bắt đầu");
        }
        if (assessment.isScheduleWindowShorterThanAssessmentTime(effectiveStart, effectiveEnd)) {
            throw new IllegalArgumentException(AssessmentScheduleWindowMessages.tooShortForAssessmentTime(assessment));
        }
        // Chỉ ràng buộc với kỳ thi thường: ca thi phải nằm trong khung mở/đóng đã định của kỳ thi.
        // CLASS_TEST đi chiều ngược lại -- openAt/closeAt được ghi lại theo ca thi ở dưới, nên so
        // với khung cũ rồi chặn sẽ khiến không dời được lịch bài kiểm tra trên lớp.
        if (!isClassTest && assessment.isScheduleWindowOutsideAssessmentWindow(effectiveStart, effectiveEnd)) {
            throw new IllegalArgumentException(AssessmentScheduleWindowMessages.outsideAssessmentWindow(assessment));
        }

        if (input.schoolRoomId() != null) {
            assessmentScheduleRoomValidator.requireRoomOfAssessmentSchool(input.schoolRoomId(), assessment);
        }

        assessmentScheduleRoomValidator.requireNoOverlap(
            effectiveRoomId, effectiveStart, effectiveEnd, schedule.getId());

        // Dời giờ ca cũng phải soát giám thị, nếu không thì luật "không gác hai ca trùng giờ" bị
        // lách bằng cách gán lúc hai ca chưa đụng nhau rồi mới kéo chúng chồng lên.
        assessmentScheduleProctorConflictValidator.requireProctorsFreeForNewWindow(
            schedule.getId(), effectiveStart, effectiveEnd);
        // Cùng lý do, đổi trục sang thí sinh: xếp học sinh lúc hai ca chưa đụng nhau rồi kéo giờ
        // cho chúng chồng lên là lối lách y hệt.
        assessmentScheduleCandidateConflictValidator.requireCandidatesFreeForNewWindow(
            schedule.getId(), effectiveStart, effectiveEnd);

        // Với bài kiểm tra trên lớp, ca thi CHÍNH LÀ khung mở/đóng của bài (ghi ngược xuống assessment ở
        // dưới), nên sửa ca thi là một đường ghi openAt/closeAt khác -- không soi ở đây thì đổi giờ
        // ca là lối vòng qua mặt ràng buộc hạn gói mà Create/UpdateAssessmentUseCase đang áp.
        if (isClassTest) {
            subscriptionPeriodGuardService.requireWithinSubscriptionPeriod(
                assessment.getSchoolId(), effectiveStart, effectiveEnd);
        }

        var now = Instant.now();
        if (isClassTest) {
            schedule.setSchoolRoomId(effectiveRoomId);
            schedule.setStartDate(effectiveStart);
            schedule.setEndDate(effectiveEnd);
            schedule.setUpdatedAt(now);
            schedule.setUpdatedBy(currentUserId);
            assessmentScheduleRepository.save(schedule);
            assessment.setOpenAt(effectiveStart);
            assessment.setCloseAt(effectiveEnd);
            assessment.setUpdatedAt(now);
            assessment.setUpdatedBy(currentUserId);
            assessmentRepository.save(assessment);
        } else {
            int updated = assessmentScheduleRepository.updateAtomic(
                schedule.getId(),
                input.schoolRoomId(),
                input.startDate(),
                input.endDate(),
                now,
                currentUserId);
            if (updated == 0) {
                throw new IllegalStateException("Không thể cập nhật ca thi (đã bị thay đổi trạng thái)");
            }
        }
        return schedule.getId();
    }

    private UUID authorize(Assessment assessment) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId()
            .orElseThrow(() -> new UnauthorizedException("User is not logged in"));
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        var schoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
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
