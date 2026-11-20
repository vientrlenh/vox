package com.sep.vox.application.port.input.usecase.assessmentgrading;

import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import java.time.Instant;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.event.AssessmentResultInvalidClearedPayloadV1;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.GradingDecisionCommand;
import com.sep.vox.application.port.input.service.GradingActionSupport;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.response.input.assessmentgrading.GradingActionResponse;
import com.sep.vox.domain.common.AggregateTypeConstant;
import com.sep.vox.domain.common.EventTypeConstant;
import com.sep.vox.domain.model.assessment.AssessmentGradingAssignment;
import com.sep.vox.domain.model.assessment.GradingOutcome;
import com.sep.vox.domain.model.assessment.GradingRoundType;
import com.sep.vox.domain.model.outbox.Outbox;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentGradingAssignmentRepository;
import com.sep.vox.domain.repository.OutboxRepository;

/**
 * A teacher re-inspects an invalidated attempt and concludes there was NO violation: the attempt returns to PENDING_REVIEW,
 * the candidate is unblocked, and a new {@code INITIAL} round is opened right away for that same teacher.
 *
 * <p><strong>Why it opens a new round itself:</strong> an attempt that has just returned to PENDING_REVIEW has no
 * valid score and needs to be graded; the person who holds it and has just read it is the most reasonable one.
 * More importantly, it also closes the scenario of review BE-1: an attempt that was cleared of invalidation falls into the
 * auto-assign pool and is then given to a second person.
 *
 * <p><strong>Why {@code blocked_at} is cleared here:</strong> {@code resolveDefaultStatus}
 * forces INVALID while the candidate is still blocked, so without clearing it every later score recomputation
 * would pull the attempt back to INVALID and the teacher's decision would be silently nullified.
 *
 * <p>This is the ONLY place where a teacher can unblock a candidate (normally that is the
 * SCHOOL_ADMIN's right through {@code UnblockAssessmentCandidateUseCase}). The scope is tightened: exactly the
 * attempt that was given the {@code REMEDIATION} round, the reason is mandatory, and every use goes into the audit.
 */
@Service
public class ClearInvalidResultUseCase implements IUseCase<GradingDecisionCommand, GradingActionResponse> {

    private final GradingActionSupport gradingActionSupport;
    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final AssessmentGradingAssignmentRepository assessmentGradingAssignmentRepository;
    private final OutboxRepository outboxRepository;
    private final JsonSerializationPort jsonSerializationPort;

    public ClearInvalidResultUseCase(
            GradingActionSupport gradingActionSupport,
            AssessmentCandidateRepository assessmentCandidateRepository,
            AssessmentGradingAssignmentRepository assessmentGradingAssignmentRepository,
            OutboxRepository outboxRepository,
            JsonSerializationPort jsonSerializationPort) {
        this.gradingActionSupport = gradingActionSupport;
        this.assessmentCandidateRepository = assessmentCandidateRepository;
        this.assessmentGradingAssignmentRepository = assessmentGradingAssignmentRepository;
        this.outboxRepository = outboxRepository;
        this.jsonSerializationPort = jsonSerializationPort;
    }

    @Override
    @Transactional
    public GradingActionResponse execute(GradingDecisionCommand command) {
        GradingActionSupport.PreparedAction prepared = gradingActionSupport.prepare(
            command.assignmentId(), GradingOutcome.CLEARED_INVALID, command.reason());

        AssessmentCandidateResult result = prepared.context().candidateResult();
        Instant now = Instant.now();

        AssessmentCandidate candidate = assessmentCandidateRepository.findById(result.getCandidateId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy thí sinh của bài thi."));
        if (candidate.getBlockedAt() != null) {
            candidate.setBlockedAt(null);
            candidate.setUpdatedAt(now);
            candidate.setUpdatedBy(prepared.currentUserId());
            assessmentCandidateRepository.save(candidate);
        }

        // finish() đóng dòng REMEDIATION và nhả active_result_id; chỉ sau đó mới mở
        // được vòng INITIAL cho cùng bài mà không đụng unique index.
        gradingActionSupport.finish(prepared, result);

        // Hạn của vòng REMEDIATION KHÔNG được kế thừa: vòng soi bài vô hiệu thường
        // được xử lý sát hoặc đã quá hạn, nên phân công mới sẽ sinh ra với một mốc đã
        // trôi qua — đỏ "quá hạn" ngay lúc tạo, job nhắc bắn ngay lượt sau, và lọt vào
        // danh sách thu hồi dù giáo viên chưa có một phút nào để chấm. Để trống, admin
        // đặt hạn sau qua PUT /grading-assignments/{id}/deadline.
        AssessmentGradingAssignment next = assessmentGradingAssignmentRepository.save(AssessmentGradingAssignment.open(
            result.getId(),
            prepared.currentUserId(),
            GradingRoundType.INITIAL,
            null,
            result.getTotalScore(),
            now,
            prepared.currentUserId(),
            null
        ));

        UUID studentId = gradingActionSupport.resolveStudentId(result);
        if (studentId != null) {
            AssessmentResultInvalidClearedPayloadV1 payload = new AssessmentResultInvalidClearedPayloadV1(
                result.getId(), studentId, prepared.context().assessmentName(), prepared.reason(),
                result.getSessionId(), prepared.context().assessmentKind());
            outboxRepository.save(Outbox.create(
                AggregateTypeConstant.ASSESSMENT_CANDIDATE_RESULT,
                result.getId(),
                EventTypeConstant.ASSESSMENT_RESULT_INVALID_CLEARED,
                jsonSerializationPort.toJson(payload),
                now
            ));
        }

        return new GradingActionResponse(
            command.assignmentId(),
            result.getId(),
            GradingOutcome.CLEARED_INVALID.name(),
            result.getStatus() == null ? null : result.getStatus().name(),
            result.getTotalScore(),
            next.getId()
        );
    }
}
