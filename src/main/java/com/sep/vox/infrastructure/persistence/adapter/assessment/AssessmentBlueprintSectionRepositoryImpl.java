package com.sep.vox.infrastructure.persistence.adapter.assessment;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.assessment.AssessmentBlueprintSection;
import com.sep.vox.domain.repository.assessment.AssessmentBlueprintSectionRepository;
import com.sep.vox.infrastructure.persistence.mapper.assessment.AssessmentBlueprintSectionMapper;
import com.sep.vox.infrastructure.persistence.repository.assessment.SpringDataAssessmentBlueprintSectionRepository;

@Repository
public class AssessmentBlueprintSectionRepositoryImpl implements AssessmentBlueprintSectionRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(AssessmentBlueprintSectionRepositoryImpl.class);

    private final SpringDataAssessmentBlueprintSectionRepository springDataAssessmentBlueprintSectionRepository;

    public AssessmentBlueprintSectionRepositoryImpl(SpringDataAssessmentBlueprintSectionRepository springDataAssessmentBlueprintSectionRepository) {
        this.springDataAssessmentBlueprintSectionRepository = springDataAssessmentBlueprintSectionRepository;
    }

    @Override
    public AssessmentBlueprintSection save(AssessmentBlueprintSection section) {
        var saved = springDataAssessmentBlueprintSectionRepository.save(AssessmentBlueprintSectionMapper.toJpa(section));
        return AssessmentBlueprintSectionMapper.toDomain(saved);
    }

    @Override
    public Optional<AssessmentBlueprintSection> findById(UUID id) {
        return springDataAssessmentBlueprintSectionRepository.findById(id)
            .map(AssessmentBlueprintSectionMapper::toDomain);
    }

    @Override
    public List<AssessmentBlueprintSection> findByBlueprintVersionId(UUID blueprintVersionId) {
        return springDataAssessmentBlueprintSectionRepository.findByBlueprintVersionIdOrderByOrderAsc(blueprintVersionId).stream()
            .map(AssessmentBlueprintSectionMapper::toDomain)
            .toList();
    }

    @Override
    public List<AssessmentBlueprintSection> findByBlueprintVersionIdIn(Collection<UUID> blueprintVersionIds) {
        if (blueprintVersionIds.isEmpty()) {
            return List.of();
        }
        var startedAt = System.nanoTime();
        var result = springDataAssessmentBlueprintSectionRepository.findByBlueprintVersionIdInOrderByOrderAsc(blueprintVersionIds).stream()
            .map(AssessmentBlueprintSectionMapper::toDomain)
            .toList();
        LOGGER.info("[blueprint-perf] repo AssessmentBlueprintSectionRepositoryImpl.findByBlueprintVersionIdIn versionIds={} tookMs={}",
            blueprintVersionIds.size(), (System.nanoTime() - startedAt) / 1_000_000);
        return result;
    }

    @Override
    public void deleteById(UUID id) {
        springDataAssessmentBlueprintSectionRepository.deleteById(id);
    }
}
