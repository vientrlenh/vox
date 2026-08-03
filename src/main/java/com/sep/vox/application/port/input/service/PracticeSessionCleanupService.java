package com.sep.vox.application.port.input.service;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.repository.personalization.PracticeItemEvaluationRepository;
import com.sep.vox.domain.repository.personalization.PracticeSessionRepository;
import com.sep.vox.domain.service.personalization.SessionDiagnosisPolicy;

@Service
public class PracticeSessionCleanupService {

    private final PracticeSessionRepository practiceSessionRepository;
    private final PracticeItemEvaluationRepository practiceItemEvaluationRepository;

    public PracticeSessionCleanupService(
            PracticeSessionRepository practiceSessionRepository,
            PracticeItemEvaluationRepository practiceItemEvaluationRepository) {
        this.practiceSessionRepository = practiceSessionRepository;
        this.practiceItemEvaluationRepository = practiceItemEvaluationRepository;
    }

    @Transactional
    public int cleanupStaleSessions(Instant staleBefore) {
        var stale = practiceSessionRepository.findStaleInProgress(staleBefore);
        for (var session : stale) {
            var completed = practiceItemEvaluationRepository.countCompletedBySessionId(session.getId());
            var score = practiceItemEvaluationRepository.findLastValidNormalizedScore(session.getId());
            var diagnosis = completed > 0 ? null : SessionDiagnosisPolicy.diagnose(score, 0, 0);
            practiceSessionRepository.save(session.closedAsStale(
                completed > 0 ? "COMPLETED" : "ABANDONED",
                diagnosis,
                session.getLastHeartbeatAt()
            ));
        }
        return stale.size();
    }
}
