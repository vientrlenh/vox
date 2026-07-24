package com.sep.vox.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.grpc.test.autoconfigure.AutoConfigureTestGrpcTransport;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.config.ContainerTestConfig;
import com.sep.vox.infrastructure.persistence.entity.ExamResultAppealItemJpaEntity;

/**
 * Bảng con của đơn phúc khảo dùng id uuidv7 do DB sinh và một unique index để chặn
 * trùng phần thi. Cả hai đều là hành vi của Postgres — mock không nhìn thấy, nên
 * phải kiểm bằng container thật.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestGrpcTransport
@Transactional
class SpringDataExamResultAppealItemRepositoryTests extends ContainerTestConfig {

    @Autowired
    private SpringDataExamResultAppealItemRepository repository;

    private UUID appealId;
    private UUID paperItemId;

    @BeforeEach
    void setUp() {
        appealId = UUID.randomUUID();
        paperItemId = UUID.randomUUID();
    }

    @Test
    void should_generate_uuidv7_id_on_insert() {
        var saved = repository.saveAndFlush(item(paperItemId, UUID.randomUUID(), null));

        assertThat(saved.getId()).isNotNull();
    }

    @Test
    void should_reject_duplicate_paper_item_in_one_appeal() {
        repository.saveAndFlush(item(paperItemId, UUID.randomUUID(), null));

        assertThatThrownBy(() -> repository.saveAndFlush(item(paperItemId, UUID.randomUUID(), null)))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void should_allow_same_paper_item_in_different_appeals() {
        repository.saveAndFlush(item(paperItemId, UUID.randomUUID(), null));

        var other = new ExamResultAppealItemJpaEntity(
            null, UUID.randomUUID(), paperItemId, UUID.randomUUID(), null);

        assertThat(repository.saveAndFlush(other).getId()).isNotNull();
    }

    @Test
    void should_find_items_of_an_appeal_in_insertion_order() {
        var first = repository.saveAndFlush(item(paperItemId, UUID.randomUUID(), null));
        var second = repository.saveAndFlush(
            item(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("7.50")));

        var found = repository.findByAppealIdOrderByIdAsc(appealId);

        assertThat(found).extracting(item -> item.getId())
            .containsExactly(first.getId(), second.getId());
        assertThat(found.get(1).getFinalScore()).isEqualByComparingTo("7.50");
    }

    @Test
    void should_delete_items_by_appeal_ids() {
        repository.saveAndFlush(item(paperItemId, UUID.randomUUID(), null));

        repository.deleteByAppealIdIn(List.of(appealId));
        repository.flush();

        assertThat(repository.findByAppealIdOrderByIdAsc(appealId)).isEmpty();
    }

    private ExamResultAppealItemJpaEntity item(UUID paperItem, UUID responseId, BigDecimal finalScore) {
        return new ExamResultAppealItemJpaEntity(null, appealId, paperItem, responseId, finalScore);
    }
}
