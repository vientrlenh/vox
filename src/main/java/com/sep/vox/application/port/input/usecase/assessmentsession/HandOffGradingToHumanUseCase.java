package com.sep.vox.application.port.input.usecase.assessmentsession;

import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicy;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentSession;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.HandOffGradingToHumanCommand;
import com.sep.vox.application.port.input.service.AssessmentSessionModerationAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.assessment.AssessmentSessionStatus;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.AssessmentItemResponseRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentSessionRepository;

/**
 * An attempt whose AI grading failed -> hand it to a human grader.
 *
 * <p><strong>Why a separate use case instead of reusing a grading assignment:</strong> a grading
 * assignment is attached to {@code assessment_candidate_results}, but a {@code GRADING_FAILED} session <em>has no result
 * row at all</em> -- measured 2026-08-18 on session 01a015a8: 2 answers, 0 evaluations,
 * {@code result_id} null. With no result there is nothing to assign, so the grading queue never
 * sees this attempt and it stays forever on the "Grading failed" screen.
 *
 * <p>The only job here is to <em>create the attempt to be assigned</em>: a {@code PENDING_REVIEW} result row
 * with no score. {@code GradingRoundPolicy.assignableStatuses(INITIAL)} equals exactly
 * {@code {PENDING_REVIEW}}, so right afterwards the attempt appears in the queue as UNASSIGNED
 * (the {@code unassignedOnly} predicate of JpaAssessmentGradingQueryRepository is "no assignment points
 * to this result") and the school assigns it to a teacher like any other attempt.
 *
 * <p><strong>It does not choose a teacher itself:</strong> who to assign to is a coordination decision of the
 * school -- who is free, who teaches that class, who graded this attempt in the previous round. Guessing here would hand the attempt
 * to someone who perhaps should not take it.
 *
 * <p><strong>It does NOT grade 0 and does NOT change the session status.</strong> The session stays
 * {@code GRADING_FAILED} because that is the truth: the AI's grading failed. That way the regrade-with-AI button can still be
 * used -- the two exits are not mutually exclusive, and the user can try the AI again after putting it into
 * the human grader queue.
 */
@Service
public class HandOffGradingToHumanUseCase implements IUseCase<HandOffGradingToHumanCommand, UUID> {

    private static final Logger LOGGER = LoggerFactory.getLogger(HandOffGradingToHumanUseCase.class);

    private final AssessmentSessionRepository assessmentSessionRepository;
    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private final AssessmentItemResponseRepository assessmentItemResponseRepository;
    private final AssessmentPolicyRepository assessmentPolicyRepository;
    private final AssessmentSessionModerationAccessService moderationAccessService;

    public HandOffGradingToHumanUseCase(
            AssessmentSessionRepository assessmentSessionRepository,
            AssessmentCandidateRepository assessmentCandidateRepository,
            AssessmentRepository assessmentRepository,
            AssessmentCandidateResultRepository assessmentCandidateResultRepository,
            AssessmentItemResponseRepository assessmentItemResponseRepository,
            AssessmentPolicyRepository assessmentPolicyRepository,
            AssessmentSessionModerationAccessService moderationAccessService) {
        this.assessmentSessionRepository = assessmentSessionRepository;
        this.assessmentCandidateRepository = assessmentCandidateRepository;
        this.assessmentRepository = assessmentRepository;
        this.assessmentCandidateResultRepository = assessmentCandidateResultRepository;
        this.assessmentItemResponseRepository = assessmentItemResponseRepository;
        this.assessmentPolicyRepository = assessmentPolicyRepository;
        this.moderationAccessService = moderationAccessService;
    }

    @Override
    @Transactional
    public UUID execute(HandOffGradingToHumanCommand input) {
        AssessmentSession session = assessmentSessionRepository.findById(input.sessionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên thi"));
        AssessmentCandidate candidate = assessmentCandidateRepository.findById(session.getCandidateId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy thí sinh của phiên thi"));
        Assessment assessment = assessmentRepository.findById(session.getAssessmentId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy kỳ thi của phiên thi"));

        moderationAccessService.authorize(assessment, candidate);
        if (assessment.getStatus() == AssessmentStatus.RESULTS_PUBLISHED) {
            throw new IllegalStateException("Kỳ thi đã công bố điểm, không thể chuyển sang chấm tay");
        }
        if (session.getStatus() != AssessmentSessionStatus.GRADING_FAILED) {
            throw new IllegalStateException("Chỉ chuyển sang chấm tay được khi AI chấm lỗi");
        }

        // Không có câu trả lời thì không có gì để người chấm xem. Giao một bài trắng chỉ tạo ra
        // một việc không làm được: giáo viên mở ra, không thấy gì, và cũng không đóng được phân
        // công vì không chấm nổi.
        if (assessmentItemResponseRepository.findBySessionId(session.getId()).isEmpty()) {
            throw new IllegalStateException(
                "Phiên thi này không có câu trả lời nào, không có gì để người chấm xem");
        }

        AssessmentCandidateResult existing = assessmentCandidateResultRepository.findBySessionId(session.getId()).orElse(null);
        if (existing != null) {
            // Đã có kết quả nghĩa là bài đã ở trong vòng đời chấm rồi -- vào hàng đợi hoặc đang
            // được giao. Tạo thêm là sinh bài trùng; đổi trạng thái của nó là ghi đè quyết định
            // của người khác.
            LOGGER.info(
                "Phiên {} đã có kết quả {} ({}), không tạo lại.",
                session.getId(), existing.getId(), existing.getStatus());
            return existing.getId();
        }

        AssessmentPolicy policy = assessment.getAssessmentPolicyId() == null
            ? null
            : assessmentPolicyRepository.findById(assessment.getAssessmentPolicyId()).orElse(null);

        // The same set of fields as persistInvalidBlockedResult (SubmitAssessmentSessionUseCase): the rubric and
        // framework must be fixed RIGHT when it is created, because the grading screen reads the scale and the list of criteria
        // from the result's own rubricVersionId. If left null the teacher opens it and has no criteria
        // to enter.
        AssessmentCandidateResult result = new AssessmentCandidateResult();
        result.setAssessmentId(session.getAssessmentId());
        result.setCandidateId(session.getCandidateId());
        result.setSessionId(session.getId());
        result.setAssessmentPolicyId(policy == null ? null : policy.getId());
        result.setPolicyVersion(policy == null ? 0 : policy.getVersion());
        result.setRubricVersionId(policy == null ? null : policy.getRubricVersionId());
        result.setFrameworkVersionId(policy == null ? null : policy.getFrameworkVersionId());
        result.setTargetFrameworkBandId(policy == null ? null : policy.getTargetFrameworkBandId());
        result.setRubricResultBandId(null);
        // Điểm để TRỐNG, không phải 0. Chưa ai chấm thì chưa có điểm, và 0 ở đây sẽ đi thẳng vào
        // bảng điểm như một con điểm thật.
        result.setTotalScore(null);
        result.setStatus(AssessmentCandidateResultStatus.PENDING_REVIEW);

        AssessmentCandidateResult saved = assessmentCandidateResultRepository.save(result);
        LOGGER.info(
            "Phiên {} (AI chấm lỗi) chuyển sang chấm tay: tạo kết quả {} ở PENDING_REVIEW, "
                + "chờ nhà trường phân công.",
            session.getId(), saved.getId());
        return saved.getId();
    }
}
