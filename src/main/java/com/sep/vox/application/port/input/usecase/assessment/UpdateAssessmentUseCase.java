package com.sep.vox.application.port.input.usecase.assessment;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.UpdateAssessmentCommand;
import com.sep.vox.application.port.input.service.ClassTestTokenQuotaGuardService;
import com.sep.vox.application.port.input.service.AssessmentPolicyValidator;
import com.sep.vox.application.port.input.service.AssessmentScheduleCandidateConflictValidator;
import com.sep.vox.application.port.input.service.AssessmentScheduleProctorConflictValidator;
import com.sep.vox.application.port.input.service.AssessmentScheduleRoomValidator;
import com.sep.vox.application.port.input.service.AssessmentStreamConfigResolver;
import com.sep.vox.application.port.input.service.SubscriptionPeriodGuardService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.projection.repository.UserRoleQueryRepository;
import com.sep.vox.application.shared.DateMapper;
import com.sep.vox.application.shared.StringNormalization;
import com.sep.vox.domain.dto.assessment.AssessmentDto;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.assessment.AssessmentScheduleStatus;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.assessment.AssessmentMemberRepository;
import com.sep.vox.domain.repository.assessment.AssessmentRepository;
import com.sep.vox.domain.repository.assessment.AssessmentScheduleRepository;
import com.sep.vox.domain.service.assessment.AssessmentEditingGuard;
import com.sep.vox.domain.service.assessment.AssessmentScheduleWindowMessages;

