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
 * "Mỗi bài tối đa MỘT phân công đang mở" là bất biến của cả tính năng chấm bài, và
 * chốt cuối của nó là unique index trên {@code active_result_id} — pre-check ở use
 * case chỉ để báo lỗi đọc được, nó không chặn được race. Chỉ chạy thật trên DB mới
 * kiểm được.
 *
 * <p>Điểm tinh tế nhất của thiết kế nằm ở đây: bất biến phải cho phép NHIỀU dòng đã
 * đóng của cùng một bài (bài đi qua bốn vòng), nên unique không thể đặt trên
 * {@code candidate_result_id} như bản cũ.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestGrpcTransport
@Transactional
class SpringDataExamGradingAssignmentRepositoryTests extends ContainerTestConfig {

    @Autowired
    private SpringDataExamGradingAssignmentRepository repository;

    private ExamGradingAssignmentJpaEntity open(UUID candidateResultId, UUID teacherId, String roundType) {
        return new ExamGradingAssignmentJpaEntity(
            null,
            candidateResultId,
            teacherId,
            roundType,
            null,
            "ASSIGNED",
            null,
            null,
            OffsetDateTime.parse("2026-07-26T09:00:00+07:00"),
            UUID.randomUUID(),
            null,
            null,
            null,
            null,
            candidateResultId,
            0
        );
    }

    private ExamGradingAssignmentJpaEntity completed(
            UUID candidateResultId, UUID teacherId, String roundType, String outcome) {
        return new ExamGradingAssignmentJpaEntity(
            null,
            candidateResultId,
            teacherId,
            roundType,
            null,
            "COMPLETED",
            outcome,
            null,
            OffsetDateTime.parse("2026-07-26T09:00:00+07:00"),
            UUID.randomUUID(),
            OffsetDateTime.parse("2026-07-26T10:00:00+07:00"),
            null,
            null,
            null,
            // COMPLETED nhả active_result_id về NULL — đây là cơ chế cho phép vòng sau.
            null,
            0
        );
    }

    @Test
    void should_generate_uuidv7_id_on_insert() {
        var saved = repository.saveAndFlush(open(UUID.randomUUID(), UUID.randomUUID(), "INITIAL"));

        assertThat(saved.getId()).isNotNull();
    }

