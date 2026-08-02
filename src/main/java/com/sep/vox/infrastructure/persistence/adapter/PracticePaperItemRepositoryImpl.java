package com.sep.vox.infrastructure.persistence.adapter;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.personalization.PracticePaperItem;
import com.sep.vox.domain.repository.personalization.PracticePaperItemRepository;
import com.sep.vox.infrastructure.persistence.entity.PracticePaperItemJpaEntity;
import com.sep.vox.infrastructure.persistence.repository.SpringDataPracticePaperItemRepository;

@Repository
public class PracticePaperItemRepositoryImpl implements PracticePaperItemRepository {

    private final SpringDataPracticePaperItemRepository repository;

    public PracticePaperItemRepositoryImpl(SpringDataPracticePaperItemRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(PracticePaperItem item) {
        repository.save(new PracticePaperItemJpaEntity(
            item.id(),
            item.practicePaperId(),
            item.practiceQuestionId(),
            item.slotOrder(),
            item.targetCriterionCode(),
            item.targetSubAttribute(),
            item.targetDifficultyRank()
        ));
    }

    @Override
    public int sumPlannedSecondsForPaper(UUID paperId) {
        return repository.sumPlannedSecondsForPaper(paperId);
    }

    @Override
    public List<UUID> findQuestionIdsForPaper(UUID paperId) {
        return repository.findQuestionIdsForPaper(paperId);
    }

    @Override
    public int countItemsForPaper(UUID paperId) {
        return repository.countByPracticePaperId(paperId);
    }
}
