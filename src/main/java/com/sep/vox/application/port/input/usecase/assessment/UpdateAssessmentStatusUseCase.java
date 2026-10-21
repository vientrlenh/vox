package com.sep.vox.application.port.input.usecase.assessment;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.event.assessment.AssessmentResultsPublishedEvent;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.PlanLimitExceededException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.UpdateAssessmentStatusCommand;
import com.sep.vox.application.port.input.service.AssessmentCandidateResultFinalizationService;
import com.sep.vox.application.port.input.service.AssessmentScheduleClosureService;
import com.sep.vox.application.port.input.service.ClassTestGradingAssignmentService;
import com.sep.vox.application.port.input.service.AssessmentHumanGradingNotificationService;
import com.sep.vox.application.port.input.service.ClassTestTokenQuotaGuardService;
import com.sep.vox.application.port.input.service.ZeroScoreAssessmentResultService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.EventPublisherPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.projection.repository.UserRoleQueryRepository;
import com.sep.vox.application.shared.StringNormalization;
import com.sep.vox.domain.dto.assessment.AssessmentDto;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.assessment.AssessmentCandidateStatus;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.assessment.AssessmentPaperStatus;
import com.sep.vox.domain.model.assessment.AssessmentSchedule;
import com.sep.vox.domain.model.assessment.AssessmentScheduleStatus;
import com.sep.vox.domain.model.assessment.AssessmentSessionStatus;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;
import com.sep.vox.domain.repository.assessment.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.assessment.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.assessment.AssessmentMemberRepository;
import com.sep.vox.domain.repository.assessment.AssessmentPaperRepository;
import com.sep.vox.domain.repository.assessment.AssessmentRepository;
import com.sep.vox.domain.repository.assessment.AssessmentScheduleProctorRepository;
import com.sep.vox.domain.repository.assessment.AssessmentScheduleRepository;
import com.sep.vox.domain.repository.assessment.AssessmentSessionRepository;
import com.sep.vox.domain.service.assessment.AssessmentScheduleWindowMessages;

@Service
public class UpdateAssessmentStatusUseCase implements IUseCase<UpdateAssessmentStatusCommand, AssessmentDto> {

    /**
     * Huỷ được khi bài chưa chốt kết quả. RESULTS_PUBLISHED không rút lại được (học sinh đã xem điểm),
     * và CANCELLED thì không huỷ thêm lần nữa.
     */
    private static final Set<AssessmentStatus> CANCELLABLE_FROM =
        EnumSet.of(AssessmentStatus.DRAFT, AssessmentStatus.SCHEDULED, AssessmentStatus.IN_PROGRESS, AssessmentStatus.CLOSED);

    private final AssessmentRepository assessmentRepository;
    private final AssessmentMemberRepository assessmentMemberRepository;
    private final AssessmentPaperRepository assessmentPaperRepository;
    private final AssessmentScheduleRepository assessmentScheduleRepository;
    private final AssessmentScheduleProctorRepository assessmentScheduleProctorRepository;
    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final AssessmentSessionRepository assessmentSessionRepository;
    private final AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private final AssessmentPolicyRepository assessmentPolicyRepository;
    private final AssessmentCandidateResultFinalizationService assessmentCandidateResultFinalizationService;
    private final ZeroScoreAssessmentResultService zeroScoreAssessmentResultService;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final AssessmentQuestionSecureLockService assessmentQuestionSecureLockService;
    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final UserContextPort userContextPort;
    private final EventPublisherPort eventPublisherPort;
    private final ClassTestGradingAssignmentService classTestGradingAssignmentService;
    private final AssessmentHumanGradingNotificationService assessmentHumanGradingNotificationService;
    private final ClassTestTokenQuotaGuardService classTestTokenQuotaGuardService;
    private final AssessmentScheduleClosureService assessmentScheduleClosureService;

