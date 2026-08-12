package com.sep.vox.infrastructure.persistence.adapter;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.model.personalization.InterestDimension;
import com.sep.vox.domain.repository.InterestDimensionRepository;
import com.sep.vox.infrastructure.persistence.entity.InterestDimensionJpaEntity;
import com.sep.vox.infrastructure.persistence.repository.SpringDataInterestDimensionRepository;

@Repository
public class InterestDimensionRepositoryImpl implements InterestDimensionRepository {

    private final SpringDataInterestDimensionRepository repository;

    public InterestDimensionRepositoryImpl(SpringDataInterestDimensionRepository repository) {
        this.repository = repository;
    }

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
        var now = Instant.now();
        var saved = repository.save(new InterestDimensionJpaEntity(
            dimension.getCode(),
            dimension.getLabel(),
            dimension.getDescription(),
            dimension.isActive(),
            dimension.isQuizEligible(),
            dimension.getDisplayOrder(),
            dimension.getCreatedAt() == null ? now : dimension.getCreatedAt(),
            now
        ));
        return toDomain(saved);
    }

    @Override
    @Transactional
    public void deactivate(String code) {
        repository.deactivate(code);
    }

    private static InterestDimension toDomain(InterestDimensionJpaEntity entity) {
        return new InterestDimension(
            entity.getCode(),
            entity.getLabel(),
            entity.getDescription(),
            entity.isActive(),
            entity.isQuizEligible(),
            entity.getDisplayOrder(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
