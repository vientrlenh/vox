package com.sep.vox.application.port.input.usecase.proctoring;

import java.time.Instant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.sep.vox.application.port.input.command.proctoring.RecordProctoringAlertCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.model.assessment.AssessmentProctoringAlert;
import com.sep.vox.domain.repository.AssessmentProctoringAlertRepository;
import com.sep.vox.domain.repository.AssessmentSessionRepository;

/**
 * Writes one proctoring alert into the ledger.
 *
 * <p>Deliberately NOT annotated {@code @Transactional}: de-duplication relies on the unique constraint on
 * {@code event_id}, and the adapter must be able to catch that violation and carry on. Inside a transaction opened by
 * the use case, a failed write marks the transaction rollback-only and everything after it is meaningless even though
 * the exception was caught. Letting each write handle its own transaction is what makes swallowing the error
 * actually swallow it.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecordProctoringAlertUseCase implements IUseCase<RecordProctoringAlertCommand, Boolean> {

    private final AssessmentProctoringAlertRepository assessmentProctoringAlertRepository;
    private final AssessmentSessionRepository assessmentSessionRepository;

    @Override
    public Boolean execute(RecordProctoringAlertCommand input) {
        // A session that does not exist is SKIPPED, not thrown. Unlike the recording path, where an unknown sessionId is a
        // sign of real damage worth stopping for: here the source includes WebRTC sessions whose id the AI service
        // generates itself, plus data left over from before the identity fix. Throwing would push them into the DLT and
        // retry forever, blocking the valid alerts behind them in the same partition -- a terrible trade for a record
        // we cannot use anyway.
        if (!assessmentSessionRepository.findById(input.assessmentSessionId()).isPresent()) {
            log.warn(
                "Skipping the proctoring alert because the assessment session was not found: eventId={} assessmentSessionId={} alertType={}",
                input.eventId(), input.assessmentSessionId(), input.alertType()
            );
            return false;
        }

        AssessmentProctoringAlert alert = AssessmentProctoringAlert.raised(
                input.eventId(), input.assessmentSessionId(), input.candidateId(), input.streamId(), input.streamType(),
                input.alertType(), input.level(), input.source(), input.detail(), input.confidence(),
                input.sequenceNo(), input.capturedAt(), input.raisedAt(), Instant.now());

        return assessmentProctoringAlertRepository.saveIfAbsent(alert);
    }
}
