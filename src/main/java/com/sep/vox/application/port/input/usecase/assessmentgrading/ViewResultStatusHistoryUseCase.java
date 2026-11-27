package com.sep.vox.application.port.input.usecase.assessmentgrading;

import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.service.AssessmentGradingAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.dto.ResultStatusHistoryInfo;
import com.sep.vox.application.query.repository.AssessmentResultAuditQueryRepository;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.AssessmentGradingAssignmentRepository;
import com.sep.vox.domain.repository.AssessmentRepository;

/**
 * The score timeline of a submission: who, when, from which status to which status,
 * why, score before → after.
 *
 * <p>Three groups of viewers, three different reasons: a school admin needs it to justify a
 * dispute, a student needs it to understand why their score changed, and a teacher WHO WAS EVER
 * assigned the submission needs it as context -- especially in the appeal round, where the right question is "which
 * hands has this score passed through". A student can only see their own submission; a teacher can only
 * see submissions they once held, even if that assignment is already closed.
 */
@Service
@RequiredArgsConstructor
public class ViewResultStatusHistoryUseCase implements IUseCase<UUID, List<ResultStatusHistoryInfo>> {

    private final AssessmentResultAuditQueryRepository assessmentResultAuditQueryRepository;
    private final AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final AssessmentGradingAssignmentRepository assessmentGradingAssignmentRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentGradingAccessService assessmentGradingAccessService;
    private final UserContextPort userContextPort;

    @Override
    @Transactional(readOnly = true)
    public List<ResultStatusHistoryInfo> execute(UUID candidateResultId) {
        UUID currentUserId = assessmentGradingAccessService.requireActiveUserId();
        AssessmentCandidateResult result = assessmentCandidateResultRepository.findById(candidateResultId)
            .orElseThrow(() -> new ResourceNotFoundException("Submission result not found."));

        // The owner of the attempt can always see their own history -- checked first so a student does not
        // go through the admin authorization branch and receive ForbiddenException.
        boolean isOwner = assessmentCandidateRepository.findById(result.getCandidateId())
            .map(candidate -> currentUserId.equals(candidate.getStudentId()))
            .orElse(false);
        // The timeline carries scoreBefore/scoreAfter -- for the owner that IS the score, so it follows the same
        // publication rule as the result screen. Returns empty instead of throwing: this is an auxiliary block,
        // throwing would break the whole page just because of a side item.
        if (isOwner && !AssessmentCandidateResultStatus.isVisibleToCandidate(result.getStatus())) {
            return List.of();
        }
        if (!isOwner) {
            requireHistoryReadAccess(result, candidateResultId, currentUserId);
        }
        return assessmentResultAuditQueryRepository.findHistory(candidateResultId);
    }

    private void requireHistoryReadAccess(AssessmentCandidateResult result, UUID candidateResultId, UUID currentUserId) {
        if (userContextPort.isSystemAdmin()) {
            return;
        }
        // A teacher who was EVER assigned this attempt: the permission to read the timeline follows the grading
        // that was done, not the assignment row that is still open. In the appeal round, the grader of the
        // previous round is the one being asked "why did you grade it this way" -- and by then their assignment
        // is already COMPLETED, so demanding an open assignment would lock out exactly the person who needs to read it
        // most. The same rule as listening to the recording in AssessmentRecordingAccessService.
        //
        // This is READ permission. Every score WRITE action still goes through
        // AssessmentGradingAccessService.authorizeAssignedTeacher on the row that is actually open.
        if (assessmentGradingAssignmentRepository.existsByCandidateResultIdAndTeacherId(candidateResultId, currentUserId)) {
            return;
        }
        Assessment assessment = assessmentRepository.findById(result.getAssessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Assessment not found."));
        assessmentGradingAccessService.authorizeSchoolAdmin(assessment.getSchoolId(), currentUserId);
    }
}
