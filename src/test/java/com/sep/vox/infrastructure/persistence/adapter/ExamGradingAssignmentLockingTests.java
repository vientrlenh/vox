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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import com.sep.vox.config.ContainerTestConfig;
import com.sep.vox.domain.model.exam.ExamGradingAssignment;
import com.sep.vox.domain.model.exam.GradingAssignmentStatus;
import com.sep.vox.domain.model.exam.GradingOutcome;
import com.sep.vox.domain.model.exam.GradingRoundType;
import com.sep.vox.domain.repository.ExamGradingAssignmentRepository;

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
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestGrpcTransport
class ExamGradingAssignmentLockingTests extends ContainerTestConfig {

    /** Trần chờ cho mọi lần join luồng — để bug về khoá thành test đỏ, không thành build treo. */
    private static final int TIMEOUT_SECONDS = 20;

    @Autowired
    private ExamGradingAssignmentRepository repository;

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
        return transactionTemplate.execute(status -> repository.save(ExamGradingAssignment.open(
            candidateResultId, teacherId, GradingRoundType.INITIAL, null, null,
            Instant.now(), teacherId, null)).getId());
    }

    @Test
    void should_make_the_second_writer_wait_and_see_the_completed_assignment() throws Exception {
        var assignmentId = givenOpenAssignment();
        var firstHoldsLock = new CountDownLatch(1);
        var secondStarted = new CountDownLatch(1);

        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            Future<?> first = pool.submit(() -> transactionTemplate.executeWithoutResult(status -> {
                var assignment = repository.findByIdForUpdate(assignmentId).orElseThrow();
                firstHoldsLock.countDown();
                // Giữ khoá cho tới khi luồng hai chắc chắn đã lao vào và đang bị chặn.
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
            // Chờ được nghĩa là nó đọc lại sau khi luồng một commit, chứ không phải đọc
            // ảnh chụp cũ rồi chấm đè lên.
            assertThat(second.get(TIMEOUT_SECONDS, TimeUnit.SECONDS))
                .isEqualTo(GradingAssignmentStatus.COMPLETED);
        }

        var persisted = transactionTemplate.execute(status ->
            repository.findById(assignmentId).orElseThrow());
        assertThat(persisted.getOutcome()).isEqualTo(GradingOutcome.UPHELD);
        assertThat(persisted.getActiveResultId()).isNull();
    }

    @Test
    void should_read_a_stale_snapshot_when_the_write_path_skips_the_lock() throws Exception {
        // Mặt trái, để lý do dùng loadForUpdate không bị người đọc sau coi là thừa:
        // findById thường vẫn đọc được ảnh chụp cũ trong lúc luồng khác đang chấm dở.
        // Chính đây là ca mà @Version từng bắt, và là lý do đường GHI phải khoá.
        var assignmentId = givenOpenAssignment();
        var firstHoldsLock = new CountDownLatch(1);
        var secondFinished = new CountDownLatch(1);

        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            Future<?> first = pool.submit(() -> transactionTemplate.executeWithoutResult(status -> {
                var assignment = repository.findByIdForUpdate(assignmentId).orElseThrow();
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
