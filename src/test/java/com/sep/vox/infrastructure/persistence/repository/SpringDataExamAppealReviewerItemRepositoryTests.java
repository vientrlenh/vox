package com.sep.vox.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.grpc.test.autoconfigure.AutoConfigureTestGrpcTransport;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.config.ContainerTestConfig;
import com.sep.vox.infrastructure.persistence.entity.ExamAppealReviewerItemJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.ExamAppealReviewerJpaEntity;

/**
 * Báo cáo chấm lại theo từng phần thi. Xoá theo đơn phải bắc cầu qua bảng
 * exam_appeal_reviewers bằng subquery — chỉ chạy thật trên DB mới kiểm được.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestGrpcTransport
@Transactional
class SpringDataExamAppealReviewerItemRepositoryTests extends ContainerTestConfig {

    @Autowired
    private SpringDataExamAppealReviewerItemRepository repository;

    @Autowired
    private SpringDataExamAppealReviewerRepository reviewerRepository;

    private UUID appealId;

    @BeforeEach
    void setUp() {
        appealId = UUID.randomUUID();
    }

    @Test
    void should_generate_uuidv7_id_on_insert() {
        var reviewer = saveReviewer();

        var saved = repository.saveAndFlush(reviewerItem(reviewer.getId(), UUID.randomUUID()));

        assertThat(saved.getId()).isNotNull();
    }

    @Test
    void should_find_items_of_a_reviewer() {
        var reviewer = saveReviewer();
        var other = saveReviewer();
        repository.saveAndFlush(reviewerItem(reviewer.getId(), UUID.randomUUID()));
        repository.saveAndFlush(reviewerItem(reviewer.getId(), UUID.randomUUID()));
        repository.saveAndFlush(reviewerItem(other.getId(), UUID.randomUUID()));

        assertThat(repository.findByAppealReviewerIdOrderByIdAsc(reviewer.getId())).hasSize(2);
    }

    @Test
    void should_delete_reviewer_items_by_appeal_ids() {
        var reviewer = saveReviewer();
        repository.saveAndFlush(reviewerItem(reviewer.getId(), UUID.randomUUID()));
        repository.saveAndFlush(reviewerItem(reviewer.getId(), UUID.randomUUID()));

        repository.deleteByAppealIdIn(List.of(appealId));
        repository.flush();

        assertThat(repository.findByAppealReviewerIdOrderByIdAsc(reviewer.getId())).isEmpty();
    }

    @Test
    void should_not_delete_reviewer_items_of_other_appeals() {
        var reviewer = saveReviewer();
        repository.saveAndFlush(reviewerItem(reviewer.getId(), UUID.randomUUID()));

        repository.deleteByAppealIdIn(List.of(UUID.randomUUID()));
        repository.flush();

        assertThat(repository.findByAppealReviewerIdOrderByIdAsc(reviewer.getId())).hasSize(1);
    }

    private ExamAppealReviewerJpaEntity saveReviewer() {
        return reviewerRepository.saveAndFlush(new ExamAppealReviewerJpaEntity(
            null,
            appealId,
            UUID.randomUUID(),
            "ASSIGNED",
            OffsetDateTime.parse("2026-07-15T09:00:00+07:00"),
            UUID.randomUUID(),
            null
        ));
    }

    private ExamAppealReviewerItemJpaEntity reviewerItem(UUID appealReviewerId, UUID appealItemId) {
        return new ExamAppealReviewerItemJpaEntity(
            null, appealReviewerId, appealItemId, UUID.randomUUID(), new BigDecimal("7.50"), "ok");
    }
}
