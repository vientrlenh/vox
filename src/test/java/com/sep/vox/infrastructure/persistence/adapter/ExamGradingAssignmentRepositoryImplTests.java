package com.sep.vox.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.grpc.test.autoconfigure.AutoConfigureTestGrpcTransport;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.config.ContainerTestConfig;
import com.sep.vox.domain.model.exam.ExamGradingAssignment;
import com.sep.vox.domain.model.exam.GradingOutcome;
import com.sep.vox.domain.model.exam.GradingRoundType;
import com.sep.vox.domain.repository.ExamGradingAssignmentRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Đóng vòng cũ rồi mở vòng mới cho CÙNG một bài, trong CÙNG một transaction, là hình
 * dạng chung của {@code ClearInvalidResultUseCase} và nhánh giao-lại của
 * {@code ReclaimOverdueAssignmentsUseCase}. Đó cũng là chỗ duy nhất unique index
 * {@code uq_grading_assignment_active_result} có thể nổ vì lý do không hiển nhiên:
 * {@code ActionQueue} của Hibernate chạy INSERT trước UPDATE, nên dòng mới sẽ chạm DB
 * khi dòng cũ còn đang giữ {@code active_result_id}.
 *
 * <p>Thứ giữ cho nó không nổ là {@code saveAndFlush}/{@code saveAllAndFlush} trong
 * {@code ExamGradingAssignmentRepositoryImpl} — một chi tiết trông như thừa với người
 * đọc sau, và đổi về {@code save} thì mọi test mock vẫn xanh. Lớp này là chỗ duy nhất
 * bắt được việc đó.
 *
 * <p>{@code SpringDataExamGradingAssignmentRepositoryTests} không phủ được: nó lưu các
 * entity MỚI, tức không có UPDATE nào để xếp sai thứ tự.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestGrpcTransport
@Transactional
class ExamGradingAssignmentRepositoryImplTests extends ContainerTestConfig {

    @Autowired
    private ExamGradingAssignmentRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    private final UUID candidateResultId = UUID.randomUUID();
    private final UUID teacherId = UUID.randomUUID();
    private final OffsetDateTime now = OffsetDateTime.now();

    private ExamGradingAssignment givenOpenRound(GradingRoundType roundType) {
        repository.save(ExamGradingAssignment.open(
            candidateResultId, teacherId, roundType, null, null, now, teacherId, null));
        // Đẩy dòng đầu xuống DB rồi quên đi: transaction thật của use case bắt đầu với
        // một persistence context rỗng và đọc dòng này lên từ DB.
        entityManager.flush();
        entityManager.clear();
        return repository.findOpenByCandidateResultId(candidateResultId).orElseThrow();
    }

    @Test
    void should_reopen_a_result_in_the_same_transaction_that_closed_the_previous_round() {
        var previous = givenOpenRound(GradingRoundType.REMEDIATION);

        // Đúng thứ tự của ClearInvalidResultUseCase, không flush ở giữa.
        previous.complete(GradingOutcome.CLEARED_INVALID, "Không vi phạm.", now);
        repository.save(previous);
        repository.save(ExamGradingAssignment.open(
            candidateResultId, teacherId, GradingRoundType.INITIAL, null, null, now, teacherId, null));

        entityManager.flush();
        entityManager.clear();

        var reopened = repository.findOpenByCandidateResultId(candidateResultId).orElseThrow();
        assertThat(reopened.getRoundType()).isEqualTo(GradingRoundType.INITIAL);
        assertThat(repository.findByCandidateResultIdOrderByAssignedAtDesc(candidateResultId)).hasSize(2);
    }

    @Test
    void should_reopen_a_result_through_save_all_in_the_same_transaction() {
        var previous = givenOpenRound(GradingRoundType.APPEAL);

        // Hình dạng của ReclaimOverdueAssignmentsUseCase: đóng lẻ, mở theo lô.
        previous.complete(GradingOutcome.DECLINED, "Thu hồi do quá hạn chấm.", now);
        repository.save(previous);
        repository.saveAll(List.of(ExamGradingAssignment.open(
            candidateResultId, UUID.randomUUID(), GradingRoundType.APPEAL, null, null, now, teacherId, null)));

        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findOpenByCandidateResultId(candidateResultId)).isPresent();
        assertThat(repository.findByCandidateResultIdOrderByAssignedAtDesc(candidateResultId)).hasSize(2);
    }
}
