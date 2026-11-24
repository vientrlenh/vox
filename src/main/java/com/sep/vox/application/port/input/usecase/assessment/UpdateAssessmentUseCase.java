package com.sep.vox.application.port.input.usecase.assessment;

import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentSchedule;
import java.time.Instant;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.assessment.UpdateAssessmentCommand;
import com.sep.vox.application.port.input.service.ClassTestTokenQuotaGuardService;
import com.sep.vox.application.port.input.service.AssessmentPolicyValidator;
import com.sep.vox.application.port.input.service.AssessmentScheduleCandidateConflictValidator;
import com.sep.vox.application.port.input.service.AssessmentScheduleProctorConflictValidator;
import com.sep.vox.application.port.input.service.AssessmentScheduleRoomValidator;
import com.sep.vox.application.port.input.service.AssessmentStreamConfigResolver;
import com.sep.vox.application.port.input.service.SubscriptionPeriodGuardService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.dto.AssessmentDto;
import com.sep.vox.domain.mapper.AssessmentDtoMapper;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.assessment.AssessmentScheduleStatus;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentScheduleRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.service.assessment.AssessmentEditingGuard;
import com.sep.vox.domain.service.assessment.AssessmentScheduleWindowMessages;

@Service
@RequiredArgsConstructor
public class UpdateAssessmentUseCase implements IUseCase<UpdateAssessmentCommand, AssessmentDto> {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentMemberRepository assessmentMemberRepository;
    private final AssessmentScheduleRepository assessmentScheduleRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final AssessmentPolicyValidator assessmentPolicyValidator;
    private final AssessmentStreamConfigResolver assessmentStreamConfigResolver;
    private final UserContextPort userContextPort;
    private final ClassTestTokenQuotaGuardService classTestTokenQuotaGuardService;
    private final AssessmentScheduleRoomValidator assessmentScheduleRoomValidator;
    private final AssessmentScheduleProctorConflictValidator assessmentScheduleProctorConflictValidator;
    private final AssessmentScheduleCandidateConflictValidator assessmentScheduleCandidateConflictValidator;
    private final SubscriptionPeriodGuardService subscriptionPeriodGuardService;

