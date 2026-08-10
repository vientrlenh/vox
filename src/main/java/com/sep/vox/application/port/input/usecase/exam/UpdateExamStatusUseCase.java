package com.sep.vox.application.port.input.usecase.exam;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.ExamCandidateStatusSupport;
import com.sep.vox.application.common.ExamScheduleWindowMessages;
import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.PlanLimitExceededException;
import com.sep.vox.application.event.ExamResultsPublishedEvent;
import com.sep.vox.application.port.input.command.UpdateExamStatusCommand;
import com.sep.vox.application.port.input.service.ExamCandidateResultFinalizationService;
import com.sep.vox.application.port.input.service.ExamScheduleClosureService;
import com.sep.vox.application.port.input.service.ClassTestGradingAssignmentService;
import com.sep.vox.application.port.input.service.ClassTestTokenQuotaGuardService;
import com.sep.vox.application.port.input.service.ZeroScoreExamResultService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.EventPublisherPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.dto.ExamDto;
import com.sep.vox.domain.mapper.ExamDtoMapper;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.exam.ExamPaperStatus;
import com.sep.vox.domain.model.exam.ExamSchedule;
import com.sep.vox.domain.model.exam.ExamScheduleStatus;
import com.sep.vox.domain.model.exam.ExamSessionStatus;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamPaperRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamScheduleRepository;
import com.sep.vox.domain.repository.ExamSessionRepository;
import com.sep.vox.domain.repository.ExamScheduleProctorRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;

@Service
public class UpdateExamStatusUseCase implements IUseCase<UpdateExamStatusCommand, ExamDto> {

    /**
     * Huỷ được khi bài chưa chốt kết quả. RESULTS_PUBLISHED không rút lại được (học sinh đã xem điểm),
     * và CANCELLED thì không huỷ thêm lần nữa.
     */
    private static final Set<ExamStatus> CANCELLABLE_FROM =
        EnumSet.of(ExamStatus.DRAFT, ExamStatus.SCHEDULED, ExamStatus.IN_PROGRESS, ExamStatus.CLOSED);

    private final ExamRepository examRepository;
    private final ExamMemberRepository examMemberRepository;
    private final ExamPaperRepository examPaperRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final ExamScheduleProctorRepository examScheduleProctorRepository;
    private final ExamCandidateRepository examCandidateRepository;
    private final ExamSessionRepository examSessionRepository;
    private final ExamCandidateResultRepository examCandidateResultRepository;
    private final AssessmentPolicyRepository assessmentPolicyRepository;
    private final ExamCandidateResultFinalizationService examCandidateResultFinalizationService;
    private final ZeroScoreExamResultService zeroScoreExamResultService;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final ExamQuestionSecureLockService examQuestionSecureLockService;
    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final UserContextPort userContextPort;
    private final EventPublisherPort eventPublisherPort;
    private final ClassTestGradingAssignmentService classTestGradingAssignmentService;
    private final ClassTestTokenQuotaGuardService classTestTokenQuotaGuardService;
    private final ExamScheduleClosureService examScheduleClosureService;

    public UpdateExamStatusUseCase(
            ExamRepository examRepository,
            ExamMemberRepository examMemberRepository,
            ExamPaperRepository examPaperRepository,
            ExamScheduleRepository examScheduleRepository,
            ExamScheduleProctorRepository examScheduleProctorRepository,
            ExamCandidateRepository examCandidateRepository,
            ExamSessionRepository examSessionRepository,
            ExamCandidateResultRepository examCandidateResultRepository,
            AssessmentPolicyRepository assessmentPolicyRepository,
            ExamCandidateResultFinalizationService examCandidateResultFinalizationService,
            ZeroScoreExamResultService zeroScoreExamResultService,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository,
            ExamQuestionSecureLockService examQuestionSecureLockService,
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            SubscriptionPlanRepository subscriptionPlanRepository,
            UserContextPort userContextPort,
            EventPublisherPort eventPublisherPort,
            ClassTestGradingAssignmentService classTestGradingAssignmentService,
            ClassTestTokenQuotaGuardService classTestTokenQuotaGuardService,
            ExamScheduleClosureService examScheduleClosureService) {
        this.examRepository = examRepository;
        this.examMemberRepository = examMemberRepository;
        this.examPaperRepository = examPaperRepository;
        this.examScheduleRepository = examScheduleRepository;
        this.examScheduleProctorRepository = examScheduleProctorRepository;
        this.examCandidateRepository = examCandidateRepository;
        this.examSessionRepository = examSessionRepository;
        this.examCandidateResultRepository = examCandidateResultRepository;
        this.assessmentPolicyRepository = assessmentPolicyRepository;
        this.examCandidateResultFinalizationService = examCandidateResultFinalizationService;
        this.zeroScoreExamResultService = zeroScoreExamResultService;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.examQuestionSecureLockService = examQuestionSecureLockService;
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.userContextPort = userContextPort;
        this.eventPublisherPort = eventPublisherPort;
        this.classTestGradingAssignmentService = classTestGradingAssignmentService;
        this.classTestTokenQuotaGuardService = classTestTokenQuotaGuardService;
        this.examScheduleClosureService = examScheduleClosureService;
    }

