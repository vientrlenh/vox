package com.sep.vox.application.port.input.usecase.practicesession;

import java.time.Instant;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.mapper.practicesession.PracticeSessionResponseMapper;
import com.sep.vox.application.mapper.practicesession.SessionRowMapper;
import com.sep.vox.application.port.input.command.practicesession.EndPracticeSessionCommand;
import com.sep.vox.application.port.input.service.PracticeGradingFlushService;
import com.sep.vox.application.port.input.service.PracticeSessionClosedHandler;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.PracticeSessionQueryRepository;
import com.sep.vox.application.response.input.practicesession.PracticeSessionResponses.PracticeSession;
import com.sep.vox.domain.repository.PracticeItemEvaluationRepository;
import com.sep.vox.domain.repository.PracticeSessionRepository;
import com.sep.vox.domain.service.personalization.SessionDiagnosisPolicy;

@Service
@RequiredArgsConstructor
@Slf4j
public class EndPracticeSessionUseCase implements IUseCase<EndPracticeSessionCommand, PracticeSession> {

    private final PracticeSessionRepository practiceSessionRepository;
    private final PracticeSessionQueryRepository practiceSessionQueryRepository;
    private final PracticeItemEvaluationRepository practiceItemEvaluationRepository;
    private final PracticeGradingFlushService gradingFlushService;
    private final PracticeSessionClosedHandler sessionClosedHandler;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public PracticeSession execute(EndPracticeSessionCommand input) {
        UUID studentId = userContextPort.getCurrentAuthenticatedUserId();
        UUID sessionId = input.sessionId();
        requireOwnedInProgress(sessionId, studentId);
        // Flush grading BEFORE reading the score: the question the student left unfinished has never
        // been graded, and if it is not fired here nobody ever will. The result arrives some tens of
        // seconds later -- RecordPracticeAttemptEvaluationUseCase then refreshes overall_score itself.
        gradingFlushService.flush(sessionId);
        Double lastScore = practiceItemEvaluationRepository.findLastValidNormalizedScore(sessionId);
        com.sep.vox.domain.model.personalization.PracticeSession session = practiceSessionRepository.findById(sessionId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiên luyện."));
        String status = endedStatus(session);
        String diagnosis = "ABANDONED".equals(status)
            ? SessionDiagnosisPolicy.diagnose(lastScore, input.helpRequestCount(), input.longPauseCount())
            : null;
        saveEndedSession(session, input, status, diagnosis);
        learnFromClosedSession(studentId, sessionId, status, diagnosis);
        return PracticeSessionResponseMapper.toResponse(
            SessionRowMapper.toDto(
                practiceSessionQueryRepository.findSessionRow(sessionId, studentId).orElse(null)
            )
        );
    }

    private void saveEndedSession(com.sep.vox.domain.model.personalization.PracticeSession session,
                                  EndPracticeSessionCommand input, String status, String diagnosis) {
        practiceSessionRepository.save(session.ended(
            status,
            diagnosis,
            input.helpRequestCount(),
            input.longPauseCount(),
            Instant.now(),
            practiceItemEvaluationRepository.findAverageItemScoreBySessionId(input.sessionId())
        ));
    }

    /**
     * COMPLETED = the student DID SPEAK, not "grading has finished".
     *
     * <p>The previous version counted countCompletedBySessionId, i.e. evaluations ALREADY in the DB. But
     * grading is asynchronous: measured on real data, ended_at 11:03:25 while evaluated_at 11:04:10 --
     * 45 seconds AFTER. So every session became ABANDONED even when the student spoke every turn and
     * ended it themselves.
     *
     * <p>The cost is more than a wrong label: recordSessionOutcome exits at once when the status is not
     * COMPLETED (the interest score is never written) and findDashboardCounts filters on
     * status='COMPLETED' (session count and average score stay 0).
     *
     * <p>The status must describe the LEARNER'S BEHAVIOUR, not infrastructure latency. The grading result
     * that arrives later is still written to practice_item_evaluations and overall_score is recomputed
     * from it -- the student sees it when reopening the history.
     *
     * <p>gradedSeconds is the seconds in which VAD heard speech, the same number quota deducts. Opening a
     * session and leaving without saying anything is still ABANDONED -- genuinely abandoned.
     */
    private String endedStatus(com.sep.vox.domain.model.personalization.PracticeSession session) {
        return session.getGradedSeconds() > 0 ? "COMPLETED" : "ABANDONED";
    }

    /**
     * The "learn from the session just ended" part is shared with the cleanup job path -- see
     * PracticeSessionClosedHandler. Kept separate here, a session closed by the job (most of them,
     * since students usually just close the app) would not update the interest score nor generate new topics.
     */
    private void learnFromClosedSession(UUID studentId, UUID sessionId, String status, String diagnosis) {
        try {
            sessionClosedHandler.afterClosed(studentId, sessionId, status, diagnosis);
        } catch (RuntimeException exception) {
            // The session IS already closed above; a failure in the follow-up bookkeeping must not
            // break the student's end-session request. Caught here, outside the REQUIRES_NEW boundary.
            log.warn("Ghi nhận sau khi kết thúc phiên {} thất bại.", sessionId, exception);
        }
    }

    private void requireOwnedInProgress(UUID sessionId, UUID studentId) {
        if (!practiceSessionRepository.existsByIdAndStudentIdAndStatus(sessionId, studentId, "IN_PROGRESS")) {
            throw new ResourceNotFoundException("Phiên luyện không còn hoạt động.");
        }
    }

}
