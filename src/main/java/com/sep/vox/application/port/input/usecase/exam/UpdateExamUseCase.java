package com.sep.vox.application.port.input.usecase.exam;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateExamCommand;
import com.sep.vox.application.port.input.service.ClassTestTokenQuotaGuardService;
import com.sep.vox.application.port.input.service.ExamAssessmentPolicyValidator;
import com.sep.vox.application.port.input.service.ExamStreamConfigResolver;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.dto.ExamDto;
import com.sep.vox.domain.mapper.ExamDtoMapper;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.exam.ExamScheduleStatus;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamScheduleRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.service.exam.ExamEditingGuard;
import com.sep.vox.domain.service.exam.ExamScheduleWindowMessages;

@Service
public class UpdateExamUseCase implements IUseCase<UpdateExamCommand, ExamDto> {

    private final ExamRepository examRepository;
    private final ExamMemberRepository examMemberRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final ExamAssessmentPolicyValidator examAssessmentPolicyValidator;
    private final ExamStreamConfigResolver examStreamConfigResolver;
    private final UserContextPort userContextPort;
    private final ClassTestTokenQuotaGuardService classTestTokenQuotaGuardService;

    public UpdateExamUseCase(
            ExamRepository examRepository,
            ExamMemberRepository examMemberRepository,
            ExamScheduleRepository examScheduleRepository,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository,
            ExamAssessmentPolicyValidator examAssessmentPolicyValidator,
            ExamStreamConfigResolver examStreamConfigResolver,
            UserContextPort userContextPort,
            ClassTestTokenQuotaGuardService classTestTokenQuotaGuardService) {
        this.examAssessmentPolicyValidator = examAssessmentPolicyValidator;
        this.examStreamConfigResolver = examStreamConfigResolver;
        this.examRepository = examRepository;
        this.examMemberRepository = examMemberRepository;
        this.examScheduleRepository = examScheduleRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.userContextPort = userContextPort;
        this.classTestTokenQuotaGuardService = classTestTokenQuotaGuardService;
    }

