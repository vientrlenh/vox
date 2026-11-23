package com.sep.vox.application.port.input.usecase.assessment;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.DeleteAssessmentCommand;
import com.sep.vox.application.port.input.service.AssessmentScheduleClosureService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.application.response.input.assessment.DeleteAssessmentResponse;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.AssessmentPaperItemRepository;
import com.sep.vox.domain.repository.AssessmentPaperRepository;
import com.sep.vox.domain.repository.AssessmentPaperSectionRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentScheduleProctorRepository;
import com.sep.vox.domain.repository.AssessmentScheduleRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

/**
 * Deletes an assessment. Only an assessment that is still DRAFT may be hard-deleted -- from scheduling onwards the sessions have been published
 * to students so the assessment must not disappear, it can only move to CANCELLED. This boundary matches
 * {@code UpdateAssessmentStatusUseCase.CANCELLABLE_FROM}: a published result cannot be taken back.
 *
 * <p>The hard-delete branch has to clean up all child data itself: no table has an FK or ON DELETE CASCADE pointing back to
 * {@code assessments}, so missing a table leaves orphan sessions/candidates -- they drop out of every screen that
 * uses a JOIN but still show up on any screen that loads by id, for example the student's assessment schedule.
 */
@Service
@RequiredArgsConstructor
public class DeleteAssessmentUseCase implements IUseCase<DeleteAssessmentCommand, DeleteAssessmentResponse> {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentScheduleRepository assessmentScheduleRepository;
    private final AssessmentScheduleProctorRepository assessmentScheduleProctorRepository;
    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final AssessmentPaperRepository assessmentPaperRepository;
    private final AssessmentPaperSectionRepository assessmentPaperSectionRepository;
    private final AssessmentPaperItemRepository assessmentPaperItemRepository;
    private final AssessmentMemberRepository assessmentMemberRepository;
    private final AssessmentQuestionSecureLockService assessmentQuestionSecureLockService;
    private final AssessmentScheduleClosureService assessmentScheduleClosureService;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public DeleteAssessmentResponse execute(DeleteAssessmentCommand input) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        UUID currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        boolean schoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));

        Assessment assessment = assessmentRepository.findById(input.assessmentId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));

        authorizeDelete(assessment.getId(), assessment.getSchoolId(), assessment.getKind(), currentUserId, currentSchoolId, schoolAdmin);

        return switch (assessment.getStatus()) {
            case RESULTS_PUBLISHED ->
                throw new IllegalStateException("Không thể xoá kỳ thi đã công bố kết quả");
            // Đã huỷ rồi thì bấm xoá lần nữa không có gì để làm: trả đúng kết quả cũ thay vì ném lỗi.
            case CANCELLED -> new DeleteAssessmentResponse(false, true);
            case DRAFT -> {
                cascadeDelete(assessment.getId(), currentUserId);
                yield new DeleteAssessmentResponse(true, false);
            }
            default -> {
                cancel(assessment);
                yield new DeleteAssessmentResponse(false, true);
            }
        };
    }

    private void cancel(Assessment assessment) {
        assessment.setStatus(AssessmentStatus.CANCELLED);
        assessmentRepository.save(assessment);
        // Cancelling an assessment must carry its sessions along -- see AssessmentScheduleClosureService for why.
        assessmentScheduleClosureService.cancelSchedulesForAssessment(assessment.getId());
    }

    /**
     * Xoá con trước cha. Không có FK nào chặn nên sai thứ tự hay bỏ sót bảng đều không báo lỗi —
     * chỉ lặng lẽ để lại dòng mồ côi.
     */
    private void cascadeDelete(UUID assessmentId, UUID currentUserId) {
        // Locked questions point into the assessment_secure_pools of the assessment: deleting the pool and forgetting to unlock leaves the questions
        // in the question bank in a permanently locked state.
        assessmentQuestionSecureLockService.releaseAllForAssessment(assessmentId, currentUserId);

        List<UUID> paperIds = assessmentPaperRepository.findByAssessmentId(assessmentId).stream()
            .map(paper -> paper.getId())
            .toList();
        if (!paperIds.isEmpty()) {
            assessmentPaperItemRepository.deleteByPaperIdIn(paperIds);
            assessmentPaperSectionRepository.deleteByPaperIdIn(paperIds);
            assessmentPaperRepository.deleteByAssessmentId(assessmentId);
        }

        // Deliberately uses findAllIdsByAssessmentId rather than findByAssessmentId: the latter filters out soft-deleted sessions,
        // and the proctors of those sessions have to be cleaned up too.
        List<UUID> scheduleIds = assessmentScheduleRepository.findAllIdsByAssessmentId(assessmentId);
        if (!scheduleIds.isEmpty()) {
            assessmentScheduleProctorRepository.deleteByScheduleIdIn(scheduleIds);
        }
        assessmentCandidateRepository.deleteByAssessmentId(assessmentId);
        assessmentScheduleRepository.deleteByAssessmentId(assessmentId);

        assessmentMemberRepository.deleteByAssessmentId(assessmentId);
        assessmentRepository.deleteById(assessmentId);
    }

    private void authorizeDelete(
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
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
        if (!assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessmentId, currentUserId, AssessmentMemberRole.CHAIR)) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
    }
}
