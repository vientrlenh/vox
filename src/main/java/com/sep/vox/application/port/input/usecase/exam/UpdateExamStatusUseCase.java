package com.sep.vox.application.port.input.usecase.exam;

import java.time.OffsetDateTime;

import com.sep.vox.domain.model.school.SchoolUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.PlanLimitExceededException;
import com.sep.vox.application.port.input.command.UpdateExamStatusCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.dto.ExamDto;
import com.sep.vox.domain.mapper.ExamDtoMapper;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.model.subscription.QuotaType;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;
import com.sep.vox.domain.repository.SubscriptionQuotaRepository;

@Service
public class UpdateExamStatusUseCase implements IUseCase<UpdateExamStatusCommand, ExamDto> {

    private final ExamRepository examRepository;
    private final ExamMemberRepository examMemberRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final ExamQuestionSecureLockService examQuestionSecureLockService;
    private final ExamCandidateRepository examCandidateRepository;
    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final SubscriptionQuotaRepository subscriptionQuotaRepository;
    private final UserContextPort userContextPort;

    public UpdateExamStatusUseCase(
            ExamRepository examRepository,
            ExamMemberRepository examMemberRepository,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository,
            ExamQuestionSecureLockService examQuestionSecureLockService,
            ExamCandidateRepository examCandidateRepository,
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            SubscriptionPlanRepository subscriptionPlanRepository,
            SubscriptionQuotaRepository subscriptionQuotaRepository,
            UserContextPort userContextPort) {
        this.examRepository = examRepository;
        this.examMemberRepository = examMemberRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.examQuestionSecureLockService = examQuestionSecureLockService;
        this.examCandidateRepository = examCandidateRepository;
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.subscriptionQuotaRepository = subscriptionQuotaRepository;
        this.userContextPort = userContextPort;
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
            .map(SchoolUser::getSchoolId)
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
            }
            case "START" -> requireTransition(exam, ExamStatus.SCHEDULED, ExamStatus.IN_PROGRESS);
            case "CLOSE" -> {
                requireTransition(exam, ExamStatus.IN_PROGRESS, ExamStatus.CLOSED);
                examQuestionSecureLockService.releaseIfAutoAfterClose(exam.getId());
            }
            case "PUBLISH_RESULTS" -> requireTransition(exam, ExamStatus.CLOSED, ExamStatus.RESULTS_PUBLISHED);
            case "CANCEL" -> exam.setStatus(ExamStatus.CANCELLED);
            default -> throw new IllegalStateException("Action không hợp lệ");
        }

        exam.setUpdatedAt(OffsetDateTime.now());
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
}