    public UpdateAssessmentStatusUseCase(
            AssessmentRepository assessmentRepository,
            AssessmentMemberRepository assessmentMemberRepository,
            AssessmentPaperRepository assessmentPaperRepository,
            AssessmentScheduleRepository assessmentScheduleRepository,
            AssessmentScheduleProctorRepository assessmentScheduleProctorRepository,
            AssessmentCandidateRepository assessmentCandidateRepository,
            AssessmentSessionRepository assessmentSessionRepository,
            AssessmentCandidateResultRepository assessmentCandidateResultRepository,
            AssessmentPolicyRepository assessmentPolicyRepository,
            AssessmentCandidateResultFinalizationService assessmentCandidateResultFinalizationService,
            ZeroScoreAssessmentResultService zeroScoreAssessmentResultService,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository,
            AssessmentQuestionSecureLockService assessmentQuestionSecureLockService,
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            SubscriptionPlanRepository subscriptionPlanRepository,
            UserContextPort userContextPort,
            EventPublisherPort eventPublisherPort,
            ClassTestGradingAssignmentService classTestGradingAssignmentService,
            AssessmentHumanGradingNotificationService assessmentHumanGradingNotificationService,
            ClassTestTokenQuotaGuardService classTestTokenQuotaGuardService,
            AssessmentScheduleClosureService assessmentScheduleClosureService) {
        this.assessmentRepository = assessmentRepository;
        this.assessmentMemberRepository = assessmentMemberRepository;
        this.assessmentPaperRepository = assessmentPaperRepository;
        this.assessmentScheduleRepository = assessmentScheduleRepository;
        this.assessmentScheduleProctorRepository = assessmentScheduleProctorRepository;
        this.assessmentCandidateRepository = assessmentCandidateRepository;
        this.assessmentSessionRepository = assessmentSessionRepository;
        this.assessmentCandidateResultRepository = assessmentCandidateResultRepository;
        this.assessmentPolicyRepository = assessmentPolicyRepository;
        this.assessmentCandidateResultFinalizationService = assessmentCandidateResultFinalizationService;
        this.zeroScoreAssessmentResultService = zeroScoreAssessmentResultService;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.assessmentQuestionSecureLockService = assessmentQuestionSecureLockService;
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.userContextPort = userContextPort;
        this.eventPublisherPort = eventPublisherPort;
        this.classTestGradingAssignmentService = classTestGradingAssignmentService;
        this.assessmentHumanGradingNotificationService = assessmentHumanGradingNotificationService;
        this.classTestTokenQuotaGuardService = classTestTokenQuotaGuardService;
        this.assessmentScheduleClosureService = assessmentScheduleClosureService;
    }

