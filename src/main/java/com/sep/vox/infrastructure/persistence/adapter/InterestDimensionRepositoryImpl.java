package com.sep.vox.infrastructure.persistence.adapter;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.model.personalization.InterestDimension;
import com.sep.vox.domain.repository.InterestDimensionRepository;
import com.sep.vox.infrastructure.persistence.entity.personalization.InterestDimensionJpaEntity;
import com.sep.vox.infrastructure.persistence.repository.SpringDataInterestDimensionRepository;

@Repository
@RequiredArgsConstructor
public class InterestDimensionRepositoryImpl implements InterestDimensionRepository {

    private final SpringDataInterestDimensionRepository repository;

    @Override
    public List<InterestDimension> findAll() {
        return repository.findAllByOrderByDisplayOrderAscCodeAsc().stream()
            .map(InterestDimensionRepositoryImpl::toDomain)
            .toList();
    }

    @Override
    public List<InterestDimension> findActive() {
        return repository.findByActiveTrueOrderByDisplayOrderAscCodeAsc().stream()
            .map(InterestDimensionRepositoryImpl::toDomain)
            .toList();
    }

    @Override
    public List<InterestDimension> findQuizEligible() {
        return repository
            .findByActiveTrueAndQuizEligibleTrueOrderByDisplayOrderAscCodeAsc().stream()
            .map(InterestDimensionRepositoryImpl::toDomain)
            .toList();
    }

    @Override
    public Optional<InterestDimension> findByCode(String code) {
        return repository.findById(code).map(InterestDimensionRepositoryImpl::toDomain);
    }

    @Override
    public InterestDimension save(InterestDimension dimension) {
        Instant now = Instant.now();
        InterestDimensionJpaEntity saved = repository.save(InterestDimensionJpaEntity.builder()
                .code(dimension.getCode())
                .label(dimension.getLabel())
                .description(dimension.getDescription())
                .active(dimension.isActive())
                .quizEligible(dimension.isQuizEligible())
                .displayOrder(dimension.getDisplayOrder())
                .createdAt(dimension.getCreatedAt() == null ? now : dimension.getCreatedAt())
                .updatedAt(now)
                .build());
        return toDomain(saved);
    }

    @Override
    @Transactional
    public void deactivate(String code) {
        repository.deactivate(code);
    }

    private static InterestDimension toDomain(InterestDimensionJpaEntity entity) {
        return InterestDimension.builder()
                .code(entity.getCode())
                .label(entity.getLabel())
                .description(entity.getDescription())
                .active(entity.isActive())
                .quizEligible(entity.isQuizEligible())
                .displayOrder(entity.getDisplayOrder())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