@Service
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

    public UpdateAssessmentUseCase(
            AssessmentRepository assessmentRepository,
            AssessmentMemberRepository assessmentMemberRepository,
            AssessmentScheduleRepository assessmentScheduleRepository,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository,
            AssessmentPolicyValidator assessmentPolicyValidator,
            AssessmentStreamConfigResolver assessmentStreamConfigResolver,
            UserContextPort userContextPort,
            ClassTestTokenQuotaGuardService classTestTokenQuotaGuardService,
            AssessmentScheduleRoomValidator assessmentScheduleRoomValidator,
            AssessmentScheduleProctorConflictValidator assessmentScheduleProctorConflictValidator,
            AssessmentScheduleCandidateConflictValidator assessmentScheduleCandidateConflictValidator,
            SubscriptionPeriodGuardService subscriptionPeriodGuardService) {
        this.subscriptionPeriodGuardService = subscriptionPeriodGuardService;
        this.assessmentPolicyValidator = assessmentPolicyValidator;
        this.assessmentStreamConfigResolver = assessmentStreamConfigResolver;
        this.assessmentScheduleRoomValidator = assessmentScheduleRoomValidator;
        this.assessmentScheduleProctorConflictValidator = assessmentScheduleProctorConflictValidator;
        this.assessmentScheduleCandidateConflictValidator = assessmentScheduleCandidateConflictValidator;
        this.assessmentRepository = assessmentRepository;
        this.assessmentMemberRepository = assessmentMemberRepository;
        this.assessmentScheduleRepository = assessmentScheduleRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.userContextPort = userContextPort;
        this.classTestTokenQuotaGuardService = classTestTokenQuotaGuardService;
    }

    @Override
    @Transactional
    public AssessmentDto execute(UpdateAssessmentCommand input) {
        var command = normalize(input);
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId()
            .orElseThrow(() -> new UnauthorizedException("User is not logged in"));
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        var schoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));

        var assessment = assessmentRepository.findById(command.assessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài kiểm tra"));

        authorizeMutation(assessment.getId(), assessment.getSchoolId(), assessment.getKind(), currentUserId, currentSchoolId, schoolAdmin);
        if (assessment.getKind() == AssessmentKind.CLASS_TEST
                && assessment.getStatus() != AssessmentStatus.DRAFT
                && assessment.getStatus() != AssessmentStatus.SCHEDULED) {
            throw new IllegalStateException("Chỉ được cập nhật bài kiểm tra trên lớp khi chưa bắt đầu");
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
            assessment.setOpenAt(DateMapper.toInstant(command.openAt()));
        }
        if (command.closeAt() != null) {
            assessment.setCloseAt(DateMapper.toInstant(command.closeAt()));
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
            assessment.setTimeDurationSecond(command.assessmentTimeDurationSecond());
        }
        // Bài đã publish (SCHEDULED) -- kỳ thi tập trung lẫn bài trên lớp -- vẫn sửa được
        // duration/maxAttempt -- sửa xong mà không soi lại thì ước lượng token đã soi lúc publish
        // (UpdateAssessmentStatusUseCase.validatePlanLimits) thành vô nghĩa, nên phải chạy lại đúng guard
        // đó ở đây, không riêng CLASS_TEST.
        //
        // Lưu ý: từ khi tách giây-sinh-chi-phí khỏi thời-lượng-bài-thi (xem PaperTimeCalculator),
        // ước lượng KHÔNG còn đọc assessmentTimeDurationSecond nữa mà tự duyệt mã đề, nên chỉ maxAttempt
        // mới thật sự làm nó đổi. Vẫn soi lại khi duration đổi vì rẻ, và vì đường REST ghi tay cột
        // đó có thể lệch với mã đề thật.
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
            var streamConfig = assessmentStreamConfigResolver.resolve(
                command.requiredStreamTypes(), command.streamTypePermission());
            assessment.setRequiredStreamType(streamConfig.requiredStreamType());
            assessment.setStreamTypePermission(streamConfig.streamTypePermission());
        }
        if (assessment.getKind() == AssessmentKind.CLASS_TEST) {
            requireAssessmentWindow(assessment.getOpenAt(), assessment.getCloseAt(), assessment.getKind());
            // Khung mở/đóng sẽ được đồng bộ thẳng xuống ca thi ở dưới, nên phải đủ dài cho bài làm.
            if (touchesScheduleTiming(command)
                    && assessment.isScheduleWindowShorterThanAssessmentTime(assessment.getOpenAt(), assessment.getCloseAt())) {
                throw new IllegalStateException(AssessmentScheduleWindowMessages.schedulesNoLongerFit(1, assessment));
            }
        } else {
            requireAssessmentWindow(assessment.getOpenAt(), assessment.getCloseAt(), assessment.getKind());
            requireExistingSchedulesStillFit(assessment, command);
        }
        // Soi theo trường của chính bài kiểm tra, không phải trường người gọi -- cùng lý do đã ghi ở
        // nhánh assessmentPolicy phía trên.
        subscriptionPeriodGuardService.requireWithinSubscriptionPeriod(
            assessment.getSchoolId(), assessment.getOpenAt(), assessment.getCloseAt());
        var now = Instant.now();
        if (assessment.getKind() == AssessmentKind.CLASS_TEST && (command.openAt() != null || command.closeAt() != null)) {
            syncClassTestDraftSchedules(assessment, currentUserId, now);
        }
        assessment.setUpdatedAt(now);
        assessment.setUpdatedBy(currentUserId);
        return AssessmentDto.toDto(assessmentRepository.save(assessment));
    }

    /**
     * Đổi khung mở/đóng của bài kiểm tra trên lớp ghi THẲNG giờ mới xuống ca thi, không đi qua
     * {@code UpdateAssessmentScheduleUseCase}. Nên phải soát đúng những luật mà use case kia soát — nếu
     * không, sửa giờ ở màn kỳ thi trở thành đường vòng qua mặt cả ba luật chống trùng (phòng bị đặt
     * hai ca, giáo viên bị gác hai ca, học sinh bị xếp hai ca).
     */
    private void syncClassTestDraftSchedules(com.sep.vox.domain.model.assessment.Assessment assessment, java.util.UUID currentUserId, Instant now) {
        for (var schedule : assessmentScheduleRepository.findByAssessmentId(assessment.getId())) {
            if (schedule.getStatus() == AssessmentScheduleStatus.DRAFT || schedule.getStatus() == AssessmentScheduleStatus.PUBLISHED) {
                assessmentScheduleRoomValidator.requireNoOverlap(
                    schedule.getSchoolRoomId(), assessment.getOpenAt(), assessment.getCloseAt(), schedule.getId());
                assessmentScheduleProctorConflictValidator.requireProctorsFreeForNewWindow(
                    schedule.getId(), assessment.getOpenAt(), assessment.getCloseAt());
                assessmentScheduleCandidateConflictValidator.requireCandidatesFreeForNewWindow(
                    schedule.getId(), assessment.getOpenAt(), assessment.getCloseAt());

                schedule.setStartDate(assessment.getOpenAt());
                schedule.setEndDate(assessment.getCloseAt());
                schedule.setUpdatedAt(now);
                schedule.setUpdatedBy(currentUserId);
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
            input.streamTypePermission() == null ? null : StringNormalization.normalizeCode(input.streamTypePermission())
,
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
            throw new IllegalStateException("Không thể cập nhật bài kiểm tra khi đã có bài nộp");
        }
    }

    /**
     * Kỳ thi thường: ca thi là lịch độc lập (có phòng, giám thị, thí sinh đã gán) nên không được tự
     * dời theo kỳ thi -- thay vào đó chặn nếu khung giờ hoặc thời gian làm bài mới làm ca thi đã lên
     * lịch không còn hợp lệ. Chỉ xét ca còn sẽ diễn ra; COMPLETED/MOVED/CANCELLED là quá khứ
     * (findByAssessmentId đã loại DELETED sẵn).
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
        var activeSchedules = assessmentScheduleRepository.findByAssessmentId(assessment.getId()).stream()
            .filter(schedule -> schedule.getStatus() == AssessmentScheduleStatus.DRAFT
                || schedule.getStatus() == AssessmentScheduleStatus.PUBLISHED)
            .toList();

        var outsideCount = activeSchedules.stream()
            .filter(schedule -> assessment.isScheduleWindowOutsideAssessmentWindow(schedule.getStartDate(), schedule.getEndDate()))
            .count();
        if (outsideCount > 0) {
            throw new IllegalStateException(AssessmentScheduleWindowMessages.schedulesOutsideNewWindow((int) outsideCount));
        }

        var tooShortCount = activeSchedules.stream()
            .filter(schedule -> assessment.isScheduleWindowShorterThanAssessmentTime(schedule.getStartDate(), schedule.getEndDate()))
            .count();
        if (tooShortCount > 0) {
            throw new IllegalStateException(AssessmentScheduleWindowMessages.schedulesNoLongerFit((int) tooShortCount, assessment));
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
            throw new IllegalStateException(kind == AssessmentKind.CLASS_TEST
                ? "Bài kiểm tra trên lớp phải có thời gian mở bài và đóng bài"
                : "Kỳ thi phải có thời gian mở bài và đóng bài");
        }
        if (!openAt.isBefore(closeAt)) {
            throw new IllegalStateException("Thời gian mở bài phải nhỏ hơn thời gian đóng bài");
        }
    }
}
