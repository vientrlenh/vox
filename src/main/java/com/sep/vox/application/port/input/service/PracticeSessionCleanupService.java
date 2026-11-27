package com.sep.vox.application.port.input.service;

import com.sep.vox.domain.model.personalization.PracticeSession;
import java.time.Instant;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.repository.PracticeSessionRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class PracticeSessionCleanupService {

    private final PracticeSessionRepository practiceSessionRepository;
    private final PracticeGradingFlushService gradingFlushService;
    private final PracticeSessionClosedHandler sessionClosedHandler;

    @Transactional
    public int cleanupStaleSessions(Instant staleBefore) {
        List<PracticeSession> stale = practiceSessionRepository.findStaleInProgress(staleBefore);
        for (PracticeSession session : stale) {
            closeStaleSession(session);
        }
        return stale.size();
    }

    private void closeStaleSession(PracticeSession session) {
        // A session that dropped the network is the kind MOST likely to have an unfinished question -- a student who does not
        // press end by themselves is almost certainly in the middle of a question. Not flushing here would make
        // exactly the group that needs it most lose everything.
        gradingFlushService.flush(session.getId());
        boolean spoke = session.getGradedSeconds() > 0;
        String status = statusForStaleSession(spoke);
        String diagnosis = diagnosisForStaleSession(spoke);
        practiceSessionRepository.save(session.closedAsStale(
            status,
            diagnosis,
            session.getLastHeartbeatAt()
        ));
        notifySessionClosed(session, status, diagnosis);
    }

    /**
     * The same measure as EndPracticeSessionUseCase: whether the student SPOKE, not whether grading has finished --
     * see the long comment there for why.
     *
     * <p>There is an extra nuance here: a session that reaches the cleanup job means the student did NOT press end
     * by themselves (dropped network, closed the app). But if they really did speak, that effort must still
     * be counted -- the turns were recorded, graded and entered the weakness profile.
     */
    private String statusForStaleSession(boolean spoke) {
        return spoke ? "COMPLETED" : "ABANDONED";
    }

    /**
     * UNKNOWN outright, do NOT call SessionDiagnosisPolicy.
     *
     * <p>A session that reaches the cleanup job means the client had no time to send anything -- we have neither
     * helpRequestCount nor longPauseCount. The previous version passed 0/0 into the diagnosis policy, but 0/0 makes
     * both behavioural sides of the rule always true / always false, so the policy degenerates into "score >= 0.65
     * then conclude BORED". A student who closes the app before speaking a single question, with a high score from the
     * PREVIOUS session, gets recorded as bored of this topic.
     *
     * <p>The consequence does not stop at a wrong label: recordSessionOutcome only writes an interest event for an
     * unfinished session when diagnosis == BORED, so every wrong guess lowers the topic's interest score on
     * evidence that does not exist.
     *
     * <p>If we do not know, say we do not know. UNKNOWN produces no event at all.
     */
    private String diagnosisForStaleSession(boolean spoke) {
        return spoke ? null : "UNKNOWN";
    }

    /**
     * EXACTLY the work that EndPracticeSessionUseCase does after closing a session. Missing it here is missing it
     * for the MAJORITY of sessions, not a rare case: measured on real data, 4 of the 5 most recent sessions go through
     * this very path -- the student only closes the app and does not press "Finish". For those 4 sessions the interest
     * score is not updated and no new topic is generated, so however many sessions are practised the topic list stays put.
     */
    private void notifySessionClosed(PracticeSession session, String status, String diagnosis) {
        try {
            sessionClosedHandler.afterClosed(
                session.getStudentId(), session.getId(), status, diagnosis
            );
        } catch (RuntimeException exception) {
            // Caught HERE, outside the REQUIRES_NEW transaction boundary of afterClosed. Catching it
            // inside is useless: Spring still throws UnexpectedRollbackException at commit
            // because the transaction was already marked rollback-only. Really measured -- the whole job dies,
            // no session gets closed, and they stay IN_PROGRESS across many runs.
            log.warn("Post-close handling of session {} failed.", session.getId(), exception);
        }
    }
}
