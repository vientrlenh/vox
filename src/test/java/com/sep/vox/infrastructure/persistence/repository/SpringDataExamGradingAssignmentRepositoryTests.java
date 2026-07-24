package com.sep.vox.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.grpc.test.autoconfigure.AutoConfigureTestGrpcTransport;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.config.ContainerTestConfig;
import com.sep.vox.infrastructure.persistence.entity.ExamGradingAssignmentJpaEntity;

/**
 * "Một bài, một giáo viên" là bất biến của tính năng chấm tay, và chốt cuối của nó
 * là unique index trên {@code candidate_result_id} — pre-check ở use case chỉ để
 * báo lỗi đọc được, nó không chặn được race. Chỉ chạy thật trên DB mới kiểm được.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestGrpcTransport
@Transactional
class SpringDataExamGradingAssignmentRepositoryTests extends ContainerTestConfig {

    @Autowired
    private SpringDataExamGradingAssignmentRepository repository;

    private ExamGradingAssignmentJpaEntity assignment(UUID candidateResultId, UUID teacherId, String status) {
        return new ExamGradingAssignmentJpaEntity(
            null,
            candidateResultId,
            teacherId,
            status,
            OffsetDateTime.parse("2026-07-24T09:00:00+07:00"),
            UUID.randomUUID(),
            null
        );
    }

    @Test
    void should_generate_uuidv7_id_on_insert() {
        var saved = repository.saveAndFlush(
            assignment(UUID.randomUUID(), UUID.randomUUID(), "ASSIGNED"));

        assertThat(saved.getId()).isNotNull();
    }

    @Test
    void should_reject_a_second_assignment_for_the_same_result() {
        var candidateResultId = UUID.randomUUID();
        repository.saveAndFlush(assignment(candidateResultId, UUID.randomUUID(), "ASSIGNED"));

        assertThatThrownBy(() ->
            repository.saveAndFlush(assignment(candidateResultId, UUID.randomUUID(), "ASSIGNED")))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void should_allow_the_same_teacher_to_hold_many_results() {
        var teacherId = UUID.randomUUID();
        repository.saveAndFlush(assignment(UUID.randomUUID(), teacherId, "ASSIGNED"));
        repository.saveAndFlush(assignment(UUID.randomUUID(), teacherId, "ASSIGNED"));

        // Unique nằm trên MỘT cột candidate_result_id, không phải cặp (bài, giáo viên).
        assertThat(repository.findAll()).hasSize(2);
    }

    @Test
    void should_reject_an_unknown_status() {
        assertThatThrownBy(() ->
            repository.saveAndFlush(assignment(UUID.randomUUID(), UUID.randomUUID(), "IN_PROGRESS")))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void should_find_assignment_by_candidate_result_id() {
        var candidateResultId = UUID.randomUUID();
        repository.saveAndFlush(assignment(candidateResultId, UUID.randomUUID(), "ASSIGNED"));

        assertThat(repository.findByCandidateResultId(candidateResultId)).isPresent();
        assertThat(repository.existsByCandidateResultId(candidateResultId)).isTrue();
        assertThat(repository.existsByCandidateResultId(UUID.randomUUID())).isFalse();
    }

    @Test
    void should_batch_load_assignments_for_many_results() {
        var first = UUID.randomUUID();
        var second = UUID.randomUUID();
        repository.saveAndFlush(assignment(first, UUID.randomUUID(), "ASSIGNED"));
        repository.saveAndFlush(assignment(second, UUID.randomUUID(), "COMPLETED"));
        repository.saveAndFlush(assignment(UUID.randomUUID(), UUID.randomUUID(), "ASSIGNED"));

        assertThat(repository.findByCandidateResultIdIn(List.of(first, second))).hasSize(2);
    }
}
