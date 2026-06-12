package com.sep.vox.infrastructure.persistence.adapter;

import com.sep.vox.domain.model.rubric.RubricApplicability;
import com.sep.vox.domain.model.rubric.RubricCriterion;
import com.sep.vox.domain.repository.RubricApplicabilityRepository;
import com.sep.vox.infrastructure.persistence.mapper.RubricApplicabilityMapper;
import com.sep.vox.infrastructure.persistence.mapper.RubricCriterionMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataRubricApplicabilityRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public class RubricApplicabilityRepositoryImpl implements RubricApplicabilityRepository {

    private final SpringDataRubricApplicabilityRepository springDataRubricApplicabilityRepository;

    public RubricApplicabilityRepositoryImpl(SpringDataRubricApplicabilityRepository springDataRubricApplicabilityRepository) {
        this.springDataRubricApplicabilityRepository = springDataRubricApplicabilityRepository;
    }

    @Override
    public void saveAll(List<RubricApplicability> rubricApplicabilities) {
        var entities = rubricApplicabilities.stream()
                .map(RubricApplicabilityMapper::toJpa)
                .toList();

        springDataRubricApplicabilityRepository.saveAll(entities);
    }
}
