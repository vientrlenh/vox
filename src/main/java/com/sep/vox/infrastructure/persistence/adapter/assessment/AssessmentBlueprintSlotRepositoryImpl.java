package com.sep.vox.infrastructure.persistence.adapter.assessment;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.assessment.AssessmentBlueprintSlot;
import com.sep.vox.domain.repository.assessment.AssessmentBlueprintSlotRepository;
import com.sep.vox.infrastructure.persistence.mapper.assessment.AssessmentBlueprintSlotMapper;
import com.sep.vox.infrastructure.persistence.repository.assessment.SpringDataAssessmentBlueprintSlotRepository;

@Repository
public class AssessmentBlueprintSlotRepositoryImpl implements AssessmentBlueprintSlotRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(AssessmentBlueprintSlotRepositoryImpl.class);

    private final SpringDataAssessmentBlueprintSlotRepository springDataAssessmentBlueprintSlotRepository;

    public AssessmentBlueprintSlotRepositoryImpl(SpringDataAssessmentBlueprintSlotRepository springDataAssessmentBlueprintSlotRepository) {
        this.springDataAssessmentBlueprintSlotRepository = springDataAssessmentBlueprintSlotRepository;
    }

    @Override
    public AssessmentBlueprintSlot save(AssessmentBlueprintSlot slot) {
        var saved = springDataAssessmentBlueprintSlotRepository.save(AssessmentBlueprintSlotMapper.toJpa(slot));
        return AssessmentBlueprintSlotMapper.toDomain(saved);
    }

    @Override
    public Optional<AssessmentBlueprintSlot> findById(UUID id) {
        return springDataAssessmentBlueprintSlotRepository.findById(id)
            .map(AssessmentBlueprintSlotMapper::toDomain);
    }

    @Override
    public List<AssessmentBlueprintSlot> findByIdIn(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return springDataAssessmentBlueprintSlotRepository.findAllById(ids).stream()
            .map(AssessmentBlueprintSlotMapper::toDomain)
            .toList();
    }

    @Override
    public List<AssessmentBlueprintSlot> findByBlueprintVersionId(UUID blueprintVersionId) {
        return springDataAssessmentBlueprintSlotRepository.findByBlueprintVersionIdOrderByOrderAsc(blueprintVersionId).stream()
            .map(AssessmentBlueprintSlotMapper::toDomain)
            .toList();
    }

    @Override
    public List<AssessmentBlueprintSlot> findByBlueprintVersionIdIn(Collection<UUID> blueprintVersionIds) {
        if (blueprintVersionIds.isEmpty()) {
            return List.of();
        }
        var startedAt = System.nanoTime();
        var result = springDataAssessmentBlueprintSlotRepository.findByBlueprintVersionIdInOrderByOrderAsc(blueprintVersionIds).stream()
            .map(AssessmentBlueprintSlotMapper::toDomain)
            .toList();
        LOGGER.info("[blueprint-perf] repo AssessmentBlueprintSlotRepositoryImpl.findByBlueprintVersionIdIn versionIds={} tookMs={}",
            blueprintVersionIds.size(), (System.nanoTime() - startedAt) / 1_000_000);
        return result;
    }

    @Override
    public List<AssessmentBlueprintSlot> findBySectionId(UUID sectionId) {
        return springDataAssessmentBlueprintSlotRepository.findBySectionIdOrderByOrderAsc(sectionId).stream()
            .map(AssessmentBlueprintSlotMapper::toDomain)
            .toList();
    }

    @Override
    public List<AssessmentBlueprintSlot> findBySectionIdIn(Collection<UUID> sectionIds) {
        if (sectionIds.isEmpty()) {
            return List.of();
        }
        var startedAt = System.nanoTime();
        var result = springDataAssessmentBlueprintSlotRepository.findBySectionIdInOrderByOrderAsc(sectionIds).stream()
            .map(AssessmentBlueprintSlotMapper::toDomain)
            .toList();
        LOGGER.info("[blueprint-perf] repo AssessmentBlueprintSlotRepositoryImpl.findBySectionIdIn sectionIds={} tookMs={}",
            sectionIds.size(), (System.nanoTime() - startedAt) / 1_000_000);
        return result;
    }

    @Override
    public void deleteById(UUID id) {
        springDataAssessmentBlueprintSlotRepository.deleteById(id);
    }
}
