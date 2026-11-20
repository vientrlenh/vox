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
 * Khoá bi quan trên phân công chấm — thứ thay thế cột {@code version} đã bỏ.
 *
 * <p>Vì sao cần: domain được map ra POJO detached nên {@code save} là {@code merge} ghi
 * đè TOÀN BỘ cột, không có dirty checking. Hai transaction cùng đọc một dòng ASSIGNED
 * rồi cùng ghi thì cả hai UPDATE đều thành công và người sau xoá mất việc của người
 * trước — mà không lỗi, không log. {@code findByIdForUpdate} là chỗ duy nhất chặn.
 *
 * <p>Lớp này KHÔNG {@code @Transactional}: hai luồng phải nằm trong hai transaction
 * thật thì khoá mới có gì để chặn.
 */
@IntegrationTest
@AutoConfigureTestGrpcTransport
class AssessmentGradingAssignmentLockingTests {

    /** Trần chờ cho mọi lần join luồng — để bug về khoá thành test đỏ, không thành build treo. */
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
                throw new IllegalStateException("Quá hạn chờ luồng kia — nhiều khả năng khoá bị kẹt.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
