package com.sep.vox.application.port.input.usecase.exam;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateExamCommand;
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

@Service
public class UpdateExamUseCase implements IUseCase<UpdateExamCommand, ExamDto> {

    private final ExamRepository examRepository;
    private final ExamMemberRepository examMemberRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final UserContextPort userContextPort;

    public UpdateExamUseCase(
            ExamRepository examRepository,
            ExamMemberRepository examMemberRepository,
            ExamScheduleRepository examScheduleRepository,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository,
            UserContextPort userContextPort) {
        this.examRepository = examRepository;
        this.examMemberRepository = examMemberRepository;
        this.examScheduleRepository = examScheduleRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.userContextPort = userContextPort;
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

        if (command.name() != null) {
            exam.setName(command.name());
        }
        if (command.description() != null) {
            exam.setDescription(command.description());
        }
        if (command.openAt() != null) {
            exam.setOpenAt(OffsetDateTime.parse(command.openAt()));
        }
        if (command.closeAt() != null) {
            exam.setCloseAt(OffsetDateTime.parse(command.closeAt()));
        }
        if (command.assessmentPolicyId() != null) {
            exam.setAssessmentPolicyId(command.assessmentPolicyId());
        }
        if (command.maxAttempt() != null) {
            exam.setMaxAttempt(command.maxAttempt());
        }
        if (command.examTimeDurationSecond() != null) {
            exam.setExamTimeDurationSecond(command.examTimeDurationSecond());
        }
        if (command.resultDecisionMethod() != null) {
            exam.setResultDecisionMethod(command.resultDecisionMethod());
        }
        if (command.requiresOtp() != null) {
            exam.setRequiresOtp(command.requiresOtp());
        }
        if (exam.getKind() == ExamKind.CLASS_TEST) {
            requireClassTestScheduleWindow(exam.getOpenAt(), exam.getCloseAt());
        } else {
            validateOpenClose(exam.getOpenAt(), exam.getCloseAt());
        }
        var now = OffsetDateTime.now();
        if (exam.getKind() == ExamKind.CLASS_TEST && (command.openAt() != null || command.closeAt() != null)) {
            syncClassTestDraftSchedules(exam, currentUserId, now);
        }
        exam.setUpdatedAt(now);
        exam.setUpdatedBy(currentUserId);
        return ExamDtoMapper.toDto(examRepository.save(exam));
    }

    private void syncClassTestDraftSchedules(com.sep.vox.domain.model.exam.Exam exam, java.util.UUID currentUserId, OffsetDateTime now) {
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
            input.requiresOtp()
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
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
        if (!examMemberRepository.existsByExamIdAndUserIdAndRole(examId, currentUserId, ExamMemberRole.CHAIR)) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
        if (examRepository.existsSubmittedSessionByExamId(examId)) {
            throw new IllegalStateException("Không thể cập nhật bài kiểm tra khi đã có bài nộp");
        }
    }

    private void validateOpenClose(OffsetDateTime openAt, OffsetDateTime closeAt) {
        if (openAt == null || closeAt == null) {
            return;
        }
        if (!openAt.isBefore(closeAt)) {
            throw new IllegalStateException("Thời gian mở bài phải nhỏ hơn thời gian đóng bài");
        }
    }

    private void requireClassTestScheduleWindow(OffsetDateTime openAt, OffsetDateTime closeAt) {
        if (openAt == null || closeAt == null) {
            throw new IllegalStateException("Bài kiểm tra trên lớp phải có thời gian mở bài và đóng bài");
        }
        validateOpenClose(openAt, closeAt);
    }
}
