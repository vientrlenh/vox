package com.sep.vox.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.grpc.test.autoconfigure.AutoConfigureTestGrpcTransport;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.model.assessment.AssessmentGradingAssignment;
import com.sep.vox.domain.model.assessment.GradingOutcome;
import com.sep.vox.domain.model.assessment.GradingRoundType;
import com.sep.vox.domain.repository.AssessmentGradingAssignmentRepository;
import com.sep.vox.support.IntegrationTest;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Closing the old round and opening a new round for the SAME attempt, in the SAME transaction, is the common shape
 * of {@code ClearInvalidResultUseCase} and the reassign branch of
 * {@code ReclaimOverdueAssignmentsUseCase}. It is also the only place where the unique index
 * {@code uq_grading_assignment_active_result} can blow up for a non-obvious reason:
 * Hibernate's {@code ActionQueue} runs INSERT before UPDATE, so the new row would hit the DB
 * while the old row is still holding {@code active_result_id}.
 *
 * <p>What keeps it from blowing up is {@code saveAndFlush}/{@code saveAllAndFlush} in
 * {@code AssessmentGradingAssignmentRepositoryImpl} -- a detail that looks redundant to a later
 * reader, and if it were changed back to {@code save} every mock test would stay green. This class is the only place
 * that catches that.
 *
 * <p>{@code SpringDataAssessmentGradingAssignmentRepositoryTests} cannot cover it: it saves
 * NEW entities, i.e. there is no UPDATE to order wrongly.
 */
@IntegrationTest
@AutoConfigureTestGrpcTransport
@Transactional
class AssessmentGradingAssignmentRepositoryImplTests {

    @Autowired
    private AssessmentGradingAssignmentRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    private final UUID candidateResultId = UUID.randomUUID();
    private final UUID teacherId = UUID.randomUUID();
    private final Instant now = Instant.now();

    private AssessmentGradingAssignment givenOpenRound(GradingRoundType roundType) {
        repository.save(AssessmentGradingAssignment.open(
            candidateResultId, teacherId, roundType, null, null, now, teacherId, null));
        // Đẩy dòng đầu xuống DB rồi quên đi: transaction thật của use case bắt đầu với
        // một persistence context rỗng và đọc dòng này lên từ DB.
        entityManager.flush();
        entityManager.clear();
        return repository.findOpenByCandidateResultId(candidateResultId).orElseThrow();
    }

    @Test
    void should_reopen_a_result_in_the_same_transaction_that_closed_the_previous_round() {
        AssessmentGradingAssignment previous = givenOpenRound(GradingRoundType.REMEDIATION);

        // Exactly the order of ClearInvalidResultUseCase, with no flush in between.
        previous.complete(GradingOutcome.CLEARED_INVALID, "Không vi phạm.", now);
        repository.save(previous);
        repository.save(AssessmentGradingAssignment.open(
            candidateResultId, teacherId, GradingRoundType.INITIAL, null, null, now, teacherId, null));

        entityManager.flush();
        entityManager.clear();

        AssessmentGradingAssignment reopened = repository.findOpenByCandidateResultId(candidateResultId).orElseThrow();
        assertThat(reopened.getRoundType()).isEqualTo(GradingRoundType.INITIAL);
        assertThat(repository.findByCandidateResultIdOrderByAssignedAtDesc(candidateResultId)).hasSize(2);
    }

    @Test
    void should_reopen_a_result_through_save_all_in_the_same_transaction() {
        AssessmentGradingAssignment previous = givenOpenRound(GradingRoundType.APPEAL);

        // The shape of ReclaimOverdueAssignmentsUseCase: close one by one, open in a batch.
        previous.complete(GradingOutcome.DECLINED, "Thu hồi do quá hạn chấm.", now);
        repository.save(previous);
        repository.saveAll(List.of(AssessmentGradingAssignment.open(
            candidateResultId, UUID.randomUUID(), GradingRoundType.APPEAL, null, null, now, teacherId, null)));

        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findOpenByCandidateResultId(candidateResultId)).isPresent();
        assertThat(repository.findByCandidateResultIdOrderByAssignedAtDesc(candidateResultId)).hasSize(2);
    }
}
