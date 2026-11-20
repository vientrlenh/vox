package com.sep.vox.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import com.sep.vox.domain.model.assessment.AssessmentRequiredStreamType;
import com.sep.vox.domain.model.assessment.AssessmentSession;
import com.sep.vox.domain.model.assessment.AssessmentSessionStatus;
import com.sep.vox.domain.repository.AssessmentSessionRepository;
import com.sep.vox.infrastructure.persistence.adapter.AssessmentSessionRepositoryImpl;
import com.sep.vox.support.RepositoryTest;

@RepositoryTest
@Import({
    AssessmentSessionRepositoryImpl.class
})
class AssessmentSessionRepositoryTests {

    @Autowired
    private AssessmentSessionRepository assessmentSessionRepository;

    private AssessmentSession newSession(AssessmentSessionStatus status) {
        return assessmentSessionRepository.save(new AssessmentSession(
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
        AssessmentSession saved = newSession(AssessmentSessionStatus.IN_PROGRESS);

        assertThat(assessmentSessionRepository.findByIdAndResumable(saved.getId())).isPresent();
    }

    /**
     * Exactly the most needed situation: the student's machine loses connection, the session is
     * marked INTERRUPTED, then the machine comes back with segments still in its buffer and needs a
     * token to push them up.
     */
    @Test
    void whenFindByIdAndResumable_thenReturnsInterruptedSession() {
        AssessmentSession saved = newSession(AssessmentSessionStatus.INTERRUPTED);

        assertThat(assessmentSessionRepository.findByIdAndResumable(saved.getId())).isPresent();
    }

    @Test
    void whenFindByIdAndResumable_thenSkipsSubmittedSession() {
        AssessmentSession saved = newSession(AssessmentSessionStatus.SUBMITTED);

        assertThat(assessmentSessionRepository.findByIdAndResumable(saved.getId())).isEmpty();
    }

    @Test
    void whenSessionHasNoChosenStreamType_thenLockWritesIt() {
        AssessmentSession saved = newSession(AssessmentSessionStatus.IN_PROGRESS);
        assertThat(saved.getChosenStreamType()).isNull();

        int updated = assessmentSessionRepository.lockChosenStreamType(saved.getId(), AssessmentRequiredStreamType.SCREEN);

        assertThat(updated).isEqualTo(1);
        assertThat(assessmentSessionRepository.findById(saved.getId()))
            .get()
            .extracting(session -> session.getChosenStreamType())
            .isEqualTo(AssessmentRequiredStreamType.SCREEN);
    }

    /**
     * Write once, then lock: the second token issue must not change the choice, even when the
     * requests arrive in parallel. This is what prevents starting with camera + screen and then
     * dropping down to camera only.
     */
    @Test
    void whenStreamTypeAlreadyChosen_thenLockChangesNothing() {
        AssessmentSession saved = newSession(AssessmentSessionStatus.IN_PROGRESS);
        assessmentSessionRepository.lockChosenStreamType(saved.getId(), AssessmentRequiredStreamType.CAMERA_AND_SCREEN);

        int updated = assessmentSessionRepository.lockChosenStreamType(saved.getId(), AssessmentRequiredStreamType.CAMERA);

        assertThat(updated).isEqualTo(0);
        assertThat(assessmentSessionRepository.findById(saved.getId()))
            .get()
            .extracting(session -> session.getChosenStreamType())
            .isEqualTo(AssessmentRequiredStreamType.CAMERA_AND_SCREEN);
    }

    @Test
    void whenSessionHasNoCheckpoint_thenRemainingSecondsIsWritten() {
        AssessmentSession saved = newSession(AssessmentSessionStatus.IN_PROGRESS);
        assertThat(saved.getRemainingSeconds()).isNull();

        assertThat(assessmentSessionRepository.checkpointRemainingSeconds(saved.getId(), 1500)).isEqualTo(1);
        assertThat(assessmentSessionRepository.findById(saved.getId()))
            .get()
            .extracting(session -> session.getRemainingSeconds())
            .isEqualTo(1500);
    }

    @Test
    void whenCountdownMovesDown_thenCheckpointIsAccepted() {
        AssessmentSession saved = newSession(AssessmentSessionStatus.IN_PROGRESS);
        assessmentSessionRepository.checkpointRemainingSeconds(saved.getId(), 1500);

        assertThat(assessmentSessionRepository.checkpointRemainingSeconds(saved.getId(), 1490)).isEqualTo(1);
        assertThat(assessmentSessionRepository.findById(saved.getId()))
            .get()
            .extracting(session -> session.getRemainingSeconds())
            .isEqualTo(1490);
    }

    /**
     * A security constraint, not an optimization: the value comes from the student's machine, so if
     * the clock were allowed to go forward, the checkpoint endpoint would become an API for
     * extending assessment time on your own. It also rejects old checkpoint packets that arrive late and
     * out of order.
     */
    @Test
    void whenCountdownWouldMoveUp_thenCheckpointIsRejected() {
        AssessmentSession saved = newSession(AssessmentSessionStatus.IN_PROGRESS);
        assessmentSessionRepository.checkpointRemainingSeconds(saved.getId(), 600);

        assertThat(assessmentSessionRepository.checkpointRemainingSeconds(saved.getId(), 1500)).isEqualTo(0);
        assertThat(assessmentSessionRepository.checkpointRemainingSeconds(saved.getId(), 600)).isEqualTo(0);
        assertThat(assessmentSessionRepository.findById(saved.getId()))
            .get()
            .extracting(session -> session.getRemainingSeconds())
            .isEqualTo(600);
    }

    @Test
    void whenSavingSession_thenChosenStreamTypeSurvivesRoundTrip() {
        AssessmentSession saved = newSession(AssessmentSessionStatus.IN_PROGRESS);
        assessmentSessionRepository.lockChosenStreamType(saved.getId(), AssessmentRequiredStreamType.CAMERA);

        // Another use case reads the assessment session, changes its status and saves it again: the
        // locked choice must not vanish just because it went through a domain <-> JPA mapping
        // round.
        AssessmentSession reloaded = assessmentSessionRepository.findById(saved.getId()).orElseThrow();
        reloaded.setStatus(AssessmentSessionStatus.INTERRUPTED);
        assessmentSessionRepository.save(reloaded);

        assertThat(assessmentSessionRepository.findById(saved.getId()))
            .get()
            .extracting(session -> session.getChosenStreamType())
            .isEqualTo(AssessmentRequiredStreamType.CAMERA);
    }
}
