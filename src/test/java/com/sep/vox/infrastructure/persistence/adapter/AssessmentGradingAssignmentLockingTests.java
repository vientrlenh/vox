package com.sep.vox.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.grpc.test.autoconfigure.AutoConfigureTestGrpcTransport;
import org.springframework.transaction.support.TransactionTemplate;

import com.sep.vox.domain.model.assessment.AssessmentGradingAssignment;
import com.sep.vox.domain.model.assessment.GradingAssignmentStatus;
import com.sep.vox.domain.model.assessment.GradingOutcome;
import com.sep.vox.domain.model.assessment.GradingRoundType;
import com.sep.vox.domain.repository.AssessmentGradingAssignmentRepository;
import com.sep.vox.support.IntegrationTest;

/**
 * Pessimistic lock on grading assignments -- the replacement for the removed {@code version} column.
 *
 * <p>Why it is needed: the domain is mapped to a detached POJO, so {@code save} is a {@code merge} that
 * overwrites ALL columns, with no dirty checking. Two transactions that both read one ASSIGNED row
 * and then both write will both succeed with their UPDATE, and the later one erases the work of the
 * earlier -- with no error and no log. {@code findByIdForUpdate} is the only thing that blocks it.
 *
 * <p>This class is NOT {@code @Transactional}: the two threads must be in two real transactions
 * for the lock to have anything to block.
 */
@IntegrationTest
@AutoConfigureTestGrpcTransport
class AssessmentGradingAssignmentLockingTests {

    /** Wait cap for every thread join -- so a locking bug becomes a red test, not a hanging build. */
    private static final int TIMEOUT_SECONDS = 20;

    @Autowired
    private AssessmentGradingAssignmentRepository repository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private final UUID candidateResultId = UUID.randomUUID();
    private final UUID teacherId = UUID.randomUUID();

    @AfterEach
    void cleanUp() {
        transactionTemplate.executeWithoutResult(status ->
            repository.deleteByCandidateResultIdIn(List.of(candidateResultId)));
    }

    private UUID givenOpenAssignment() {
        return transactionTemplate.execute(status -> repository.save(AssessmentGradingAssignment.open(
            candidateResultId, teacherId, GradingRoundType.INITIAL, null, null,
            Instant.now(), teacherId, null)).getId());
    }

    @Test
    void should_make_the_second_writer_wait_and_see_the_completed_assignment() throws Exception {
        UUID assignmentId = givenOpenAssignment();
        CountDownLatch firstHoldsLock = new CountDownLatch(1);
        CountDownLatch secondStarted = new CountDownLatch(1);

        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            Future<?> first = pool.submit(() -> transactionTemplate.executeWithoutResult(status -> {
                AssessmentGradingAssignment assignment = repository.findByIdForUpdate(assignmentId).orElseThrow();
                firstHoldsLock.countDown();
                // Hold the lock until the second thread has certainly rushed in and is blocked.
                awaitQuietly(secondStarted);
                assignment.complete(GradingOutcome.UPHELD, null, Instant.now());
                repository.save(assignment);
            }));

            Future<GradingAssignmentStatus> second = pool.submit(() -> {
                awaitQuietly(firstHoldsLock);
                secondStarted.countDown();
                return transactionTemplate.execute(status ->
                    repository.findByIdForUpdate(assignmentId).orElseThrow().getStatus());
            });

            first.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            // Being able to wait means it re-reads after thread one commits, rather than reading an
            // old snapshot and grading over it.
            assertThat(second.get(TIMEOUT_SECONDS, TimeUnit.SECONDS))
                .isEqualTo(GradingAssignmentStatus.COMPLETED);
        }

        AssessmentGradingAssignment persisted = transactionTemplate.execute(status ->
            repository.findById(assignmentId).orElseThrow());
        assertThat(persisted.getOutcome()).isEqualTo(GradingOutcome.UPHELD);
        assertThat(persisted.getActiveResultId()).isNull();
    }

    @Test
    void should_read_a_stale_snapshot_when_the_write_path_skips_the_lock() throws Exception {
        // The flip side, so that the reason for using loadForUpdate is not seen as redundant by a
        // later reader: a plain findById can still read an old snapshot while another thread is
        // mid-grading. This is exactly the case @Version used to catch, and the reason the WRITE
        // path must lock.
        UUID assignmentId = givenOpenAssignment();
        CountDownLatch firstHoldsLock = new CountDownLatch(1);
        CountDownLatch secondFinished = new CountDownLatch(1);

        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            Future<?> first = pool.submit(() -> transactionTemplate.executeWithoutResult(status -> {
                AssessmentGradingAssignment assignment = repository.findByIdForUpdate(assignmentId).orElseThrow();
                assignment.complete(GradingOutcome.UPHELD, null, Instant.now());
                repository.save(assignment);
                firstHoldsLock.countDown();
                awaitQuietly(secondFinished);
            }));

            Future<GradingAssignmentStatus> second = pool.submit(() -> {
                awaitQuietly(firstHoldsLock);
                try {
                    return transactionTemplate.execute(status ->
                        repository.findById(assignmentId).orElseThrow().getStatus());
                } finally {
                    secondFinished.countDown();
                }
            });

            assertThat(second.get(TIMEOUT_SECONDS, TimeUnit.SECONDS))
                .isEqualTo(GradingAssignmentStatus.ASSIGNED);
            first.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            if (!latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for the other thread -- the lock is most likely stuck.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
