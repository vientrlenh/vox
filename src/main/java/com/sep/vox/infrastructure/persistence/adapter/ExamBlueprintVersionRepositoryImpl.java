package com.sep.vox.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.exam.ExamBlueprintVersion;
import com.sep.vox.domain.model.exam.ExamBlueprintVersionStatus;
import com.sep.vox.domain.repository.ExamBlueprintVersionRepository;
import com.sep.vox.infrastructure.persistence.mapper.ExamBlueprintVersionMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataExamBlueprintVersionRepository;

@Repository
public class ExamBlueprintVersionRepositoryImpl implements ExamBlueprintVersionRepository {

    private final SpringDataExamBlueprintVersionRepository springDataExamBlueprintVersionRepository;

    public ExamBlueprintVersionRepositoryImpl(SpringDataExamBlueprintVersionRepository springDataExamBlueprintVersionRepository) {
        this.springDataExamBlueprintVersionRepository = springDataExamBlueprintVersionRepository;
    }

    @Override
    public ExamBlueprintVersion save(ExamBlueprintVersion version) {
        var saved = springDataExamBlueprintVersionRepository.save(ExamBlueprintVersionMapper.toJpa(version));
        return ExamBlueprintVersionMapper.toDomain(saved);
    }

    @Override
    public Optional<ExamBlueprintVersion> findById(UUID id) {
        return springDataExamBlueprintVersionRepository.findById(id)
            .map(ExamBlueprintVersionMapper::toDomain);
    }

    @Override
    public List<ExamBlueprintVersion> findByBlueprintId(UUID blueprintId) {
        return springDataExamBlueprintVersionRepository.findByBlueprintIdOrderByVersionDesc(blueprintId).stream()
            .map(ExamBlueprintVersionMapper::toDomain)
            .toList();
    }

    @Override
    public List<ExamBlueprintVersion> findByBlueprintIdAndStatus(UUID blueprintId, ExamBlueprintVersionStatus status) {
        return springDataExamBlueprintVersionRepository.findByBlueprintIdAndStatusOrderByVersionDesc(
            blueprintId,
            status.name()
        ).stream().map(ExamBlueprintVersionMapper::toDomain).toList();
    }

    @Override
    public int nextVersionNumber(UUID blueprintId) {
        return springDataExamBlueprintVersionRepository.nextVersionNumber(blueprintId);
    }

    @Override
    public boolean existsUsedByVersion(UUID versionId) {
        return springDataExamBlueprintVersionRepository.existsUsedByVersion(versionId);
    }

    @Override
    public void deleteById(UUID id) {
        springDataExamBlueprintVersionRepository.deleteById(id);
    }
}