    @Override
    @Transactional
    public ExamDto execute(UpdateExamCommand input) {
        var command = normalize(input);
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        var schoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));

        var exam = examRepository.findById(command.examId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));

        authorizeMutation(exam.getId(), exam.getSchoolId(), exam.getKind(), currentUserId, currentSchoolId, schoolAdmin);
        if (exam.getKind() == ExamKind.CLASS_TEST
                && exam.getStatus() != ExamStatus.DRAFT
                && exam.getStatus() != ExamStatus.SCHEDULED) {
            throw new IllegalStateException("Chỉ được cập nhật bài kiểm tra trên lớp khi chưa bắt đầu");
        }
        // Kỳ thi thường: thí sinh đã vào phòng thi nên mọi thay đổi thông tin đều làm lệch dữ liệu
        // đang chạy (khung giờ, thời gian làm bài, cách chốt điểm).
        ExamEditingGuard.requireExamEditable(exam);

        if (command.name() != null) {
            exam.setName(command.name());
        }
        if (command.description() != null) {
            exam.setDescription(command.description());
        }
        if (command.openAt() != null) {
            exam.setOpenAt(DateMapper.toInstant(command.openAt()));
        }
        if (command.closeAt() != null) {
            exam.setCloseAt(DateMapper.toInstant(command.closeAt()));
        }
        if (command.assessmentPolicyId() != null) {
            // Kiểm theo trường của chính bài kiểm tra, không phải trường người gọi: bài đã tồn tại nên
            // schoolId của nó mới là phạm vi đúng.
            examAssessmentPolicyValidator.validateIfPresent(command.assessmentPolicyId(), exam.getSchoolId());
            exam.setAssessmentPolicyId(command.assessmentPolicyId());
        }
        if (command.maxAttempt() != null) {
            exam.setMaxAttempt(command.maxAttempt());
        }
        if (command.examTimeDurationSecond() != null) {
            exam.setExamTimeDurationSecond(command.examTimeDurationSecond());
        }
        // Bài trên lớp đã publish (SCHEDULED) vẫn sửa được duration/maxAttempt, nhưng đó là 2 input
        // của ước lượng token đã soi lúc publish (UpdateExamStatusUseCase.validatePlanLimits) -- sửa
        // xong mà không soi lại thì ước lượng cũ vô nghĩa, nên phải chạy lại đúng guard đó ở đây.
        if (exam.getKind() == ExamKind.CLASS_TEST
                && exam.getStatus() == ExamStatus.SCHEDULED
                && (command.maxAttempt() != null || command.examTimeDurationSecond() != null)) {
            classTestTokenQuotaGuardService.requireWithinTokenQuota(exam);
        }
        if (command.resultDecisionMethod() != null) {
            exam.setResultDecisionMethod(command.resultDecisionMethod());
        }
        if (command.requiresOtp() != null) {
            exam.setRequiresOtp(command.requiresOtp());
        }
        if (command.aiConfidenceThresholdPercent() != null) {
            exam.setAiConfidenceThresholdPercent(command.aiConfidenceThresholdPercent());
        }
        // null = giữ nguyên (patch semantics như mọi trường khác); danh sách RỖNG = tắt giám sát.
        // Phân biệt này là bắt buộc: lúc tạo, null nghĩa là "không giám sát", còn ở đây null phải
        // mang nghĩa "không đụng tới" thì sửa tên mới không vô tình xoá cấu hình giám sát.
        if (command.requiredStreamTypes() != null) {
            var streamConfig = examStreamConfigResolver.resolve(
                command.requiredStreamTypes(), command.streamTypePermission());
            exam.setRequiredStreamType(streamConfig.requiredStreamType());
            exam.setStreamTypePermission(streamConfig.streamTypePermission());
        }
        if (exam.getKind() == ExamKind.CLASS_TEST) {
            requireClassTestScheduleWindow(exam.getOpenAt(), exam.getCloseAt());
            // Khung mở/đóng sẽ được đồng bộ thẳng xuống ca thi ở dưới, nên phải đủ dài cho bài làm.
            if (touchesScheduleTiming(command)
                    && exam.isScheduleWindowShorterThanExamTime(exam.getOpenAt(), exam.getCloseAt())) {
                throw new IllegalStateException(ExamScheduleWindowMessages.schedulesNoLongerFit(1, exam));
            }
        } else {
            validateOpenClose(exam.getOpenAt(), exam.getCloseAt());
            requireExistingSchedulesStillFit(exam, command);
        }
        var now = Instant.now();
        if (exam.getKind() == ExamKind.CLASS_TEST && (command.openAt() != null || command.closeAt() != null)) {
            syncClassTestDraftSchedules(exam, currentUserId, now);
        }
        exam.setUpdatedAt(now);
        exam.setUpdatedBy(currentUserId);
        return ExamDtoMapper.toDto(examRepository.save(exam));
    }

    private void syncClassTestDraftSchedules(com.sep.vox.domain.model.exam.Exam exam, java.util.UUID currentUserId, Instant now) {
        for (var schedule : examScheduleRepository.findByExamId(exam.getId())) {
            if (schedule.getStatus() == ExamScheduleStatus.DRAFT || schedule.getStatus() == ExamScheduleStatus.PUBLISHED) {
                schedule.setStartDate(exam.getOpenAt());
                schedule.setEndDate(exam.getCloseAt());
                schedule.setUpdatedAt(now);
                schedule.setUpdatedBy(currentUserId);
                examScheduleRepository.save(schedule);
            }
        }
    }

    private UpdateExamCommand normalize(UpdateExamCommand input) {
        return new UpdateExamCommand(
            input.examId(),
            StringNormalization.trimAndCollapseSpaces(input.name()),
            StringNormalization.trimAndCollapseSpaces(input.description()),
            input.openAt(),
            input.closeAt(),
            input.assessmentPolicyId(),
            input.maxAttempt(),
            input.examTimeDurationSecond(),
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
            // Chủ tịch hội đồng chạy trọn quy trình kỳ thi tập trung trên trang chi tiết (lập hội đồng →
            // chốt khung đề → ra đề → xếp lịch), nên phải sửa được thông tin kỳ thi như quản trị trường.
            if (examMemberRepository.existsByExamIdAndUserIdAndRole(examId, currentUserId, ExamMemberRole.CHAIR)) {
                return;
            }
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
        if (!examMemberRepository.existsByExamIdAndUserIdAndRole(examId, currentUserId, ExamMemberRole.CHAIR)) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
        if (examRepository.existsSubmittedSessionByExamId(examId)) {
            throw new IllegalStateException("Không thể cập nhật bài kiểm tra khi đã có bài nộp");
        }
    }

    /**
     * Kỳ thi thường: ca thi là lịch độc lập (có phòng, giám thị, thí sinh đã gán) nên không được tự
     * dời theo kỳ thi -- thay vào đó chặn nếu khung giờ hoặc thời gian làm bài mới làm ca thi đã lên
     * lịch không còn hợp lệ. Chỉ xét ca còn sẽ diễn ra; COMPLETED/MOVED/CANCELLED là quá khứ
     * (findByExamId đã loại DELETED sẵn).
     */
    /**
     * Chỉ kiểm lại bất biến ca thi khi thao tác thực sự đụng tới thời gian; sửa tên/mô tả không được
     * bị chặn chỉ vì dữ liệu sẵn có đang lệch.
     */
    private boolean touchesScheduleTiming(UpdateExamCommand command) {
        return command.openAt() != null
            || command.closeAt() != null
            || command.examTimeDurationSecond() != null;
    }

    private void requireExistingSchedulesStillFit(com.sep.vox.domain.model.exam.Exam exam, UpdateExamCommand command) {
        if (!touchesScheduleTiming(command)) {
            return;
        }
        var activeSchedules = examScheduleRepository.findByExamId(exam.getId()).stream()
            .filter(schedule -> schedule.getStatus() == ExamScheduleStatus.DRAFT
                || schedule.getStatus() == ExamScheduleStatus.PUBLISHED)
            .toList();

        var outsideCount = activeSchedules.stream()
            .filter(schedule -> exam.isScheduleWindowOutsideExamWindow(schedule.getStartDate(), schedule.getEndDate()))
            .count();
        if (outsideCount > 0) {
            throw new IllegalStateException(ExamScheduleWindowMessages.schedulesOutsideNewWindow((int) outsideCount));
        }

        var tooShortCount = activeSchedules.stream()
            .filter(schedule -> exam.isScheduleWindowShorterThanExamTime(schedule.getStartDate(), schedule.getEndDate()))
            .count();
        if (tooShortCount > 0) {
            throw new IllegalStateException(ExamScheduleWindowMessages.schedulesNoLongerFit((int) tooShortCount, exam));
        }
    }


    private void validateOpenClose(Instant openAt, Instant closeAt) {
        if (openAt == null || closeAt == null) {
            return;
        }
        if (!openAt.isBefore(closeAt)) {
            throw new IllegalStateException("Thời gian mở bài phải nhỏ hơn thời gian đóng bài");
        }
    }

    private void requireClassTestScheduleWindow(Instant openAt, Instant closeAt) {
        if (openAt == null || closeAt == null) {
            throw new IllegalStateException("Bài kiểm tra trên lớp phải có thời gian mở bài và đóng bài");
        }
        validateOpenClose(openAt, closeAt);
    }
}
