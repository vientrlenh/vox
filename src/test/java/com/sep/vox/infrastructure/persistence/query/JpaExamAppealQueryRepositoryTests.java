package com.sep.vox.infrastructure.persistence.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.grpc.test.autoconfigure.AutoConfigureTestGrpcTransport;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.config.ContainerTestConfig;
import com.sep.vox.infrastructure.persistence.entity.ExamCandidateJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.ExamCandidateResultJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.ExamJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.ExamPaperItemJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.ExamPaperSectionJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.ExamResultAppealItemJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.ExamResultAppealJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.RubricVersionJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.UserJpaEntity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Đơn phúc khảo có N phần thi, nên mọi query phân trang phải giữ đúng một dòng
 * mỗi đơn. Join to-many vào query phân trang là lỗi im lặng: đơn nhiều phần ăn
 * nhiều slot và lệch với query COUNT — chỉ DB thật mới lộ ra.
 *
 * <p>Các query ở đây là chuỗi JPQL dựng bằng EntityManager, không được compiler
 * kiểm; sai tên entity/field chỉ nổ lúc chạy.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestGrpcTransport
@Transactional
class JpaExamAppealQueryRepositoryTests extends ContainerTestConfig {

    @Autowired
    private JpaExamAppealQueryRepository repository;

    @PersistenceContext
    private EntityManager em;

    private UUID schoolId;
    private UUID examId;
    private UUID paperId;
    private UUID sectionId;
    private UUID rubricVersionId;
    private OffsetDateTime now;

    // Id của mọi entity đều là uuidv7 do DB sinh (@Generated(INSERT), insertable=false),
    // nên fixture phải persist với id null rồi đọc lại — truyền id sẵn bị coi là detached.
    private <T> T persisted(T entity) {
        em.persist(entity);
        em.flush();
        return entity;
    }

    @BeforeEach
    void setUp() {
        schoolId = UUID.randomUUID();
        paperId = UUID.randomUUID();
        now = OffsetDateTime.parse("2026-07-15T09:00:00+07:00");

        examId = persisted(new ExamJpaEntity(null, null, null, "EX-1", "IELTS Speaking Mock", null, schoolId,
            UUID.randomUUID(), "CLASS_TEST", "STUDENT_DEVICE", "RESULTS_PUBLISHED", 1, "LATEST",
            null, null, null, false, now, now, null, null)).getId();
        sectionId = persisted(new ExamPaperSectionJpaEntity(
            null, paperId, 1, "Part 2", null, null, BigDecimal.ONE, now, now, null, null)).getId();
        rubricVersionId = persisted(new RubricVersionJpaEntity(null, UUID.randomUUID(), 1, "RB-1", "Rubric",
            null, "PUBLISHED", now, null, new BigDecimal("0.00"), new BigDecimal("9.00"), "WEIGHTED_AVERAGE",
            now, now, null, null)).getId();
    }

    /** Một đơn có {@code partCount} phần thi, kèm toàn bộ dây chuyền join của nó. */
    private UUID appealWithItems(String studentName, int partCount) {
        var studentId = persisted(new UserJpaEntity(null, studentName + "@test.local", "hash", null,
            studentName, null, LocalDate.parse("2008-01-01"), null, null, "ACTIVE", now, now, null, null))
            .getId();
        var candidateId = persisted(new ExamCandidateJpaEntity(
            null, examId, studentId, paperId, null, "ASSIGNED", now, now, null, null, null)).getId();
        var resultId = persisted(new ExamCandidateResultJpaEntity(null, examId, candidateId, UUID.randomUUID(),
            UUID.randomUUID(), 1, rubricVersionId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
            new BigDecimal("6.00"), "RELEASED", now, null, now, now, null, null)).getId();

        var appealId = persisted(new ExamResultAppealJpaEntity(null, resultId, studentId, "Lý do", now,
            "PENDING", new BigDecimal("6.00"), null, null, null, null, null, null, null)).getId();

        for (var i = 0; i < partCount; i++) {
            var paperItemId = persisted(new ExamPaperItemJpaEntity(
                null, null, sectionId, paperId, UUID.randomUUID(), i + 1, BigDecimal.ONE)).getId();
            persisted(new ExamResultAppealItemJpaEntity(
                null, appealId, paperItemId, UUID.randomUUID(), null));
        }
        return appealId;
    }

    @Test
    void should_paginate_appeals_by_appeal_not_by_item() {
        appealWithItems("Nguyen Van A", 3);
        appealWithItems("Tran Thi B", 1);

        var page = repository.searchAppeals(schoolId, null, null, 0, 1);

        // Đơn 3 phần vẫn chỉ chiếm 1 slot, và COUNT vẫn đếm theo đơn.
        assertThat(page.totalElements()).isEqualTo(2);
        assertThat(page.content()).hasSize(1);
        assertThat(page.totalPages()).isEqualTo(2);
    }

    @Test
    void should_return_every_appeal_exactly_once_across_pages() {
        appealWithItems("Nguyen Van A", 3);
        appealWithItems("Tran Thi B", 2);

        var all = repository.searchAppeals(schoolId, null, null, 0, 20);

        assertThat(all.content()).hasSize(2);
        assertThat(all.content()).extracting(row -> row.id()).doesNotHaveDuplicates();
    }

    @Test
    void should_expose_part_labels_of_all_appealed_items() {
        var appealId = appealWithItems("Nguyen Van A", 3);

        var page = repository.searchAppeals(schoolId, null, null, 0, 20);

        var summary = page.content().stream()
            .filter(row -> row.id().equals(appealId)).findFirst().orElseThrow();
        // 3 phần cùng một section thì nhãn trùng, phải khử về một.
        assertThat(summary.partLabels()).containsExactly("Part 2");
    }

    @Test
    void should_load_every_appealed_item_in_detail() {
        var appealId = appealWithItems("Nguyen Van A", 3);

        var detail = repository.findDetailById(appealId, schoolId);

        assertThat(detail).isPresent();
        assertThat(detail.get().items()).hasSize(3);
        assertThat(detail.get().items()).allSatisfy(item -> {
            assertThat(item.partLabel()).isEqualTo("Part 2");
            assertThat(item.aiScores()).isEmpty();
            assertThat(item.turns()).isEmpty();
            assertThat(item.finalScore()).isNull();
        });
        assertThat(detail.get().reviewers()).isEmpty();
    }

    @Test
    void should_not_return_detail_of_another_school() {
        var appealId = appealWithItems("Nguyen Van A", 1);

        assertThat(repository.findDetailById(appealId, UUID.randomUUID())).isEmpty();
    }

    @Test
    void should_deny_task_detail_to_a_reviewer_who_is_not_assigned() {
        var appealId = appealWithItems("Nguyen Van A", 2);

        // Query phân quyền chạy một mình và trước mọi query khác: không có dòng ⇒ 403.
        assertThat(repository.findTaskDetail(appealId, UUID.randomUUID())).isEmpty();
    }

    @Test
    void should_run_every_reviewer_scoped_query() {
        appealWithItems("Nguyen Van A", 2);

        // Khói: các JPQL này là chuỗi, sai tên field chỉ nổ lúc chạy.
        assertThat(repository.findTasksByReviewerId(UUID.randomUUID(), null, 0, 20).content()).isEmpty();
        assertThat(repository.findAssignableReviewers(schoolId, "Lan")).isEmpty();
        assertThat(repository.countByStatus(schoolId).pending()).isEqualTo(1);
    }
}
