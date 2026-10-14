package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.framework.FrameworkCriterion;
import com.sep.vox.domain.repository.FrameworkCriterionRepository;
import com.sep.vox.infrastructure.persistence.entity.FrameworkCriterionJpaEntity;
import com.sep.vox.infrastructure.persistence.mapper.FrameworkCriterionMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataFrameworkCriterionRepository;

import lombok.RequiredArgsConstructor;


@Repository
@RequiredArgsConstructor 
public class FrameworkCriterionRepositoryImpl implements FrameworkCriterionRepository {

    private final SpringDataFrameworkCriterionRepository springDataFrameworkCriterionRepository;

    @Override
    public List<FrameworkCriterion> saveAll(Collection<FrameworkCriterion> criteria) {
        List<FrameworkCriterionJpaEntity> entities = criteria.stream()
            .map(FrameworkCriterionMapper::toJpa)
            .toList();
        List<FrameworkCriterionJpaEntity> saved = springDataFrameworkCriterionRepository.saveAll(entities);
        return saved.stream()
            .map(FrameworkCriterionMapper::toDomain)
            .toList();
    }

}
