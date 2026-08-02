package com.sep.vox.infrastructure.persistence.adapter;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.model.personalization.LearnerWeaknessSnapshot;
import com.sep.vox.domain.repository.personalization.LearnerWeaknessSnapshotRepository;
import com.sep.vox.infrastructure.persistence.entity.LearnerWeaknessSnapshotJpaEntity;
import com.sep.vox.infrastructure.persistence.repository.SpringDataLearnerWeaknessSnapshotRepository;

@Repository
public class LearnerWeaknessSnapshotRepositoryImpl implements LearnerWeaknessSnapshotRepository {

    private final SpringDataLearnerWeaknessSnapshotRepository repository;

    public LearnerWeaknessSnapshotRepositoryImpl(
            SpringDataLearnerWeaknessSnapshotRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void replaceForStudents(
            List<UUID> studentIds,
            List<LearnerWeaknessSnapshot> snapshots) {
        if (studentIds.isEmpty()) {
            return;
        }
        repository.deleteByStudentIdIn(studentIds);
        repository.saveAll(snapshots.stream()
            .map(item -> new LearnerWeaknessSnapshotJpaEntity(
                item.getId(),
                item.getStudentId(),
                item.getFrameworkCriterionId(),
                item.getRelEstimate(),
                item.getWeakness(),
                item.getObservationCount(),
                item.isReliable(),
                item.getComputedAt()
            ))
            .toList());
    }

    @Override
    public List<String> findFocusCriterionCodesOrderedByWeakness(UUID studentId) {
        return repository.findFocusCriterionCodesOrderedByWeakness(studentId);
    }
}
