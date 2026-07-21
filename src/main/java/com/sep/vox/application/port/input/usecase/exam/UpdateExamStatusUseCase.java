package com.sep.vox.application.port.input.usecase.exam;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateExamStatusCommand;
import com.sep.vox.application.port.input.service.ExamCandidateResultFinalizationService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.dto.ExamDto;
import com.sep.vox.domain.mapper.ExamDtoMapper;
import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.exam.ExamPaperStatus;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamPaperRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
public class UpdateExamStatusUseCase implements IUseCase<UpdateExamStatusCommand, ExamDto> {

    private final ExamRepository examRepository;
    private final ExamMemberRepository examMemberRepository;
    private final ExamPaperRepository examPaperRepository;
    private final ExamCandidateResultRepository examCandidateResultRepository;
    private final AssessmentPolicyRepository assessmentPolicyRepository;
    private final ExamCandidateResultFinalizationService examCandidateResultFinalizationService;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final ExamQuestionSecureLockService examQuestionSecureLockService;
    private final UserContextPort userContextPort;

    public UpdateExamStatusUseCase(
            ExamRepository examRepository,
            ExamMemberRepository examMemberRepository,
            ExamPaperRepository examPaperRepository,
            ExamCandidateResultRepository examCandidateResultRepository,
            AssessmentPolicyRepository assessmentPolicyRepository,
            ExamCandidateResultFinalizationService examCandidateResultFinalizationService,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository,
            ExamQuestionSecureLockService examQuestionSecureLockService,
            UserContextPort userContextPort) {
        this.examRepository = examRepository;
        this.examMemberRepository = examMemberRepository;
        this.examPaperRepository = examPaperRepository;
        this.examCandidateResultRepository = examCandidateResultRepository;
        this.assessmentPolicyRepository = assessmentPolicyRepository;
        this.examCandidateResultFinalizationService = examCandidateResultFinalizationService;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.examQuestionSecureLockService = examQuestionSecureLockService;
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
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        var schoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));

        var exam = examRepository.findById(command.examId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));

        authorizeMutation(exam.getId(), exam.getSchoolId(), exam.getKind(), currentUserId, currentSchoolId, schoolAdmin);

        switch (command.action()) {
            case "SCHEDULE" -> requireTransition(exam, ExamStatus.DRAFT, ExamStatus.SCHEDULED);
            case "START" -> {
                requireTransition(exam, ExamStatus.SCHEDULED, ExamStatus.IN_PROGRESS);
                if (exam.getKind() == ExamKind.CLASS_TEST) {
                    lockClassTestPapers(exam.getId(), currentUserId);
                }
            }
            case "CLOSE" -> {
                requireTransition(exam, ExamStatus.IN_PROGRESS, ExamStatus.CLOSED);
                examQuestionSecureLockService.releaseIfAutoAfterClose(exam.getId());
            }
            case "PUBLISH_RESULTS" -> {
                requirePublishReadiness(exam.getId());
                requireTransition(exam, ExamStatus.CLOSED, ExamStatus.RESULTS_PUBLISHED);
                finalizePassFailForExam(exam);
            }
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

    /**
     * G.3: chỉ cho publish khi mọi ExamCandidateResult của kỳ thi đã được xử lý xong
     * (RELEASED/INVALID, hoặc các trạng thái ngoài phạm vi như APPEALED/RE_GRADING) -
     * không còn cái nào PENDING_REVIEW (AI chưa tự tin, giáo viên chưa xác nhận).
     */
    private void requirePublishReadiness(java.util.UUID examId) {
        var pendingCount = examCandidateResultRepository.findByExamId(examId).stream()
            .filter(result -> result.getStatus() == ExamCandidateResultStatus.PENDING_REVIEW)
            .count();
        if (pendingCount > 0) {
            throw new IllegalStateException(
                "Còn " + pendingCount + " kết quả chưa được giáo viên xác nhận, không thể công bố kết quả");
        }
    }

    /**
     * G.3: chốt PASSED/FAILED cho mọi ExamCandidateResult của kỳ thi ngay khi vừa
     * chuyển RESULTS_PUBLISHED - INVALID -> FAILED (điểm ép về 0), RELEASED/FINAL ->
     * PASSED/FAILED theo passingScore. Trạng thái CHUNG THẨM, không đổi lại nữa.
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
        var now = OffsetDateTime.now();
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
