package com.sep.vox.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import com.sep.vox.domain.model.personalization.PracticePaper;
import com.sep.vox.domain.model.personalization.PracticePaperItem;
import com.sep.vox.domain.repository.PracticePaperItemRepository;
import com.sep.vox.domain.repository.PracticePaperRepository;
import com.sep.vox.infrastructure.persistence.entity.personalization.PracticePaperItemJpaEntity;
import com.sep.vox.support.RepositoryTest;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@RepositoryTest
@Import({ PracticePaperRepositoryImpl.class, PracticePaperItemRepositoryImpl.class })
class PracticePaperIdTests {

    @Autowired
    private PracticePaperRepository paperRepository;

    @Autowired
    private PracticePaperItemRepository paperItemRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void should_store_version_7_ids_for_a_paper_and_its_item_saved_with_null_ids() {
        PracticePaper savedPaper = paperRepository.save(newPaper());
        paperItemRepository.save(newItem(savedPaper.getId()));
        entityManager.flush();
        entityManager.clear();

        List<PracticePaperItemJpaEntity> items = entityManager
            .createQuery("select i from PracticePaperItemJpaEntity i", PracticePaperItemJpaEntity.class)
            .getResultList();

        assertThat(savedPaper.getId()).isNotNull();
        assertThat(savedPaper.getId().version()).isEqualTo(7);
        assertThat(items).hasSize(1);
        assertThat(items.getFirst().getId().version()).isEqualTo(7);
        assertThat(items.getFirst().getPracticePaperId()).isEqualTo(savedPaper.getId());
    }

    private PracticePaper newPaper() {
        return PracticePaper.builder()
            .studentId(UUID.randomUUID())
            .practiceTopicId(UUID.randomUUID())
            .targetFrameworkBandId(UUID.randomUUID())
            .origin("SELECTED")
            .goalAtBuild("ABILITY_IMPROVEMENT")
            .offeredTopicIdsJson("[]")
            .previousOfferedTopicIdsJson("[]")
            .plannedSeconds(60)
            .reservedQuotaSeconds(60)
            .expiresAt(Instant.parse("2026-01-01T00:10:00Z"))
            .status("RESERVED")
            .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
            .build();
    }

    private PracticePaperItem newItem(UUID paperId) {
        return PracticePaperItem.builder()
            .practicePaperId(paperId)
            .practiceQuestionId(UUID.randomUUID())
            .slotOrder(1)
            .targetCriterionCode("FLUENCY")
            .targetDifficultyRank(1)
            .build();
    }
}
