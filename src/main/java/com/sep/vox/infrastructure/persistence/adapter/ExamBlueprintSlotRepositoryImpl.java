package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.exam.ExamBlueprintSlot;
import com.sep.vox.domain.repository.ExamBlueprintSlotRepository;
import com.sep.vox.infrastructure.persistence.mapper.ExamBlueprintSlotMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataExamBlueprintSlotRepository;

@Repository
public class ExamBlueprintSlotRepositoryImpl implements ExamBlueprintSlotRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExamBlueprintSlotRepositoryImpl.class);

    private final SpringDataExamBlueprintSlotRepository springDataExamBlueprintSlotRepository;

    public ExamBlueprintSlotRepositoryImpl(SpringDataExamBlueprintSlotRepository springDataExamBlueprintSlotRepository) {
        this.springDataExamBlueprintSlotRepository = springDataExamBlueprintSlotRepository;
    }

    @Override
    public ExamBlueprintSlot save(ExamBlueprintSlot slot) {
        var saved = springDataExamBlueprintSlotRepository.save(ExamBlueprintSlotMapper.toJpa(slot));
        return ExamBlueprintSlotMapper.toDomain(saved);
    }

    @Override
    public Optional<ExamBlueprintSlot> findById(UUID id) {
        return springDataExamBlueprintSlotRepository.findById(id)
            .map(ExamBlueprintSlotMapper::toDomain);
    }

    @Override
    public List<ExamBlueprintSlot> findByBlueprintVersionId(UUID blueprintVersionId) {
        return springDataExamBlueprintSlotRepository.findByBlueprintVersionIdOrderByOrderAsc(blueprintVersionId).stream()
            .map(ExamBlueprintSlotMapper::toDomain)
            .toList();
    }

    @Override
    public List<ExamBlueprintSlot> findByBlueprintVersionIdIn(Collection<UUID> blueprintVersionIds) {
        if (blueprintVersionIds.isEmpty()) {
            return List.of();
        }
        var startedAt = System.nanoTime();
        var result = springDataExamBlueprintSlotRepository.findByBlueprintVersionIdInOrderByOrderAsc(blueprintVersionIds).stream()
            .map(ExamBlueprintSlotMapper::toDomain)
            .toList();
        LOGGER.info("[blueprint-perf] repo ExamBlueprintSlotRepositoryImpl.findByBlueprintVersionIdIn versionIds={} tookMs={}",
            blueprintVersionIds.size(), (System.nanoTime() - startedAt) / 1_000_000);
        return result;
    }

    @Override
    public List<ExamBlueprintSlot> findBySectionId(UUID sectionId) {
        return springDataExamBlueprintSlotRepository.findBySectionIdOrderByOrderAsc(sectionId).stream()
            .map(ExamBlueprintSlotMapper::toDomain)
            .toList();
    }

    @Override
    public List<ExamBlueprintSlot> findBySectionIdIn(Collection<UUID> sectionIds) {
        if (sectionIds.isEmpty()) {
            return List.of();
        }
        var startedAt = System.nanoTime();
        var result = springDataExamBlueprintSlotRepository.findBySectionIdInOrderByOrderAsc(sectionIds).stream()
            .map(ExamBlueprintSlotMapper::toDomain)
            .toList();
        LOGGER.info("[blueprint-perf] repo ExamBlueprintSlotRepositoryImpl.findBySectionIdIn sectionIds={} tookMs={}",
            sectionIds.size(), (System.nanoTime() - startedAt) / 1_000_000);
        return result;
    }

    @Override
    public void deleteById(UUID id) {
        springDataExamBlueprintSlotRepository.deleteById(id);
    }
}
