package com.sep.vox.application.port.input.service;

import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.assessment.AssessmentSession;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.AssessmentGradingAssignmentRepository;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentScheduleProctorRepository;
import com.sep.vox.domain.repository.AssessmentSessionRepository;

/**
 * "Who may view the recordings of this assessment session" — one source of truth for every recording read path.
 *
 * <p>Split out because there are two users with very different needs: the desktop proctor app only needs
 * the list of recordings, while the grading screen also needs the playback link. If each side copied this
 * permission block, sooner or later the two copies would drift apart -- and drifting on who may view the video of an assessment is the kind of
 * bug nobody sees until it is too late.
 */
@Service
@RequiredArgsConstructor
public class AssessmentRecordingAccessService {

    private final AssessmentSessionRepository assessmentSessionRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final AssessmentMemberRepository assessmentMemberRepository;
    private final AssessmentScheduleProctorRepository assessmentScheduleProctorRepository;
    private final AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private final AssessmentGradingAssignmentRepository assessmentGradingAssignmentRepository;
    private final UserContextPort userContextPort;

    /**
     * Throws if the caller may not view; returns the session if they may.
     *
     * <p>Four tiers: must be in the same school → school admin goes straight through → a teacher must be the
     * council chair, OR the person assigned to grade that very attempt, OR a proctor of the exact exam slot. Every other role
     * is blocked, including the candidate themselves: a student may not replay the video of their own exam through
     * this path.
     *
     * <p>The "grader" tier was added later, and it is NOT a loosening for convenience: the {@code SPOT_CHECK} and
     * {@code APPEAL} rounds deliberately hand the attempt to teachers who never took part in the exam slot
     * (a post-check that hands it back to the very proctor is no post-check at all). If recording access
     * followed only the proctor role, the exact people who must listen to the answer to grade it would be
     * the ones blocked -- and the frontend swallows the 403 as "this slot has no recording", so the fault
     * stays silent.
     */
    public AssessmentSession requireCanViewRecordings(UUID assessmentSessionId) {
        UUID userId = userContextPort.getCurrentAuthenticatedUserId();
        UUID schoolId = userContextPort.getCurrentSchoolId();

        AssessmentSession session = assessmentSessionRepository.findById(assessmentSessionId)
            .orElseThrow(() -> new ResourceNotFoundException("Assessment session not found"));

        Assessment assessment = assessmentRepository.findById(session.getAssessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Assessment not found"));

        if (schoolId == null || !assessment.getSchoolId().equals(schoolId)) {
            throw new ForbiddenException("You do not have permission to view the recording");
        }

        if (userContextPort.isSchoolAdmin()) {
            return session;
        }

        if (!userContextPort.isTeacher()) {
            throw new ForbiddenException("Your current role is not allowed to view assessment recordings");
        }

        requireChairGraderOrProctor(session, assessment, userId);
        return session;
    }

    private void requireChairGraderOrProctor(AssessmentSession session, Assessment assessment, UUID userId) {
        boolean isChair = assessmentMemberRepository
            .existsByAssessmentIdAndUserIdAndRole(assessment.getId(), userId, AssessmentMemberRole.CHAIR);
        if (isChair || isAssignedGrader(session, userId)) {
            return;
        }
        requireProctorOfSession(session, userId);
    }

    /**
     * The person assigned to grade this very attempt. Checked BEFORE the proctor branch because on the grading screen this
     * is the most common case, and letting it fall below costs two extra queries every time an attempt is opened.
     *
     * <p>It also counts closed assignment rows: for an appealed attempt, the grader of the previous round is the one
     * being asked "why did you grade it this way", and by then their row is already COMPLETED. This is the permission to READ evidence,
     * not the permission to write scores -- every grading action still goes through
     * AssessmentGradingAccessService.authorizeAssignedTeacher on the row that is actually open.
     */
    private boolean isAssignedGrader(AssessmentSession session, UUID userId) {
        return assessmentCandidateResultRepository.findBySessionId(session.getId())
            .map(result -> assessmentGradingAssignmentRepository
                .existsByCandidateResultIdAndTeacherId(result.getId(), userId))
            .orElse(false);
    }

    private void requireProctorOfSession(AssessmentSession session, UUID userId) {
        AssessmentCandidate candidate = assessmentCandidateRepository.findById(session.getCandidateId())
            .orElseThrow(() -> new ResourceNotFoundException("Candidate not found"));
        UUID scheduleId = candidate.getScheduleId();
        boolean isProctor = scheduleId != null
            && assessmentScheduleProctorRepository.existsByScheduleIdAndTeacherId(scheduleId, userId);
        if (!isProctor) {
            throw new ForbiddenException("You are not assigned to proctor this assessment schedule");
        }
    }
}