    @Override
    @Transactional
    public AssessmentDto execute(UpdateAssessmentStatusCommand input) {
        var command = new UpdateAssessmentStatusCommand(
            input.assessmentId(),
            StringNormalization.normalizeCode(input.action()),
            StringNormalization.trimAndCollapseSpaces(input.note())
        );
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId()
            .orElseThrow(() -> new UnauthorizedException("User is not logged in"));
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(user -> user.getSchoolId())
            .orElse(null);
        var schoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));

        var assessment = assessmentRepository.findById(command.assessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài kiểm tra"));

        authorizeMutation(assessment.getId(), assessment.getSchoolId(), assessment.getKind(), currentUserId, currentSchoolId, schoolAdmin);

        var now = Instant.now();
        switch (command.action()) {
            case "SCHEDULE" -> {
                // Trạng thái kiểm TRƯỚC hạn mức gói: bấm SCHEDULE trên bài không còn DRAFT mà báo lỗi
                // "vượt quá giới hạn gói" thì người dùng đi sửa nhầm chỗ.
                requireTransition(assessment, AssessmentStatus.DRAFT, AssessmentStatus.SCHEDULED);
                validatePlanLimits(assessment);
                if (assessment.getKind() == AssessmentKind.CLASS_TEST) {
                    requireClassTestScheduleReady(assessment);
                } else {
                    requireCentralizedScheduleReadiness(assessment);
                }
            }
            case "START" -> {
                if (assessment.getKind() == AssessmentKind.CLASS_TEST) {
                    requireClassTestCanStart(assessment);
                }
                requireTransition(assessment, AssessmentStatus.SCHEDULED, AssessmentStatus.IN_PROGRESS);
                if (assessment.getKind() == AssessmentKind.CLASS_TEST) {
                    lockClassTestPapers(assessment.getId(), currentUserId);
                }
            }
            case "CLOSE" -> {
                requireTransition(assessment, AssessmentStatus.IN_PROGRESS, AssessmentStatus.CLOSED);
                // Kiểm trạng thái trước rồi mới đụng ca thi, giống nhánh CANCEL bên dưới.
                assessmentScheduleClosureService.requireNoActiveSessionInOngoingSchedule(assessment.getId(), now);
                assessmentScheduleClosureService.closeSchedulesForAssessment(assessment.getId(), currentUserId, now);
                assessmentQuestionSecureLockService.releaseIfAutoAfterClose(assessment.getId());
                zeroScoreAssessmentResultService.ensureZeroResultsForMissingOrEmptyAttempts(assessment.getId());
                // Quét bù SAU khi đã bù kết quả điểm 0: bài trên lớp không có ai điều phối
                // chấm, nên đóng bài là chốt cuối để giáo viên chủ bài nhận hết phần còn lại.
                classTestGradingAssignmentService.ensureAssignmentsForAssessment(assessment.getId());
                // Sau quét bù: con số "còn bao nhiêu bài chờ chấm" chỉ đúng khi hàng đợi đã
                // đầy đủ. Áp dụng cho CẢ hai loại bài -- quét bù ở trên chỉ lo bài trên lớp.
                assessmentHumanGradingNotificationService.publishIfPendingReview(assessment, now);
            }
            case "PUBLISH_RESULTS" -> {
                zeroScoreAssessmentResultService.ensureZeroResultsForMissingOrEmptyAttempts(assessment.getId());
                requirePublishReadiness(assessment.getId());
                requireTransition(assessment, AssessmentStatus.CLOSED, AssessmentStatus.RESULTS_PUBLISHED);
                finalizePassFailForAssessment(assessment);
                eventPublisherPort.publish(new AssessmentResultsPublishedEvent(assessment.getId()));
            }
            case "CANCEL" -> {
                // Kiểm trạng thái trước rồi mới đụng ca thi: huỷ không hợp lệ thì không được ghi gì.
                requireTransition(assessment, CANCELLABLE_FROM, AssessmentStatus.CANCELLED);
                assessmentScheduleClosureService.cancelSchedulesForAssessment(assessment.getId(), currentUserId, now);
            }
            default -> throw new IllegalStateException("Action không hợp lệ");
        }

        assessment.setUpdatedAt(now);
        assessment.setUpdatedBy(currentUserId);
        return AssessmentDto.toDto(assessmentRepository.save(assessment));
    }

    private void authorizeMutation(
            UUID assessmentId,
            UUID assessmentSchoolId,
            AssessmentKind kind,
            UUID currentUserId,
            UUID currentSchoolId,
            boolean schoolAdmin) {
        if (kind == AssessmentKind.CENTRALIZED) {
            if (schoolAdmin && currentSchoolId != null && currentSchoolId.equals(assessmentSchoolId)) {
                return;
            }
            // Chủ tịch hội đồng chạy trọn quy trình kỳ thi tập trung trên trang chi tiết, nên cũng là
            // người bấm lên lịch / bắt đầu / đóng / công bố kết quả. Xoá kỳ thi thì vẫn chỉ quản trị
            // trường (xem DeleteAssessmentUseCase) — đó là thao tác phá huỷ, không nằm trong quy trình này.
            if (assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessmentId, currentUserId, AssessmentMemberRole.CHAIR)) {
                return;
            }
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
        if (!assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessmentId, currentUserId, AssessmentMemberRole.CHAIR)) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
    }

    private void requireTransition(Assessment assessment, AssessmentStatus from, AssessmentStatus to) {
        requireTransition(assessment, EnumSet.of(from), to);
    }

    /**
     * Bản nhiều trạng thái nguồn, hiện chỉ CANCEL dùng: huỷ được từ bất kỳ trạng thái nào chưa chốt,
     * nhưng kết quả đã công bố thì không rút lại được, và bài đã huỷ không huỷ thêm lần nữa.
     */
    private void requireTransition(Assessment assessment, Set<AssessmentStatus> allowedFrom, AssessmentStatus to) {
        if (!allowedFrom.contains(assessment.getStatus())) {
            throw new IllegalStateException("Trạng thái bài kiểm tra hiện tại không hợp lệ cho action này");
        }
        assessment.setStatus(to);
    }

    private void validatePlanLimits(Assessment assessment) {
        var activeSubscription = schoolSubscriptionRepository.findActiveBySchoolId(assessment.getSchoolId())
            .orElseThrow(() -> new PlanLimitExceededException(
                "Trường chưa có gói subscription đang hoạt động, không thể lên lịch kỳ thi"));
        var plan = subscriptionPlanRepository.findById(activeSubscription.getSubscriptionPlanId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy gói subscription"));

        if (assessment.getTimeDurationSecond() != null && plan.getMaxTimePerAttemptMin() != null
                && assessment.getTimeDurationSecond() > plan.getMaxTimePerAttemptMin() * 60) {
            throw new PlanLimitExceededException(
                "Thời lượng bài thi (" + assessment.getTimeDurationSecond() + " giây) vượt quá giới hạn của gói \""
                    + plan.getName() + "\" (tối đa " + plan.getMaxTimePerAttemptMin() + " phút/lượt thi)"
            );
        }

        // Ước lượng worst-case: mọi thí sinh dùng hết toàn bộ thời lượng bài thi cho mỗi lượt làm bài,
        // soi trước hạn mức GRADING (và với bài trên lớp, cả CLASS_TEST + hạn mức cá nhân giáo viên) --
        // xem ClassTestTokenQuotaGuardService để rõ vì sao phải soi cả 3 chỗ.
        classTestTokenQuotaGuardService.requireWithinTokenQuota(assessment);
    }

    /**
     * Bài kiểm tra trên lớp cũng thi trong phòng có giám khảo như kỳ thi tập trung, nên trước khi
     * công bố lịch phải đủ: ca thi có phòng, có giám khảo, và mọi học sinh đã được xếp ca + có đề.
     *
     * <p>Chọn lớp lúc tạo chỉ nạp học sinh vào danh sách dự thi; xếp ca là bước riêng, nên nếu
     * không chặn ở đây thì bài vẫn lên lịch được với cả lớp chưa có ca và không ai vào thi được.
     */
    private void requireClassTestScheduleReadiness(
            Assessment assessment,
            List<AssessmentSchedule> schedules) {
        requireSchedulesHaveRoomAndProctor(schedules);

        var candidates = assessmentCandidateRepository.findByAssessmentId(assessment.getId());
        var withoutSchedule = candidates.stream()
            .filter(candidate -> candidate.getScheduleId() == null)
            .count();
        if (withoutSchedule > 0) {
            throw new IllegalStateException(
                "Còn " + withoutSchedule + " học sinh chưa được xếp vào ca thi");
        }
        var withoutPaper = candidates.stream()
            .filter(candidate -> candidate.getAssignedPaperId() == null)
            .count();
        if (withoutPaper > 0) {
            throw new IllegalStateException("Còn " + withoutPaper + " học sinh chưa được gán đề");
        }
    }

    /**
     * Ca thi còn hiệu lực của bài. Ca đã huỷ/dời sẽ không bao giờ diễn ra nên không được tham gia
     * bất kỳ điều kiện lên lịch nào: soi phòng/giám thị của chúng thì kỳ thi bị chặn
     * bởi một ca đã bỏ, còn đếm chúng vào "đã có ca thi" thì kỳ thi lên lịch được mà không còn ca nào
     * chạy thật. {@code findByAssessmentId} chỉ lọc sẵn DELETED nên phải lọc thêm ở đây.
     */
    private List<AssessmentSchedule> effectiveSchedules(UUID assessmentId) {
        return assessmentScheduleRepository.findByAssessmentId(assessmentId).stream()
            .filter(schedule -> schedule.getStatus() != null && schedule.getStatus().isInEffect())
            .toList();
    }

    /** Ca thi còn hiệu lực nào cũng phải có phòng và ít nhất một giám khảo — đúng cho cả hai loại bài. */
    private void requireSchedulesHaveRoomAndProctor(
            List<AssessmentSchedule> schedules) {
        for (var schedule : schedules) {
            if (schedule.getSchoolRoomId() == null) {
                throw new IllegalStateException("Ca thi chưa được chọn phòng");
            }
            if (assessmentScheduleProctorRepository.countByScheduleId(schedule.getId()) == 0) {
                throw new IllegalStateException("Ca thi chưa có giám khảo");
            }
        }
    }

    /**
     * Kỳ thi tập trung trước đây lên lịch được cả khi chưa có ca thi, chưa có thí sinh và chưa có mã
     * đề nào — bài mang trạng thái SCHEDULED mà thực tế không ai vào thi được.
     *
     * <p>Chốt ở đây nhẹ hơn bài trên lớp một bậc: KHÔNG đòi từng thí sinh đã có ca và đã có đề, vì
     * luật đó đã nằm ở bước công bố từng ca ({@code UpdateAssessmentScheduleStatusUseCase.publish}: ca phải
     * có thí sinh và mọi thí sinh trong ca đã có đề). Kỳ thi tập trung vì thế phải xếp ca
     * ({@code AssignAssessmentCandidateSchedule}/{@code AutoFill}) và phân đề ({@code AssignAssessmentPapersUseCase},
     * đòi mọi đề LOCKED) TRƯỚC khi công bố ca -- cả hai thao tác đều chạy được lúc kỳ thi còn DRAFT.
     * Ở đây chỉ chặn đúng trường hợp "rỗng" mà không có cách nào chạy được.
     *
     * <p>Ngoài ra mọi ca thi phải đã được công bố: ca còn DRAFT thì học sinh và giám thị chưa nhìn
     * thấy, kỳ thi "đã lên lịch" mà có ca không ai vào được.
     */
    private void requireCentralizedScheduleReadiness(Assessment assessment) {
        var schedules = effectiveSchedules(assessment.getId());
        if (schedules.isEmpty()) {
            throw new IllegalStateException("Kỳ thi chưa có ca thi nào, không thể lên lịch");
        }
        requireSchedulesHaveRoomAndProctor(schedules);
        if (assessmentCandidateRepository.countByAssessmentId(assessment.getId()) == 0) {
            throw new IllegalStateException("Kỳ thi chưa có thí sinh nào, không thể lên lịch");
        }
        if (assessmentPaperRepository.findByAssessmentId(assessment.getId()).isEmpty()) {
            throw new IllegalStateException("Kỳ thi chưa có mã đề nào, không thể lên lịch");
        }
        // Công bố từng ca là thao tác riêng (UpdateAssessmentScheduleStatusUseCase) và không đòi trạng thái
        // kỳ thi, nên chặn ở đây không khoá luồng: công bố hết ca xong mới lên lịch được kỳ thi.
        var draftScheduleCount = schedules.stream()
            .filter(schedule -> schedule.getStatus() == AssessmentScheduleStatus.DRAFT)
            .count();
        if (draftScheduleCount > 0) {
            throw new IllegalStateException(
                "Còn " + draftScheduleCount + " ca thi chưa được công bố, không thể lên lịch kỳ thi");
        }
    }

    /**
     * Trước đây hàm này tự đẩy mọi ca thi DRAFT sang PUBLISHED khi giáo viên bấm lên lịch — bài trên
     * lớp và kỳ thi tập trung vì thế chạy hai mô hình ngược nhau, và ca thi đổi trạng thái sau lưng
     * người dùng. Giờ nó chỉ KIỂM TRA, không ghi: công bố từng ca là thao tác riêng
     * ({@code UpdateAssessmentScheduleStatusUseCase}, không phân biệt loại bài), còn lên lịch bài chỉ chốt
     * lại kết quả của thao tác đó — đúng như {@link #requireCentralizedScheduleReadiness}.
     */
    private void requireClassTestScheduleReady(Assessment assessment) {
        requireClassTestScheduleWindow(assessment);
        var schedules = effectiveSchedules(assessment.getId());
        if (schedules.isEmpty()) {
            throw new IllegalStateException("Bài kiểm tra trên lớp chưa có lịch");
        }
        requireClassTestScheduleReadiness(assessment, schedules);
        var draftScheduleCount = schedules.stream()
            .filter(schedule -> schedule.getStatus() == AssessmentScheduleStatus.DRAFT)
            .count();
        if (draftScheduleCount > 0) {
            throw new IllegalStateException(
                "Còn " + draftScheduleCount + " ca thi chưa được công bố, không thể lên lịch bài kiểm tra");
        }
    }

    private void requireClassTestScheduleWindow(Assessment assessment) {
        if (assessment.getOpenAt() == null || assessment.getCloseAt() == null) {
            throw new IllegalStateException("Bài kiểm tra trên lớp phải có thời gian mở bài và đóng bài trước khi lên lịch");
        }
        if (!assessment.getOpenAt().isBefore(assessment.getCloseAt())) {
            throw new IllegalStateException("Thời gian mở bài phải nhỏ hơn thời gian đóng bài");
        }
        // Ca thi của bài trên lớp bám đúng khung mở/đóng, nên khung phải đủ cho thời gian làm bài.
        // Đặt ở đây (không phải CreateClassTestUseCase.createDraftSchedule) vì lúc tạo ca thi thì
        // assessmentTimeDurationSecond còn là giá trị thô người dùng nhập, chưa qua recalculate.
        if (assessment.isScheduleWindowShorterThanAssessmentTime(assessment.getOpenAt(), assessment.getCloseAt())) {
            throw new IllegalStateException(AssessmentScheduleWindowMessages.schedulesNoLongerFit(1, assessment));
        }
    }

    private void requireClassTestCanStart(Assessment assessment) {
        var now = Instant.now();
        if (assessment.getCloseAt() != null && !now.isBefore(assessment.getCloseAt())) {
            throw new IllegalStateException("Bài kiểm tra trên lớp đã quá thời gian đóng bài");
        }
    }

    /**
     * G.3: chỉ cho publish khi MỌI AssessmentCandidateResult của kỳ thi đều đã ở đúng RELEASED
     * hoặc INVALID (2 trạng thái "đã xử lý xong, sẵn sàng chốt") - còn bất kỳ trạng thái
     * nào khác (PENDING_REVIEW chưa duyệt, FINAL/APPEALED/RE_GRADING/RETAKE_REQUIRED từ
     * luồng phúc khảo/nghi vấn) đều chặn publish cho tới khi được xử lý dứt điểm.
     *
     * <p>PHIÊN/KẾT QUẢ ĐÃ XOÁ MỀM (status DELETED) PHẢI LOẠI KHỎI CẢ HAI VÒNG LẶP DƯỚI ĐÂY.
     * Xoá mềm không xoá dòng, chỉ đổi status -- {@code findByCandidateId}/{@code findByAssessmentId}
     * không tự lọc theo status, nên nếu không loại tường minh thì một học sinh thi hỏng, bị
     * xoá phiên, rồi thi lại (DeleteAssessmentSessionUseCase) sẽ chặn publish CỦA CẢ KỲ THI vĩnh
     * viễn: DELETED không bao giờ tự chuyển thành RELEASED/INVALID (AssessmentCandidateResultFinalizationService.
     * finalizeForPublish cũng cố tình bỏ qua DELETED, xem "ngoài phạm vi, giữ nguyên" ở đó),
     * và phiên đã xoá thì hiển nhiên không có kết quả MỚI để mà lấp vào. Đo thật: chặn ở đúng
     * dòng "Còn N kết quả chưa ở trạng thái RELEASED hoặc INVALID" dù kỳ thi không còn kết quả
     * nào đang treo thật sự -- phiên thay thế đã chấm xong, chỉ có phiên CŨ đã xoá là đứng yên.
     */
    private void requirePublishReadiness(UUID assessmentId) {
        var missingResultCount = assessmentCandidateRepository.findByAssessmentId(assessmentId).stream()
            .filter(candidate -> !AssessmentCandidateStatus.isNonScorable(candidate.getStatus()))
            .flatMap(candidate -> assessmentSessionRepository.findByCandidateId(candidate.getId()).stream())
            .filter(session -> assessmentId.equals(session.getAssessmentId()))
            .filter(session -> session.getStatus() != AssessmentSessionStatus.IN_PROGRESS
                && session.getStatus() != AssessmentSessionStatus.INTERRUPTED
                // Phiên đã xoá không bao giờ "có kết quả" (kết quả của nó cũng đã bị xoá theo,
                // xem DeleteAssessmentSessionUseCase), nhưng KHÔNG PHẢI thứ đang chặn publish -- nó bị
                // bỏ qua có chủ đích, không phải bị bỏ sót.
                && session.getStatus() != AssessmentSessionStatus.DELETED)
            .filter(session -> assessmentCandidateResultRepository.findBySessionId(session.getId()).isEmpty())
            .count();
        if (missingResultCount > 0) {
            throw new IllegalStateException(
                "Còn " + missingResultCount + " phiên thi chưa có kết quả, không thể công bố kết quả");
        }

        var notReadyCount = assessmentCandidateResultRepository.findByAssessmentId(assessmentId).stream()
            .filter(result -> result.getStatus() != AssessmentCandidateResultStatus.RELEASED
                && result.getStatus() != AssessmentCandidateResultStatus.INVALID
                && result.getStatus() != AssessmentCandidateResultStatus.DELETED)
            .count();
        if (notReadyCount > 0) {
            throw new IllegalStateException(
                "Còn " + notReadyCount + " kết quả chưa ở trạng thái RELEASED hoặc INVALID, không thể công bố kết quả");
        }
    }

    /**
     * G.3: chốt kết quả cho mọi AssessmentCandidateResult của kỳ thi ngay khi vừa chuyển
     * RESULTS_PUBLISHED - INVALID -> FAILED (điểm ép về 0), RELEASED/FINAL -> PASSED/FAILED
     * theo passingScore nếu policy có ngưỡng, hoặc -> FINAL nếu không có ngưỡng (nhà trường tự
     * chọn PASSED/FAILED sau qua decideAssessmentCandidateResultOutcome). Xem
     * AssessmentCandidateResultFinalizationService.finalizeForPublish.
     */
    private void finalizePassFailForAssessment(Assessment assessment) {
        var passingScore = assessment.getAssessmentPolicyId() == null
            ? null
            : assessmentPolicyRepository.findById(assessment.getAssessmentPolicyId())
                .map(policy -> policy.getPassingScore())
                .orElse(null);

        for (var result : assessmentCandidateResultRepository.findByAssessmentId(assessment.getId())) {
            if (assessmentCandidateResultFinalizationService.finalizeForPublish(result, passingScore)) {
                assessmentCandidateResultRepository.save(result);
            }
        }
    }

    private void lockClassTestPapers(UUID assessmentId, UUID currentUserId) {
        var now = Instant.now();
        for (var paper : assessmentPaperRepository.findByAssessmentId(assessmentId)) {
            if (paper.getStatus() != AssessmentPaperStatus.LOCKED) {
                paper.setStatus(AssessmentPaperStatus.LOCKED);
                paper.setUpdatedAt(now);
                paper.setUpdatedBy(currentUserId);
                assessmentPaperRepository.save(paper);
            }
        }
    }
}
