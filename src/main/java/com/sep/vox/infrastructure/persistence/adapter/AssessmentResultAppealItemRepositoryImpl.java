package com.sep.vox.infrastructure.persistence.adapter;

import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentResultAppealItemJpaEntity;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.assessment.AssessmentResultAppealItem;
import com.sep.vox.domain.repository.AssessmentResultAppealItemRepository;
import com.sep.vox.infrastructure.persistence.mapper.AssessmentResultAppealItemMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataAssessmentResultAppealItemRepository;

@Repository
public class AssessmentResultAppealItemRepositoryImpl implements AssessmentResultAppealItemRepository {

    private final SpringDataAssessmentResultAppealItemRepository springDataAssessmentResultAppealItemRepository;

    public AssessmentResultAppealItemRepositoryImpl(
            SpringDataAssessmentResultAppealItemRepository springDataAssessmentResultAppealItemRepository) {
        this.springDataAssessmentResultAppealItemRepository = springDataAssessmentResultAppealItemRepository;
    }

    @Override
    public List<AssessmentResultAppealItem> saveAll(List<AssessmentResultAppealItem> items) {
        List<AssessmentResultAppealItemJpaEntity> entities = items.stream().map(AssessmentResultAppealItemMapper::toJpa).toList();
        return springDataAssessmentResultAppealItemRepository.saveAll(entities).stream()
            .map(AssessmentResultAppealItemMapper::toDomain)
            .toList();
    }

    @Override
    public List<AssessmentResultAppealItem> findByAppealId(UUID appealId) {
        return springDataAssessmentResultAppealItemRepository.findByAppealIdOrderByIdAsc(appealId).stream()
            .map(AssessmentResultAppealItemMapper::toDomain)
            .toList();
    }

    @Override
    public void deleteByAppealIdIn(Collection<UUID> appealIds) {
        springDataAssessmentResultAppealItemRepository.deleteByAppealIdIn(appealIds);
    }
}
