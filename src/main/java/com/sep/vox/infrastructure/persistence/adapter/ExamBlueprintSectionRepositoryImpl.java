package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.exam.ExamBlueprintSection;
import com.sep.vox.domain.repository.ExamBlueprintSectionRepository;
import com.sep.vox.infrastructure.persistence.mapper.ExamBlueprintSectionMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataExamBlueprintSectionRepository;

@Repository
public class ExamBlueprintSectionRepositoryImpl implements ExamBlueprintSectionRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExamBlueprintSectionRepositoryImpl.class);

    private final SpringDataExamBlueprintSectionRepository springDataExamBlueprintSectionRepository;

    public ExamBlueprintSectionRepositoryImpl(SpringDataExamBlueprintSectionRepository springDataExamBlueprintSectionRepository) {
        this.springDataExamBlueprintSectionRepository = springDataExamBlueprintSectionRepository;
    }

    @Override
    public ExamBlueprintSection save(ExamBlueprintSection section) {
        var saved = springDataExamBlueprintSectionRepository.save(ExamBlueprintSectionMapper.toJpa(section));
        return ExamBlueprintSectionMapper.toDomain(saved);
    }

    @Override
    public Optional<ExamBlueprintSection> findById(UUID id) {
        return springDataExamBlueprintSectionRepository.findById(id)
            .map(ExamBlueprintSectionMapper::toDomain);
    }

    @Override
    public List<ExamBlueprintSection> findByBlueprintVersionId(UUID blueprintVersionId) {
        return springDataExamBlueprintSectionRepository.findByBlueprintVersionIdOrderByOrderAsc(blueprintVersionId).stream()
            .map(ExamBlueprintSectionMapper::toDomain)
            .toList();
    }

    @Override
    public List<ExamBlueprintSection> findByBlueprintVersionIdIn(Collection<UUID> blueprintVersionIds) {
        if (blueprintVersionIds.isEmpty()) {
            return List.of();
        }
        var startedAt = System.nanoTime();
        var result = springDataExamBlueprintSectionRepository.findByBlueprintVersionIdInOrderByOrderAsc(blueprintVersionIds).stream()
            .map(ExamBlueprintSectionMapper::toDomain)
            .toList();
        LOGGER.info("[blueprint-perf] repo ExamBlueprintSectionRepositoryImpl.findByBlueprintVersionIdIn versionIds={} tookMs={}",
            blueprintVersionIds.size(), (System.nanoTime() - startedAt) / 1_000_000);
        return result;
    }

    @Override
    public void deleteById(UUID id) {
        springDataExamBlueprintSectionRepository.deleteById(id);
    }
}
