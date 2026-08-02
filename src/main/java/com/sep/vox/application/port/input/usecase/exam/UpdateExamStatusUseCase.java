package com.sep.vox.application.port.input.usecase.exam;

import java.time.Instant;

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
import com.sep.vox.domain.model.exam.ExamScheduleStatus;
import com.sep.vox.domain.model.exam.ExamSessionStatus;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.model.subscription.QuotaType;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamPaperRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamScheduleRepository;
import com.sep.vox.domain.repository.ExamSessionRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;
import com.sep.vox.domain.repository.SubscriptionQuotaRepository;

@Service
public class UpdateExamStatusUseCase implements IUseCase<UpdateExamStatusCommand, ExamDto> {

    private final ExamRepository examRepository;
    private final ExamMemberRepository examMemberRepository;
    private final ExamPaperRepository examPaperRepository;
    private final ExamScheduleRepository examScheduleRepository;
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
    private final SubscriptionQuotaRepository subscriptionQuotaRepository;
    private final UserContextPort userContextPort;
    private final EventPublisherPort eventPublisherPort;

    public UpdateExamStatusUseCase(
            ExamRepository examRepository,
            ExamMemberRepository examMemberRepository,
            ExamPaperRepository examPaperRepository,
            ExamScheduleRepository examScheduleRepository,
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
            SubscriptionQuotaRepository subscriptionQuotaRepository,
            UserContextPort userContextPort,
            EventPublisherPort eventPublisherPort) {
        this.examRepository = examRepository;
        this.examMemberRepository = examMemberRepository;
        this.examPaperRepository = examPaperRepository;
        this.examScheduleRepository = examScheduleRepository;
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
        this.subscriptionQuotaRepository = subscriptionQuotaRepository;
        this.userContextPort = userContextPort;
        this.eventPublisherPort = eventPublisherPort;
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

        switch (command.action()) {
            case "SCHEDULE" -> {
                validatePlanLimits(exam);
                requireTransition(exam, ExamStatus.DRAFT, ExamStatus.SCHEDULED);
                if (exam.getKind() == ExamKind.CLASS_TEST) {
                    publishClassTestSchedules(exam, currentUserId);
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
                examQuestionSecureLockService.releaseIfAutoAfterClose(exam.getId());
                zeroScoreExamResultService.ensureZeroResultsForMissingOrEmptyAttempts(exam.getId());
            }
            case "PUBLISH_RESULTS" -> {
                zeroScoreExamResultService.ensureZeroResultsForMissingOrEmptyAttempts(exam.getId());
                requirePublishReadiness(exam.getId());
                requireTransition(exam, ExamStatus.CLOSED, ExamStatus.RESULTS_PUBLISHED);
                finalizePassFailForExam(exam);
                eventPublisherPort.publish(new ExamResultsPublishedEvent(exam.getId()));
            }
            case "CANCEL" -> exam.setStatus(ExamStatus.CANCELLED);
            default -> throw new IllegalStateException("Action không hợp lệ");
        }

        exam.setUpdatedAt(Instant.now());
        exam.setUpdatedBy(currentUserId);
        return ExamDtoMapper.toDto(examRepository.save(exam));
    }

    private void authorizeMutation(
            java.util.UUID examId,
            java.util.UUID examSchoolId,
            ExamKind kind,
            java.util.UUID currentUserId,
            java.util.UUID currentSchoolId,
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

    private void requireTransition(com.sep.vox.domain.model.exam.Exam exam, ExamStatus from, ExamStatus to) {
        if (exam.getStatus() != from) {
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

        if (exam.getExamTimeDurationSecond() != null) {
            // Ước lượng worst-case: mọi thí sinh dùng hết toàn bộ thời lượng bài thi cho mỗi lượt làm bài.
            //Công thức worst-case: thời lượng bài thi (giây) × số thí sinh × maxAttempt — giả định mọi thí sinh dùng hết toàn bộ thời gian cho mỗi lượt thi được phép, so với token GRADING
            //còn lại (totalAllocated - usedQuantity) của subscription đang active. Nếu vượt, chặn SCHEDULE và báo lỗi gợi ý mua thêm token hoặc nâng cấp gói
            var estimatedTokens = (long) exam.getExamTimeDurationSecond() * candidateCount * exam.getMaxAttempt();
            var quota = subscriptionQuotaRepository.findBySubscriptionIdAndQuotaType(activeSubscription.getId(), QuotaType.GRADING)
                .orElseThrow(() -> new PlanLimitExceededException("Không tìm thấy hạn mức token của gói đăng ký"));
            var remaining = quota.getTotalAllocated() - quota.getUsedQuantity();
            if (estimatedTokens > remaining) {
                throw new PlanLimitExceededException(
                    "Số token ước tính cần dùng (" + estimatedTokens + ") vượt quá số token còn lại ("
                        + remaining + "), vui lòng mua thêm token hoặc nâng cấp gói"
                );
            }
        }
    }

    private void publishClassTestSchedules(com.sep.vox.domain.model.exam.Exam exam, java.util.UUID currentUserId) {
        requireClassTestScheduleWindow(exam);
        var schedules = examScheduleRepository.findByExamId(exam.getId());
        if (schedules.isEmpty()) {
            throw new IllegalStateException("Bài kiểm tra trên lớp chưa có lịch");
        }
        var now = Instant.now();
        for (var schedule : schedules) {
            if (schedule.getStatus() == ExamScheduleStatus.DRAFT) {
                schedule.setStatus(ExamScheduleStatus.PUBLISHED);
                schedule.setUpdatedAt(now);
                schedule.setUpdatedBy(currentUserId);
                examScheduleRepository.save(schedule);
            }
        }
    }

    private void requireClassTestScheduleWindow(com.sep.vox.domain.model.exam.Exam exam) {
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

    private void requireClassTestCanStart(com.sep.vox.domain.model.exam.Exam exam) {
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
    private void requirePublishReadiness(java.util.UUID examId) {
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
    private void finalizePassFailForExam(com.sep.vox.domain.model.exam.Exam exam) {
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

    private void lockClassTestPapers(java.util.UUID examId, java.util.UUID currentUserId) {
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