    @Test
    void should_reject_a_second_open_assignment_for_the_same_result() {
        var candidateResultId = UUID.randomUUID();
        repository.saveAndFlush(open(candidateResultId, UUID.randomUUID(), "INITIAL"));

        assertThatThrownBy(() ->
            repository.saveAndFlush(open(candidateResultId, UUID.randomUUID(), "SPOT_CHECK")))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void should_allow_many_completed_assignments_for_the_same_result() {
        var candidateResultId = UUID.randomUUID();
        repository.saveAndFlush(completed(candidateResultId, UUID.randomUUID(), "INITIAL", "UPHELD"));
        repository.saveAndFlush(completed(candidateResultId, UUID.randomUUID(), "SPOT_CHECK", "REGRADED"));

        // Postgres coi các NULL là KHÁC NHAU, nên unique index thường trên
        // active_result_id cho đúng ngữ nghĩa của partial index WHERE status='ASSIGNED'.
        assertThat(repository.findByCandidateResultIdOrderByAssignedAtDesc(candidateResultId)).hasSize(2);
    }

    @Test
    void should_allow_reopening_a_result_after_the_previous_round_is_closed() {
        var candidateResultId = UUID.randomUUID();
        repository.saveAndFlush(completed(candidateResultId, UUID.randomUUID(), "REMEDIATION", "CLEARED_INVALID"));

        // Chính là kịch bản CLEAR_INVALID: đóng vòng cũ rồi mở vòng INITIAL mới.
        repository.saveAndFlush(open(candidateResultId, UUID.randomUUID(), "INITIAL"));

        assertThat(repository.findByActiveResultId(candidateResultId)).isPresent();
    }

    @Test
    void should_allow_the_same_teacher_to_hold_many_results() {
        var teacherId = UUID.randomUUID();
        repository.saveAndFlush(open(UUID.randomUUID(), teacherId, "INITIAL"));
        repository.saveAndFlush(open(UUID.randomUUID(), teacherId, "SPOT_CHECK"));

        assertThat(repository.findAll()).hasSize(2);
    }

    @Test
    void should_reject_an_unknown_status() {
        var entity = open(UUID.randomUUID(), UUID.randomUUID(), "INITIAL");
        entity.setStatus("IN_PROGRESS");

        assertThatThrownBy(() -> repository.saveAndFlush(entity))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void should_reject_an_unknown_round_type() {
        assertThatThrownBy(() ->
            repository.saveAndFlush(open(UUID.randomUUID(), UUID.randomUUID(), "SECOND_OPINION")))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void should_reject_an_unknown_outcome() {
        assertThatThrownBy(() -> repository.saveAndFlush(
            completed(UUID.randomUUID(), UUID.randomUUID(), "INITIAL", "PARTIALLY_UPHELD")))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void should_find_the_open_assignment_of_a_result() {
        var candidateResultId = UUID.randomUUID();
        repository.saveAndFlush(open(candidateResultId, UUID.randomUUID(), "INITIAL"));

        assertThat(repository.findByActiveResultId(candidateResultId)).isPresent();
        assertThat(repository.findByActiveResultId(UUID.randomUUID())).isEmpty();
    }

    @Test
    void should_batch_load_open_assignments_for_many_results() {
        var first = UUID.randomUUID();
        var second = UUID.randomUUID();
        repository.saveAndFlush(open(first, UUID.randomUUID(), "INITIAL"));
        repository.saveAndFlush(completed(second, UUID.randomUUID(), "INITIAL", "UPHELD"));
        repository.saveAndFlush(open(UUID.randomUUID(), UUID.randomUUID(), "INITIAL"));

        // Chỉ dòng đang mở lọt vào: dòng đã đóng của `second` mang active_result_id NULL.
        assertThat(repository.findByActiveResultIdIn(List.of(first, second))).hasSize(1);
    }

    @Test
    void should_find_overdue_assignments_only_while_they_are_still_open() {
        var overdue = open(UUID.randomUUID(), UUID.randomUUID(), "INITIAL");
        overdue.setDeadlineAt(OffsetDateTime.parse("2026-07-20T09:00:00+07:00"));
        repository.saveAndFlush(overdue);

        var closedButPastDeadline = completed(UUID.randomUUID(), UUID.randomUUID(), "INITIAL", "UPHELD");
        closedButPastDeadline.setDeadlineAt(OffsetDateTime.parse("2026-07-20T09:00:00+07:00"));
        repository.saveAndFlush(closedButPastDeadline);

        var now = OffsetDateTime.parse("2026-07-26T09:00:00+07:00");
        assertThat(repository.findOverdue(now)).hasSize(1);
        // Chưa nhắc lần nào thì cũng nằm trong danh sách cần nhắc.
        assertThat(repository.findDueForReminder(now)).hasSize(1);
    }

    @Test
    void should_not_remind_twice_for_the_same_assignment() {
        var overdue = open(UUID.randomUUID(), UUID.randomUUID(), "INITIAL");
        overdue.setDeadlineAt(OffsetDateTime.parse("2026-07-20T09:00:00+07:00"));
        overdue.setRemindedAt(OffsetDateTime.parse("2026-07-21T09:00:00+07:00"));
        repository.saveAndFlush(overdue);

        var now = OffsetDateTime.parse("2026-07-26T09:00:00+07:00");
        assertThat(repository.findOverdue(now)).hasSize(1);
        assertThat(repository.findDueForReminder(now)).isEmpty();
    }
}
