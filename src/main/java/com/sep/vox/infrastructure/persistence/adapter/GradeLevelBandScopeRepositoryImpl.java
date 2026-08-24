package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.gradelevel.GradeLevelBandScope;
import com.sep.vox.domain.repository.GradeLevelBandScopeRepository;
import com.sep.vox.infrastructure.persistence.mapper.GradeLevelBandScopeMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataGradeLevelBandScopeRepository;

@Repository
public class GradeLevelBandScopeRepositoryImpl implements GradeLevelBandScopeRepository {

    private final SpringDataGradeLevelBandScopeRepository bandScopeRepository;

    public GradeLevelBandScopeRepositoryImpl(SpringDataGradeLevelBandScopeRepository bandScopeRepository) {
        this.bandScopeRepository = bandScopeRepository;
    }

    @Override
    public Optional<GradeLevelBandScope> findByGradeLevelIdAndFrameworkVersionId(
            UUID gradeLevelId, UUID frameworkVersionId) {
        if (gradeLevelId == null || frameworkVersionId == null) {
            return Optional.empty();
        }
        return bandScopeRepository.findByGradeLevelIdAndFrameworkVersionId(gradeLevelId, frameworkVersionId)
                .map(GradeLevelBandScopeMapper::toDomain);
    }

    @Override
    public List<GradeLevelBandScope> findByGradeLevelIdInAndFrameworkVersionIdIn(
            Collection<UUID> gradeLevelIds, Collection<UUID> frameworkVersionIds) {
        if (gradeLevelIds == null || gradeLevelIds.isEmpty()
                || frameworkVersionIds == null || frameworkVersionIds.isEmpty()) {
            return List.of();
        }
        return bandScopeRepository.findByGradeLevelIdInAndFrameworkVersionIdIn(gradeLevelIds, frameworkVersionIds)
                .stream()
                .map(GradeLevelBandScopeMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByBandId(UUID bandId) {
        return bandId != null && bandScopeRepository.existsByBandId(bandId);
    }

    @Override
    public GradeLevelBandScope save(GradeLevelBandScope bandScope) {
        var saved = bandScopeRepository.save(GradeLevelBandScopeMapper.toEntity(bandScope));
        return GradeLevelBandScopeMapper.toDomain(saved);
    }

    @Override
    public void deleteById(UUID id) {
        bandScopeRepository.deleteById(id);
    }
}
