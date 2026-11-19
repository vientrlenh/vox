package com.sep.vox.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import com.sep.vox.domain.model.exam.ExamRequiredStreamType;
import com.sep.vox.domain.model.exam.ExamSession;
import com.sep.vox.domain.model.exam.ExamSessionStatus;
import com.sep.vox.domain.repository.ExamSessionRepository;
import com.sep.vox.infrastructure.persistence.adapter.ExamSessionRepositoryImpl;
import com.sep.vox.support.RepositoryTest;

@RepositoryTest
@Import({
    ExamSessionRepositoryImpl.class
})
class ExamSessionRepositoryTests {

    @Autowired
    private ExamSessionRepository examSessionRepository;

    private ExamSession newSession(ExamSessionStatus status) {
        return examSessionRepository.save(new ExamSession(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            Instant.now(),
            null,
            status,
            false,
            null
        ));
    }

    @Test
    void whenFindByIdAndResumable_thenReturnsInProgressSession() {
        ExamSession saved = newSession(ExamSessionStatus.IN_PROGRESS);

        assertThat(examSessionRepository.findByIdAndResumable(saved.getId())).isPresent();
    }

    /**
     * Exactly the most needed situation: the student's machine loses connection, the session is
     * marked INTERRUPTED, then the machine comes back with segments still in its buffer and needs a
     * token to push them up.
     */
    @Test
    void whenFindByIdAndResumable_thenReturnsInterruptedSession() {
        ExamSession saved = newSession(ExamSessionStatus.INTERRUPTED);

        assertThat(examSessionRepository.findByIdAndResumable(saved.getId())).isPresent();
    }

    @Test
    void whenFindByIdAndResumable_thenSkipsSubmittedSession() {
        ExamSession saved = newSession(ExamSessionStatus.SUBMITTED);

        assertThat(examSessionRepository.findByIdAndResumable(saved.getId())).isEmpty();
    }

    @Test
    void whenSessionHasNoChosenStreamType_thenLockWritesIt() {
        ExamSession saved = newSession(ExamSessionStatus.IN_PROGRESS);
        assertThat(saved.getChosenStreamType()).isNull();

        int updated = examSessionRepository.lockChosenStreamType(saved.getId(), ExamRequiredStreamType.SCREEN);

        assertThat(updated).isEqualTo(1);
        assertThat(examSessionRepository.findById(saved.getId()))
            .get()
            .extracting(session -> session.getChosenStreamType())
            .isEqualTo(ExamRequiredStreamType.SCREEN);
    }

    /**
     * Write once, then lock: the second token issue must not change the choice, even when the
     * requests arrive in parallel. This is what prevents starting with camera + screen and then
     * dropping down to camera only.
     */
    @Test
    void whenStreamTypeAlreadyChosen_thenLockChangesNothing() {
        ExamSession saved = newSession(ExamSessionStatus.IN_PROGRESS);
        examSessionRepository.lockChosenStreamType(saved.getId(), ExamRequiredStreamType.CAMERA_AND_SCREEN);

        int updated = examSessionRepository.lockChosenStreamType(saved.getId(), ExamRequiredStreamType.CAMERA);

        assertThat(updated).isEqualTo(0);
        assertThat(examSessionRepository.findById(saved.getId()))
            .get()
            .extracting(session -> session.getChosenStreamType())
            .isEqualTo(ExamRequiredStreamType.CAMERA_AND_SCREEN);
    }

    @Test
    void whenSessionHasNoCheckpoint_thenRemainingSecondsIsWritten() {
        ExamSession saved = newSession(ExamSessionStatus.IN_PROGRESS);
        assertThat(saved.getRemainingSeconds()).isNull();

        assertThat(examSessionRepository.checkpointRemainingSeconds(saved.getId(), 1500)).isEqualTo(1);
        assertThat(examSessionRepository.findById(saved.getId()))
            .get()
            .extracting(session -> session.getRemainingSeconds())
            .isEqualTo(1500);
    }

    @Test
    void whenCountdownMovesDown_thenCheckpointIsAccepted() {
        ExamSession saved = newSession(ExamSessionStatus.IN_PROGRESS);
        examSessionRepository.checkpointRemainingSeconds(saved.getId(), 1500);

        assertThat(examSessionRepository.checkpointRemainingSeconds(saved.getId(), 1490)).isEqualTo(1);
        assertThat(examSessionRepository.findById(saved.getId()))
            .get()
            .extracting(session -> session.getRemainingSeconds())
            .isEqualTo(1490);
    }

    /**
     * A security constraint, not an optimization: the value comes from the student's machine, so if
     * the clock were allowed to go forward, the checkpoint endpoint would become an API for
     * extending exam time on your own. It also rejects old checkpoint packets that arrive late and
     * out of order.
     */
    @Test
    void whenCountdownWouldMoveUp_thenCheckpointIsRejected() {
        ExamSession saved = newSession(ExamSessionStatus.IN_PROGRESS);
        examSessionRepository.checkpointRemainingSeconds(saved.getId(), 600);

        assertThat(examSessionRepository.checkpointRemainingSeconds(saved.getId(), 1500)).isEqualTo(0);
        assertThat(examSessionRepository.checkpointRemainingSeconds(saved.getId(), 600)).isEqualTo(0);
        assertThat(examSessionRepository.findById(saved.getId()))
            .get()
            .extracting(session -> session.getRemainingSeconds())
            .isEqualTo(600);
    }

    @Test
    void whenSavingSession_thenChosenStreamTypeSurvivesRoundTrip() {
        ExamSession saved = newSession(ExamSessionStatus.IN_PROGRESS);
        examSessionRepository.lockChosenStreamType(saved.getId(), ExamRequiredStreamType.CAMERA);

        // Another use case reads the exam session, changes its status and saves it again: the
        // locked choice must not vanish just because it went through a domain <-> JPA mapping
        // round.
        ExamSession reloaded = examSessionRepository.findById(saved.getId()).orElseThrow();
        reloaded.setStatus(ExamSessionStatus.INTERRUPTED);
        examSessionRepository.save(reloaded);

        assertThat(examSessionRepository.findById(saved.getId()))
            .get()
            .extracting(session -> session.getChosenStreamType())
            .isEqualTo(ExamRequiredStreamType.CAMERA);
    }
}