    @Override
    @Transactional
    public AssessmentDto execute(UpdateAssessmentCommand input) {
        UpdateAssessmentCommand command = normalize(input);
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        UUID currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        boolean schoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));

        Assessment assessment = assessmentRepository.findById(command.assessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài kiểm tra"));

        authorizeMutation(assessment.getId(), assessment.getSchoolId(), assessment.getKind(), currentUserId, currentSchoolId, schoolAdmin);
        if (assessment.getKind() == AssessmentKind.CLASS_TEST
                && assessment.getStatus() != AssessmentStatus.DRAFT
                && assessment.getStatus() != AssessmentStatus.SCHEDULED) {
            throw new IllegalArgumentException("Chỉ được cập nhật bài kiểm tra trên lớp khi chưa bắt đầu");
        }
        // Kỳ thi thường: thí sinh đã vào phòng thi nên mọi thay đổi thông tin đều làm lệch dữ liệu
        // đang chạy (khung giờ, thời gian làm bài, cách chốt điểm).
        AssessmentEditingGuard.requireAssessmentEditable(assessment);

        if (command.name() != null) {
            assessment.setName(command.name());
        }
        if (command.description() != null) {
            assessment.setDescription(command.description());
        }
        if (command.openAt() != null) {
            assessment.setOpenAt(command.openAt());
        }
        if (command.closeAt() != null) {
            assessment.setCloseAt(command.closeAt());
        }
        if (command.assessmentPolicyId() != null) {
            // Kiểm theo trường của chính bài kiểm tra, không phải trường người gọi: bài đã tồn tại nên
            // schoolId của nó mới là phạm vi đúng.
            assessmentPolicyValidator.validateIfPresent(command.assessmentPolicyId(), assessment.getSchoolId());
            assessment.setAssessmentPolicyId(command.assessmentPolicyId());
        }
        if (command.maxAttempt() != null) {
            assessment.setMaxAttempt(command.maxAttempt());
        }
        if (command.assessmentTimeDurationSecond() != null) {
            assessment.setAssessmentTimeDurationSecond(command.assessmentTimeDurationSecond());
        }
        // A published (SCHEDULED) assessment -- centralized or class test -- can still have its
        // duration/maxAttempt edited -- if it is edited without re-checking, the token estimate checked at publish time
        // (UpdateAssessmentStatusUseCase.validatePlanLimits) becomes meaningless, so exactly the same guard
        // has to run again here, not only for CLASS_TEST.
        //
        // Note: since the cost-generating seconds were separated from the assessment duration (see PaperTimeCalculator),
        // the estimate NO LONGER reads assessmentTimeDurationSecond but walks the paper variants itself, so only maxAttempt
        // really changes it. It is still re-checked when the duration changes because it is cheap, and because the REST path that writes that
        // column by hand may diverge from the real paper variants.
        if (assessment.getStatus() == AssessmentStatus.SCHEDULED
                && (command.maxAttempt() != null || command.assessmentTimeDurationSecond() != null)) {
            classTestTokenQuotaGuardService.requireWithinTokenQuota(assessment);
        }
        if (command.resultDecisionMethod() != null) {
            assessment.setResultDecisionMethod(command.resultDecisionMethod());
        }
        if (command.requiresOtp() != null) {
            assessment.setRequiresOtp(command.requiresOtp());
        }
        if (command.aiConfidenceThresholdPercent() != null) {
            assessment.setAiConfidenceThresholdPercent(command.aiConfidenceThresholdPercent());
        }
        // null = giữ nguyên (patch semantics như mọi trường khác); danh sách RỖNG = tắt giám sát.
        // Phân biệt này là bắt buộc: lúc tạo, null nghĩa là "không giám sát", còn ở đây null phải
        // mang nghĩa "không đụng tới" thì sửa tên mới không vô tình xoá cấu hình giám sát.
        if (command.requiredStreamTypes() != null) {
            AssessmentStreamConfigResolver.StreamConfig streamConfig = assessmentStreamConfigResolver.resolve(
                command.requiredStreamTypes(), command.streamTypePermission());
            assessment.setRequiredStreamType(streamConfig.requiredStreamType());
            assessment.setStreamTypePermission(streamConfig.streamTypePermission());
        }
        if (assessment.getKind() == AssessmentKind.CLASS_TEST) {
            requireAssessmentWindow(assessment.getOpenAt(), assessment.getCloseAt(), assessment.getKind());
            // Khung mở/đóng sẽ được đồng bộ thẳng xuống ca thi ở dưới, nên phải đủ dài cho bài làm.
            if (touchesScheduleTiming(command)
                    && assessment.isScheduleWindowShorterThanAssessmentTime(assessment.getOpenAt(), assessment.getCloseAt())) {
                throw new IllegalArgumentException(AssessmentScheduleWindowMessages.schedulesNoLongerFit(1, assessment));
            }
        } else {
            requireAssessmentWindow(assessment.getOpenAt(), assessment.getCloseAt(), assessment.getKind());
            requireExistingSchedulesStillFit(assessment, command);
        }
        // Soi theo trường của chính bài kiểm tra, không phải trường người gọi -- cùng lý do đã ghi ở
        // nhánh assessmentPolicy phía trên.
        subscriptionPeriodGuardService.requireWithinSubscriptionPeriod(
            assessment.getSchoolId(), assessment.getOpenAt(), assessment.getCloseAt());
        Instant now = Instant.now();
        if (assessment.getKind() == AssessmentKind.CLASS_TEST && (command.openAt() != null || command.closeAt() != null)) {
            syncClassTestDraftSchedules(assessment, currentUserId, now);
        }
        return AssessmentDtoMapper.toDto(assessmentRepository.save(assessment));
    }

    /**
     * Changing the open/close window of a class test writes the new time DIRECTLY down to the session, without going through
     * {@code UpdateAssessmentScheduleUseCase}. So it has to check exactly the rules that the other use case checks -- otherwise,
     * editing the time on the assessment screen becomes a detour around all three anti-overlap rules (a room booked
     * for two sessions, a teacher proctoring two sessions, a student placed in two sessions).
     */
    private void syncClassTestDraftSchedules(com.sep.vox.domain.model.assessment.Assessment assessment, java.util.UUID currentUserId, Instant now) {
        for (AssessmentSchedule schedule : assessmentScheduleRepository.findByAssessmentId(assessment.getId())) {
            if (schedule.getStatus() == AssessmentScheduleStatus.DRAFT || schedule.getStatus() == AssessmentScheduleStatus.PUBLISHED) {
                assessmentScheduleRoomValidator.requireNoOverlap(
                    schedule.getSchoolRoomId(), assessment.getOpenAt(), assessment.getCloseAt(), schedule.getId());
                assessmentScheduleProctorConflictValidator.requireProctorsFreeForNewWindow(
                    schedule.getId(), assessment.getOpenAt(), assessment.getCloseAt());
                assessmentScheduleCandidateConflictValidator.requireCandidatesFreeForNewWindow(
                    schedule.getId(), assessment.getOpenAt(), assessment.getCloseAt());

                schedule.setStartDate(assessment.getOpenAt());
                schedule.setEndDate(assessment.getCloseAt());
                assessmentScheduleRepository.save(schedule);
            }
        }
    }

    private UpdateAssessmentCommand normalize(UpdateAssessmentCommand input) {
        return new UpdateAssessmentCommand(
            input.assessmentId(),
            StringNormalization.trimAndCollapseSpaces(input.name()),
            StringNormalization.trimAndCollapseSpaces(input.description()),
            input.openAt(),
            input.closeAt(),
            input.assessmentPolicyId(),
            input.maxAttempt(),
            input.assessmentTimeDurationSecond(),
            input.resultDecisionMethod(),
            input.requiresOtp(),
            input.requiredStreamTypes() == null ? null : input.requiredStreamTypes().stream()
                .map(StringNormalization::normalizeCode)
                .toList(),
            input.streamTypePermission(),
            input.aiConfidenceThresholdPercent()
        );
    }

    private void authorizeMutation(
            java.util.UUID assessmentId,
            java.util.UUID assessmentSchoolId,
            AssessmentKind kind,
            java.util.UUID currentUserId,
            java.util.UUID currentSchoolId,
            boolean schoolAdmin) {
        if (kind == AssessmentKind.CENTRALIZED) {
            if (schoolAdmin && currentSchoolId != null && currentSchoolId.equals(assessmentSchoolId)) {
                return;
            }
            // Chủ tịch hội đồng chạy trọn quy trình kỳ thi tập trung trên trang chi tiết (lập hội đồng →
            // chốt khung đề → ra đề → xếp lịch), nên phải sửa được thông tin kỳ thi như quản trị trường.
            if (assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessmentId, currentUserId, AssessmentMemberRole.CHAIR)) {
                return;
            }
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
        if (!assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessmentId, currentUserId, AssessmentMemberRole.CHAIR)) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
        if (assessmentRepository.existsSubmittedSessionByAssessmentId(assessmentId)) {
            throw new IllegalArgumentException("Không thể cập nhật bài kiểm tra khi đã có bài nộp");
        }
    }

    /**
     * A regular assessment: the session is an independent schedule (with a room, proctors and assigned candidates) so it must not be moved
     * along with the assessment by itself -- instead it blocks if the new time window or attempt time makes an already scheduled
     * session no longer valid. Only sessions that will still take place are considered; COMPLETED/MOVED/CANCELLED are the past
     * (findByAssessmentId has already excluded DELETED).
     */
    /**
     * Chỉ kiểm lại bất biến ca thi khi thao tác thực sự đụng tới thời gian; sửa tên/mô tả không được
     * bị chặn chỉ vì dữ liệu sẵn có đang lệch.
     */
    private boolean touchesScheduleTiming(UpdateAssessmentCommand command) {
        return command.openAt() != null
            || command.closeAt() != null
            || command.assessmentTimeDurationSecond() != null;
    }

    private void requireExistingSchedulesStillFit(com.sep.vox.domain.model.assessment.Assessment assessment, UpdateAssessmentCommand command) {
        if (!touchesScheduleTiming(command)) {
            return;
        }
        List<AssessmentSchedule> activeSchedules = assessmentScheduleRepository.findByAssessmentId(assessment.getId()).stream()
            .filter(schedule -> schedule.getStatus() == AssessmentScheduleStatus.DRAFT
                || schedule.getStatus() == AssessmentScheduleStatus.PUBLISHED)
            .toList();

        long outsideCount = activeSchedules.stream()
            .filter(schedule -> assessment.isScheduleWindowOutsideAssessmentWindow(schedule.getStartDate(), schedule.getEndDate()))
            .count();
        if (outsideCount > 0) {
            throw new IllegalArgumentException(AssessmentScheduleWindowMessages.schedulesOutsideNewWindow((int) outsideCount));
        }

        long tooShortCount = activeSchedules.stream()
            .filter(schedule -> assessment.isScheduleWindowShorterThanAssessmentTime(schedule.getStartDate(), schedule.getEndDate()))
            .count();
        if (tooShortCount > 0) {
            throw new IllegalArgumentException(AssessmentScheduleWindowMessages.schedulesNoLongerFit((int) tooShortCount, assessment));
        }
    }


    /**
     * Cả hai kind đều bắt buộc có khung mở/đóng: với bài trên lớp vì khung này chính là ca thi, với
     * kỳ thi tập trung vì nó là ràng buộc ngoài của mọi ca thi -- để trống thì ca thi không còn mốc
     * nào chặn, kể cả hạn gói dịch vụ.
     *
     * <p>Bài kiểm tra tạo từ trước khi luật này có hiệu lực có thể còn null; lúc đó phải nhập đủ hai
     * mốc mới lưu được, đúng như bài tạo mới.
     */
    private void requireAssessmentWindow(Instant openAt, Instant closeAt, AssessmentKind kind) {
        if (openAt == null || closeAt == null) {
            throw new IllegalArgumentException(kind == AssessmentKind.CLASS_TEST
                ? "Bài kiểm tra trên lớp phải có thời gian mở bài và đóng bài"
                : "Kỳ thi phải có thời gian mở bài và đóng bài");
        }
        if (!openAt.isBefore(closeAt)) {
            throw new IllegalArgumentException("Thời gian mở bài phải nhỏ hơn thời gian đóng bài");
        }
    }
}
