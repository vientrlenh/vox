package com.sep.vox.infrastructure.persistence.adapter;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.model.personalization.PracticePaperItem;
import com.sep.vox.domain.repository.PracticePaperItemRepository;
import com.sep.vox.infrastructure.persistence.entity.personalization.PracticePaperItemJpaEntity;
import com.sep.vox.infrastructure.persistence.repository.SpringDataPracticePaperItemRepository;

@Repository
public class PracticePaperItemRepositoryImpl implements PracticePaperItemRepository {

    private final SpringDataPracticePaperItemRepository repository;

    public PracticePaperItemRepositoryImpl(SpringDataPracticePaperItemRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(PracticePaperItem item) {
        repository.save(PracticePaperItemJpaEntity.builder()
                .id(item.getId())
                .practicePaperId(item.getPracticePaperId())
                .practiceQuestionId(item.getPracticeQuestionId())
                .slotOrder(item.getSlotOrder())
                .targetCriterionCode(item.getTargetCriterionCode())
                .targetSubAttribute(item.getTargetSubAttribute())
                .targetDifficultyRank(item.getTargetDifficultyRank())
                .build());
    }

    @Override
    public List<UUID> findQuestionIdsForPaper(UUID paperId) {
        return repository.findQuestionIdsForPaper(paperId);
    }

    @Override
    @Transactional
    public int deleteLastItemForPaper(UUID paperId, UUID questionId) {
        return repository.deleteLastItemForPaper(paperId, questionId);
    }
}