    @Override
    @Transactional
    public ExamDto execute(UpdateExamStatusCommand input) {
        var command = new UpdateExamStatusCommand(
            input.examId(),
            StringNormalization.normalizeCode(input.action()),
            StringNormalization.trimAndCollapseSpaces(input.note())
        );
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(user -> user.getSchoolId())
            .orElse(null);
        var schoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));

        var exam = examRepository.findById(command.examId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));

        authorizeMutation(exam.getId(), exam.getSchoolId(), exam.getKind(), currentUserId, currentSchoolId, schoolAdmin);

        var now = Instant.now();
        switch (command.action()) {
            case "SCHEDULE" -> {
                // Trạng thái kiểm TRƯỚC hạn mức gói: bấm SCHEDULE trên bài không còn DRAFT mà báo lỗi
                // "vượt quá giới hạn gói" thì người dùng đi sửa nhầm chỗ.
                requireTransition(exam, ExamStatus.DRAFT, ExamStatus.SCHEDULED);
                validatePlanLimits(exam);
                if (exam.getKind() == ExamKind.CLASS_TEST) {
                    requireClassTestScheduleReady(exam);
                } else {
                    requireCentralizedScheduleReadiness(exam);
                }
            }
            case "START" -> {
                if (exam.getKind() == ExamKind.CLASS_TEST) {
                    requireClassTestCanStart(exam);
                }
                requireTransition(exam, ExamStatus.SCHEDULED, ExamStatus.IN_PROGRESS);
                if (exam.getKind() == ExamKind.CLASS_TEST) {
                    lockClassTestPapers(exam.getId(), currentUserId);
                }
            }
            case "CLOSE" -> {
                requireTransition(exam, ExamStatus.IN_PROGRESS, ExamStatus.CLOSED);
                // Kiểm trạng thái trước rồi mới đụng ca thi, giống nhánh CANCEL bên dưới.
                examScheduleClosureService.requireNoActiveSessionInOngoingSchedule(exam.getId(), now);
                examScheduleClosureService.closeSchedulesForExam(exam.getId(), currentUserId, now);
                examQuestionSecureLockService.releaseIfAutoAfterClose(exam.getId());
                zeroScoreExamResultService.ensureZeroResultsForMissingOrEmptyAttempts(exam.getId());
                // Quét bù SAU khi đã bù kết quả điểm 0: bài trên lớp không có ai điều phối
                // chấm, nên đóng bài là chốt cuối để giáo viên chủ bài nhận hết phần còn lại.
                classTestGradingAssignmentService.ensureAssignmentsForExam(exam.getId());
            }
            case "PUBLISH_RESULTS" -> {
                zeroScoreExamResultService.ensureZeroResultsForMissingOrEmptyAttempts(exam.getId());
                requirePublishReadiness(exam.getId());
                requireTransition(exam, ExamStatus.CLOSED, ExamStatus.RESULTS_PUBLISHED);
                finalizePassFailForExam(exam);
                eventPublisherPort.publish(new ExamResultsPublishedEvent(exam.getId()));
            }
            case "CANCEL" -> {
                // Kiểm trạng thái trước rồi mới đụng ca thi: huỷ không hợp lệ thì không được ghi gì.
                requireTransition(exam, CANCELLABLE_FROM, ExamStatus.CANCELLED);
                examScheduleClosureService.cancelSchedulesForExam(exam.getId(), currentUserId, now);
            }
            default -> throw new IllegalStateException("Action không hợp lệ");
        }

        exam.setUpdatedAt(now);
        exam.setUpdatedBy(currentUserId);
        return ExamDtoMapper.toDto(examRepository.save(exam));
    }

    private void authorizeMutation(
            UUID examId,
            UUID examSchoolId,
            ExamKind kind,
            UUID currentUserId,
            UUID currentSchoolId,
            boolean schoolAdmin) {
        if (kind == ExamKind.CENTRALIZED) {
            if (schoolAdmin && currentSchoolId != null && currentSchoolId.equals(examSchoolId)) {
                return;
            }
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
        if (!examMemberRepository.existsByExamIdAndUserIdAndRole(examId, currentUserId, ExamMemberRole.CHAIR)) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
    }

    private void requireTransition(Exam exam, ExamStatus from, ExamStatus to) {
        requireTransition(exam, EnumSet.of(from), to);
    }

    /**
     * Bản nhiều trạng thái nguồn, hiện chỉ CANCEL dùng: huỷ được từ bất kỳ trạng thái nào chưa chốt,
     * nhưng kết quả đã công bố thì không rút lại được, và bài đã huỷ không huỷ thêm lần nữa.
     */
    private void requireTransition(Exam exam, Set<ExamStatus> allowedFrom, ExamStatus to) {
        if (!allowedFrom.contains(exam.getStatus())) {
            throw new IllegalStateException("Trạng thái bài kiểm tra hiện tại không hợp lệ cho action này");
        }
        exam.setStatus(to);
    }

    private void validatePlanLimits(Exam exam) {
        var activeSubscription = schoolSubscriptionRepository.findActiveBySchoolId(exam.getSchoolId())
            .orElseThrow(() -> new PlanLimitExceededException(
                "Trường chưa có gói subscription đang hoạt động, không thể lên lịch kỳ thi"));
        var plan = subscriptionPlanRepository.findById(activeSubscription.getPlanId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy gói subscription"));

        var candidateCount = examCandidateRepository.countByExamId(exam.getId());
        if (plan.getMaxStudentCount() != null && candidateCount > plan.getMaxStudentCount()) {
            throw new PlanLimitExceededException(
                "Số học sinh dự thi (" + candidateCount + ") vượt quá giới hạn của gói \"" + plan.getName()
                    + "\" (tối đa " + plan.getMaxStudentCount() + " học sinh), vui lòng nâng cấp gói"
            );
        }

        if (exam.getExamTimeDurationSecond() != null && plan.getMaxTimePerAttemptMin() != null
                && exam.getExamTimeDurationSecond() > plan.getMaxTimePerAttemptMin() * 60) {
            throw new PlanLimitExceededException(
                "Thời lượng bài thi (" + exam.getExamTimeDurationSecond() + " giây) vượt quá giới hạn của gói \""
                    + plan.getName() + "\" (tối đa " + plan.getMaxTimePerAttemptMin() + " phút/lượt thi)"
            );
        }

        // Ước lượng worst-case: mọi thí sinh dùng hết toàn bộ thời lượng bài thi cho mỗi lượt làm bài,
        // soi trước hạn mức GRADING (và với bài trên lớp, cả CLASS_TEST + hạn mức cá nhân giáo viên) --
        // xem ClassTestTokenQuotaGuardService để rõ vì sao phải soi cả 3 chỗ.
        classTestTokenQuotaGuardService.requireWithinTokenQuota(exam);
    }

    /**
     * Bài kiểm tra trên lớp cũng thi trong phòng có giám khảo như kỳ thi tập trung, nên trước khi
     * công bố lịch phải đủ: ca thi có phòng, có giám khảo, và mọi học sinh đã được xếp ca + có đề.
     *
     * <p>Chọn lớp lúc tạo chỉ nạp học sinh vào danh sách dự thi; xếp ca là bước riêng, nên nếu
     * không chặn ở đây thì bài vẫn lên lịch được với cả lớp chưa có ca và không ai vào thi được.
     */
    private void requireClassTestScheduleReadiness(
            Exam exam,
            List<ExamSchedule> schedules) {
        requireSchedulesHaveRoomAndProctor(schedules);

        var candidates = examCandidateRepository.findByExamId(exam.getId());
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
     * chạy thật. {@code findByExamId} chỉ lọc sẵn DELETED nên phải lọc thêm ở đây.
     */
    private List<ExamSchedule> effectiveSchedules(UUID examId) {
        return examScheduleRepository.findByExamId(examId).stream()
            .filter(schedule -> schedule.getStatus() != null && schedule.getStatus().isInEffect())
            .toList();
    }

    /** Ca thi còn hiệu lực nào cũng phải có phòng và ít nhất một giám khảo — đúng cho cả hai loại bài. */
    private void requireSchedulesHaveRoomAndProctor(
            List<ExamSchedule> schedules) {
        for (var schedule : schedules) {
            if (schedule.getSchoolRoomId() == null) {
                throw new IllegalStateException("Ca thi chưa được chọn phòng");
            }
            if (examScheduleProctorRepository.countByScheduleId(schedule.getId()) == 0) {
                throw new IllegalStateException("Ca thi chưa có giám khảo");
            }
        }
    }

    /**
     * Kỳ thi tập trung trước đây lên lịch được cả khi chưa có ca thi, chưa có thí sinh và chưa có mã
     * đề nào — bài mang trạng thái SCHEDULED mà thực tế không ai vào thi được.
     *
     * <p>Chốt ở đây nhẹ hơn bài trên lớp một bậc: KHÔNG đòi từng thí sinh đã có ca và đã có đề, vì kỳ
     * thi tập trung xếp ca ({@code AssignExamCandidateSchedule}/{@code AutoFill}) và phân đề
     * ({@code AssignExamPapersUseCase}, đòi mọi đề LOCKED) sau khi đã lên lịch. Chỉ chặn đúng trường
     * hợp "rỗng" mà không có cách nào chạy được.
     *
     * <p>Ngoài ra mọi ca thi phải đã được công bố: ca còn DRAFT thì học sinh và giám thị chưa nhìn
     * thấy, kỳ thi "đã lên lịch" mà có ca không ai vào được.
     */
    private void requireCentralizedScheduleReadiness(Exam exam) {
        var schedules = effectiveSchedules(exam.getId());
        if (schedules.isEmpty()) {
            throw new IllegalStateException("Kỳ thi chưa có ca thi nào, không thể lên lịch");
        }
        requireSchedulesHaveRoomAndProctor(schedules);
        if (examCandidateRepository.countByExamId(exam.getId()) == 0) {
            throw new IllegalStateException("Kỳ thi chưa có thí sinh nào, không thể lên lịch");
        }
        if (examPaperRepository.findByExamId(exam.getId()).isEmpty()) {
            throw new IllegalStateException("Kỳ thi chưa có mã đề nào, không thể lên lịch");
        }
        // Công bố từng ca là thao tác riêng (UpdateExamScheduleStatusUseCase) và không đòi trạng thái
        // kỳ thi, nên chặn ở đây không khoá luồng: công bố hết ca xong mới lên lịch được kỳ thi.
        var draftScheduleCount = schedules.stream()
            .filter(schedule -> schedule.getStatus() == ExamScheduleStatus.DRAFT)
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
     * ({@code UpdateExamScheduleStatusUseCase}, không phân biệt loại bài), còn lên lịch bài chỉ chốt
     * lại kết quả của thao tác đó — đúng như {@link #requireCentralizedScheduleReadiness}.
     */
    private void requireClassTestScheduleReady(Exam exam) {
        requireClassTestScheduleWindow(exam);
        var schedules = effectiveSchedules(exam.getId());
        if (schedules.isEmpty()) {
            throw new IllegalStateException("Bài kiểm tra trên lớp chưa có lịch");
        }
        requireClassTestScheduleReadiness(exam, schedules);
        var draftScheduleCount = schedules.stream()
            .filter(schedule -> schedule.getStatus() == ExamScheduleStatus.DRAFT)
            .count();
        if (draftScheduleCount > 0) {
            throw new IllegalStateException(
                "Còn " + draftScheduleCount + " ca thi chưa được công bố, không thể lên lịch bài kiểm tra");
        }
    }

    private void requireClassTestScheduleWindow(Exam exam) {
        if (exam.getOpenAt() == null || exam.getCloseAt() == null) {
            throw new IllegalStateException("Bài kiểm tra trên lớp phải có thời gian mở bài và đóng bài trước khi lên lịch");
        }
        if (!exam.getOpenAt().isBefore(exam.getCloseAt())) {
            throw new IllegalStateException("Thời gian mở bài phải nhỏ hơn thời gian đóng bài");
        }
        // Ca thi của bài trên lớp bám đúng khung mở/đóng, nên khung phải đủ cho thời gian làm bài.
        // Đặt ở đây (không phải CreateClassTestUseCase.createDraftSchedule) vì lúc tạo ca thi thì
        // examTimeDurationSecond còn là giá trị thô người dùng nhập, chưa qua recalculate.
        if (exam.isScheduleWindowShorterThanExamTime(exam.getOpenAt(), exam.getCloseAt())) {
            throw new IllegalStateException(ExamScheduleWindowMessages.schedulesNoLongerFit(1, exam));
        }
    }

    private void requireClassTestCanStart(Exam exam) {
        var now = Instant.now();
        if (exam.getCloseAt() != null && !now.isBefore(exam.getCloseAt())) {
            throw new IllegalStateException("Bài kiểm tra trên lớp đã quá thời gian đóng bài");
        }
    }

    /**
     * G.3: chỉ cho publish khi MỌI ExamCandidateResult của kỳ thi đều đã ở đúng RELEASED
     * hoặc INVALID (2 trạng thái "đã xử lý xong, sẵn sàng chốt") - còn bất kỳ trạng thái
     * nào khác (PENDING_REVIEW chưa duyệt, FINAL/APPEALED/RE_GRADING/RETAKE_REQUIRED từ
     * luồng phúc khảo/nghi vấn) đều chặn publish cho tới khi được xử lý dứt điểm.
     */
    private void requirePublishReadiness(UUID examId) {
        var missingResultCount = examCandidateRepository.findByExamId(examId).stream()
            .filter(candidate -> !ExamCandidateStatusSupport.isNonScorable(candidate.getStatus()))
            .flatMap(candidate -> examSessionRepository.findAllByCandidateId(candidate.getId()).stream())
            .filter(session -> examId.equals(session.getExamId()))
            .filter(session -> session.getStatus() != ExamSessionStatus.IN_PROGRESS
                && session.getStatus() != ExamSessionStatus.INTERRUPTED)
            .filter(session -> examCandidateResultRepository.findBySessionId(session.getId()).isEmpty())
            .count();
        if (missingResultCount > 0) {
            throw new IllegalStateException(
                "Còn " + missingResultCount + " phiên thi chưa có kết quả, không thể công bố kết quả");
        }

        var notReadyCount = examCandidateResultRepository.findByExamId(examId).stream()
            .filter(result -> result.getStatus() != ExamCandidateResultStatus.RELEASED
                && result.getStatus() != ExamCandidateResultStatus.INVALID)
            .count();
        if (notReadyCount > 0) {
            throw new IllegalStateException(
                "Còn " + notReadyCount + " kết quả chưa ở trạng thái RELEASED hoặc INVALID, không thể công bố kết quả");
        }
    }

    /**
     * G.3: chốt kết quả cho mọi ExamCandidateResult của kỳ thi ngay khi vừa chuyển
     * RESULTS_PUBLISHED - INVALID -> FAILED (điểm ép về 0), RELEASED/FINAL -> PASSED/FAILED
     * theo passingScore nếu policy có ngưỡng, hoặc -> FINAL nếu không có ngưỡng (nhà trường tự
     * chọn PASSED/FAILED sau qua decideExamCandidateResultOutcome). Xem
     * ExamCandidateResultFinalizationService.finalizeForPublish.
     */
    private void finalizePassFailForExam(Exam exam) {
        var passingScore = exam.getAssessmentPolicyId() == null
            ? null
            : assessmentPolicyRepository.findById(exam.getAssessmentPolicyId())
                .map(policy -> policy.getPassingScore())
                .orElse(null);

        for (var result : examCandidateResultRepository.findByExamId(exam.getId())) {
            if (examCandidateResultFinalizationService.finalizeForPublish(result, passingScore)) {
                examCandidateResultRepository.save(result);
            }
        }
    }

    private void lockClassTestPapers(UUID examId, UUID currentUserId) {
        var now = Instant.now();
        for (var paper : examPaperRepository.findByExamId(examId)) {
            if (paper.getStatus() != ExamPaperStatus.LOCKED) {
                paper.setStatus(ExamPaperStatus.LOCKED);
                paper.setUpdatedAt(now);
                paper.setUpdatedBy(currentUserId);
                examPaperRepository.save(paper);
            }
        }
    }
}
