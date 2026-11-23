package com.sep.vox.application.port.input.usecase.assessmentsession;

import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import com.sep.vox.domain.model.assessment.AssessmentSession;
import java.time.Instant;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.assessmentsession.DeleteAssessmentSessionCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.repository.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentResultAppealRepository;
import com.sep.vox.domain.repository.AssessmentSessionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

/**
 * SOFT-deletes an assessment session: marks the session (and its result) as {@code DELETED} together with the time
 * and the reason -- used to remove a broken attempt caused by a faulty room entry or faulty grading.
 *
 * <p>The previous version HARD-deleted the whole data tree (answers, turns, evaluations, per-criterion scores,
 * results, appeals, grading assignments, status log, recordings). Three problems caused it to be replaced:
 *
 * <ul>
 *   <li>It cannot be undone and cannot be verified again. Deleting an attempt by mistake loses the evidence entirely, exactly when it is needed most:
 *       when a student questions the score.
 *   <li>A hand-written cascade over more than ten tables, with no FK to back it: missing one table leaves orphan rows.
 *   <li>Two foreign keys point into {@code assessment_sessions} that this cascade never cleans
 *       ({@code school_balance_entries.assessment_session_id}, {@code school_debt_events.trigger_assessment_session_id}),
 *       both {@code NO ACTION} -- deleting a session that has incurred AI cost violates the foreign key.
 *       Keeping the row leaves both references valid.
 * </ul>
 *
 * <p>The attempt data is kept AS IS, only hidden from the read flows. The school administrator and the council
 * chair can still see a deleted session (with a "Deleted" label and the reason) -- see {@code AssessmentSessionJpaEntity}.
 *
 * <p>Permission: SCHOOL_ADMIN may delete any session of their school; the chair may delete a session of an
 * assessment they chair, REGARDLESS of the assessment kind -- matching
 * {@code UpdateAssessmentUseCase.authorizeMutation}, where the chair of a centralized assessment can edit it like the
 * school administrator.
 *
 * <p>Previously the chair branch also required {@code CLASS_TEST} and cited {@code UpdateAssessmentUseCase} as its basis -- but that place
 * ALLOWS the chair of a centralized assessment, so that basis said the opposite of the condition it was defending. The old line was also self-contradictory in terms of
 * destructiveness: the chair of a centralized assessment could already delete CANDIDATES ({@code DeleteAssessmentCandidateUseCase},
 * {@code BulkDeleteAssessmentCandidatesUseCase} do not block by assessment kind), i.e. remove a whole
 * person, yet could not remove one broken attempt of that same person. This operation is far safer:
 * soft delete, a reason is required, and it closes once the assessment has been finalized.
 */
@Service
@RequiredArgsConstructor
public class DeleteAssessmentSessionUseCase implements IUseCase<DeleteAssessmentSessionCommand, Void> {

    private final AssessmentSessionRepository assessmentSessionRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentMemberRepository assessmentMemberRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final UserContextPort userContextPort;
    private final AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private final AssessmentResultAppealRepository assessmentResultAppealRepository;

    @Override
    @Transactional
    public Void execute(DeleteAssessmentSessionCommand input) {
        UUID sessionId = input.sessionId();
        AssessmentSession session = assessmentSessionRepository.findById(sessionId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiên thi"));
        Assessment assessment = assessmentRepository.findById(session.getAssessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài kiểm tra của phiên thi này"));

        authorizeDelete(assessment.getId(), assessment.getSchoolId());
        // Once an assessment has closed the books, the score has reached (or is about to reach) the student -- see Assessment#isResultsFinalized.
        if (assessment.isResultsFinalized()) {
            throw new IllegalArgumentException("Không thể xóa bài thi khi kỳ thi đã đóng hoặc đã công bố kết quả");
        }
        // Đơn phúc khảo đang mở là tranh chấp điểm mà học sinh đã chính thức nêu: xoá mềm kết quả
        // sẽ rút mất chính bài đang bị khiếu nại và để đơn treo trỏ vào một dòng đã ẩn.
        //
        // Chốt kỳ thi ở trên hiện đã chặn gần hết đường tới đây (đơn chỉ sinh ra sau khi điểm được
        // trả), nhưng nó canh vòng đời của KỲ THI còn đơn thì bám vào TỪNG bài — hai vòng đời khác
        // nhau, đừng để cái này ngầm suy ra cái kia. Nhánh chủ tịch vừa mở rộng làm số người bấm
        // được nút này đông hơn, nên chốt tường minh rẻ hơn là đi chứng minh nó bất khả thi.
        AssessmentCandidateResult candidateResult = assessmentCandidateResultRepository.findBySessionId(sessionId).orElse(null);
        if (candidateResult != null
                && assessmentResultAppealRepository.existsOpenByCandidateResultId(candidateResult.getId())) {
            throw new IllegalArgumentException(
                "Bài thi này đang có đơn phúc khảo chưa xử lý xong. Hãy xử lý hoặc từ chối đơn trước khi xoá.");
        }

        String reason = input.reason() == null ? "" : input.reason().strip();
        if (reason.isEmpty()) {
            throw new IllegalArgumentException("Phải nhập lý do xóa bài thi");
        }

        Instant now = Instant.now();
        // Idempotent: hai người cùng bấm xoá thì lần sau nhận 0 dòng, và lý do/mốc thời gian của lần
        // đầu — thứ phải giữ nguyên để giải trình — không bị ghi đè.
        if (assessmentSessionRepository.softDelete(sessionId, now, reason) == 0) {
            return null;
        }
        // Điểm phải biến mất khỏi bảng kết quả, hàng đợi chấm và phúc khảo CÙNG LÚC với phiên thi.
        assessmentCandidateResultRepository.softDeleteBySessionId(sessionId, now, reason);
        return null;
    }

    private void authorizeDelete(UUID assessmentId, UUID assessmentSchoolId) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        UUID currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        boolean schoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));

        if (schoolAdmin && currentSchoolId != null && currentSchoolId.equals(assessmentSchoolId)) {
            return;
        }
        if (assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessmentId, currentUserId, AssessmentMemberRole.CHAIR)) {
            return;
        }
        throw new ForbiddenException("Quyền truy cập bị từ chối");
    }
}
