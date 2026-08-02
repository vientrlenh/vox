package com.sep.vox.infrastructure.persistence.adapter;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.personalization.WeaknessFrequency;
import com.sep.vox.domain.model.personalization.WeaknessObservation;
import com.sep.vox.domain.repository.WeaknessObservationRepository;
import com.sep.vox.infrastructure.persistence.entity.WeaknessObservationJpaEntity;
import com.sep.vox.infrastructure.persistence.repository.SpringDataWeaknessObservationRepository;

@Repository
public class WeaknessObservationRepositoryImpl implements WeaknessObservationRepository {

    private final SpringDataWeaknessObservationRepository repository;

    public WeaknessObservationRepositoryImpl(
            SpringDataWeaknessObservationRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean existsForKey(
            UUID sourceEvaluationId,
            UUID frameworkCriterionId,
            String subAttribute,
            String evidenceSpan) {
        return repository
            .existsBySourceEvaluationIdAndFrameworkCriterionIdAndSubAttributeAndEvidenceSpan(
                sourceEvaluationId,
                frameworkCriterionId,
                subAttribute,
                evidenceSpan
            );
    }

    @Override
    public void save(WeaknessObservation observation) {
        repository.save(new WeaknessObservationJpaEntity(
            observation.getId(),
            observation.getStudentId(),
            observation.getSourceType().name(),
            observation.getSourceEvaluationId(),
            observation.getFrameworkCriterionId(),
            observation.getCriterionCode(),
            observation.getSubAttribute(),
            observation.getEvidenceSpan(),
            observation.getObservedAt()
        ));
    }

    @Override
    public List<WeaknessFrequency> findWeaknessFrequencies(
            List<UUID> studentIds,
            OffsetDateTime windowStart,
            OffsetDateTime recentWindowStart) {
        if (studentIds.isEmpty()) {
            return List.of();
        }
        return repository
            .findWeaknessFrequencies(studentIds, windowStart, recentWindowStart).stream()
            .map(row -> new WeaknessFrequency(
                row.getStudentId(),
                row.getFrameworkCriterionId(),
                row.getSubAttribute(),
                row.getFrequency(),
                row.getRecentFrequency()
            ))
            .toList();
    }
}
