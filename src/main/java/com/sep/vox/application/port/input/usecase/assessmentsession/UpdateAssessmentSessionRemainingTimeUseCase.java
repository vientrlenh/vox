package com.sep.vox.application.port.input.usecase.assessmentsession;

import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.assessmentsession.UpdateAssessmentSessionRemainingTimeCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.assessment.AssessmentSession;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentPaperRepository;
import com.sep.vox.domain.repository.AssessmentSessionRepository;

/**
 * Records the countdown clock reported by the client, so that re-entering the session does not count down from the start.
 *
 * <p>This value has to be supplied by the client: the WPF clock does not subtract seconds while the avatar is
 * speaking, so the server cannot compute it from {@code startedAt + duration}. In return, it is data sent up by the
 * learner's machine and must be treated as untrusted -- the two constraints below are what stop it from
 * becoming an API for extending the assessment time by oneself:
 *
 * <ul>
 *   <li>an upper bound equal to the duration of the exact paper variant issued, so an abnormally large value has no
 *       effect;
 *   <li>the clock may only go backwards (enforced by a conditional UPDATE statement in
 *       {@code AssessmentSessionRepository#checkpointRemainingSeconds}).
 * </ul>
 *
 * <p>Returns the value that is actually stored, not the value the client sent - so the client knows whether its request
 * was rejected instead of having to guess.
 */
@Service
@RequiredArgsConstructor
public class UpdateAssessmentSessionRemainingTimeUseCase
        implements IUseCase<UpdateAssessmentSessionRemainingTimeCommand, Integer> {

    private final UserContextPort userContextPort;
    private final AssessmentSessionRepository assessmentSessionRepository;
    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final AssessmentPaperRepository assessmentPaperRepository;

    @Override
    @Transactional
    public Integer execute(UpdateAssessmentSessionRemainingTimeCommand input) {
        UUID userId = userContextPort.getCurrentAuthenticatedUserId();

        // RESUMABLE chứ không phải findById: checkpoint cho một phiên đã nộp hoặc đã chấm là vô
        // nghĩa, và nhận nó vào sẽ làm bẩn dữ liệu của lần chấm.
        AssessmentSession session = assessmentSessionRepository.findByIdAndResumable(input.sessionId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiên thi hoặc phiên thi đã kết thúc"));

        AssessmentCandidate candidate = assessmentCandidateRepository.findById(session.getCandidateId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy học viên thi"));

        if (!candidate.getStudentId().equals(userId)) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        assessmentSessionRepository.checkpointRemainingSeconds(session.getId(), clamp(session, input.remainingSeconds()));

        return assessmentSessionRepository.findById(session.getId())
            .map(s -> s.getRemainingSeconds())
            .orElse(null);
    }

    /**
     * Clamps the value sent by the client into the valid range. The upper bound is the duration of the exact paper variant issued to the
     * session, not the largest duration of the whole assessment, because an assessment can have several papers.
     */
    private int clamp(AssessmentSession session, int remainingSeconds) {
        int value = Math.max(0, remainingSeconds);
        Integer ceiling = assessmentPaperRepository.findById(session.getPaperId())
            .map(paper -> paper.getTimeDurationSeconds())
            .orElse(null);
        if (ceiling != null && value > ceiling) {
            return ceiling;
        }
        return value;
    }
}
