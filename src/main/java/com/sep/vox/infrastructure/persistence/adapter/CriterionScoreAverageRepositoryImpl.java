package com.sep.vox.infrastructure.persistence.adapter;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import com.sep.vox.domain.repository.CriterionScoreAverageRepository;
import com.sep.vox.infrastructure.persistence.repository.SpringDataCriterionScoreAverageRepository;

@Repository
@RequiredArgsConstructor
public class CriterionScoreAverageRepositoryImpl implements CriterionScoreAverageRepository {

    private final SpringDataCriterionScoreAverageRepository repository;

    @Override
    public List<String> findCriterionCodesOrderedByLowestAverageScore(UUID studentId) {
        return repository.findCriterionCodesOrderedByLowestAverageScore(studentId);
    }
}
