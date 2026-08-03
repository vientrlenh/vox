package com.sep.vox.infrastructure.persistence.adapter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.personalization.WeaknessScoreObservation;
import com.sep.vox.domain.repository.personalization.WeaknessScoreObservationViewRepository;
import com.sep.vox.infrastructure.persistence.repository.SpringDataWeaknessScoreObservationViewRepository;

@Repository
public class WeaknessScoreObservationViewRepositoryImpl
        implements WeaknessScoreObservationViewRepository {

    private final SpringDataWeaknessScoreObservationViewRepository repository;

    public WeaknessScoreObservationViewRepositoryImpl(
            SpringDataWeaknessScoreObservationViewRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<WeaknessScoreObservation> findAllValidScoreObservations() {
        return repository.findAllValidScoreObservations().stream()
            .map(row -> new WeaknessScoreObservation(
                row.getStudentId(),
                row.getFrameworkCriterionId(),
                row.getCriterionCode(),
                row.getFinalScore(),
                row.getMinScore(),
                row.getMaxScore(),
                row.getEvaluatedAt(),
                row.getSourceType(),
                row.getEvaluationId(),
                row.getSchoolClassId(),
                row.getSchoolGradeId()
            ))
            .toList();
    }

    @Override
    public List<UUID> findStudentsNeedingRefresh(Instant staleBefore, int limit) {
        return repository.findStudentsNeedingRefresh(staleBefore, limit);
    }
}
