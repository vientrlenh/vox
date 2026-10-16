package com.sep.vox.infrastructure.persistence.adapter.assessment;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.assessment.AssessmentPaper;
import com.sep.vox.domain.model.assessment.AssessmentPaperStatus;
import com.sep.vox.domain.repository.assessment.AssessmentPaperRepository;
import com.sep.vox.infrastructure.persistence.mapper.assessment.AssessmentPaperMapper;
import com.sep.vox.infrastructure.persistence.repository.assessment.SpringDataAssessmentPaperRepository;

@Repository
public class AssessmentPaperRepositoryImpl implements AssessmentPaperRepository {

    private final SpringDataAssessmentPaperRepository springDataAssessmentPaperRepository;

    public AssessmentPaperRepositoryImpl(SpringDataAssessmentPaperRepository springDataAssessmentPaperRepository) {
        this.springDataAssessmentPaperRepository = springDataAssessmentPaperRepository;
    }

    @Override
    public AssessmentPaper save(AssessmentPaper paper) {
        var saved = springDataAssessmentPaperRepository.save(AssessmentPaperMapper.toJpa(paper));
        return AssessmentPaperMapper.toDomain(saved);
    }

    @Override
    public List<AssessmentPaper> saveAll(Collection<AssessmentPaper> papers) {
        if (papers == null || papers.isEmpty()) {
            return List.of();
        }
        var entities = papers.stream().map(AssessmentPaperMapper::toJpa).toList();
        return springDataAssessmentPaperRepository.saveAll(entities).stream()
            .map(AssessmentPaperMapper::toDomain)
            .toList();
    }

    @Override
    public Optional<AssessmentPaper> findById(UUID id) {
        return springDataAssessmentPaperRepository.findById(id)
            .map(AssessmentPaperMapper::toDomain);
    }

    @Override
    public List<AssessmentPaper> findByIdIn(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return springDataAssessmentPaperRepository.findAllById(ids).stream()
            .map(AssessmentPaperMapper::toDomain)
            .toList();
    }

    @Override
    public int nextVariant(UUID assessmentId) {
        return springDataAssessmentPaperRepository.nextVariant(assessmentId);
    }

    @Override
    public void deleteById(UUID id) {
        springDataAssessmentPaperRepository.deleteById(id);
    }

    @Override
    public void deleteByAssessmentId(UUID assessmentId) {
        springDataAssessmentPaperRepository.deleteByAssessmentId(assessmentId);
    }

    @Override
    public List<AssessmentPaper> findByAssessmentId(UUID assessmentId) {
        return springDataAssessmentPaperRepository.findByAssessmentIdOrderByVariantAsc(assessmentId).stream()
            .map(AssessmentPaperMapper::toDomain)
            .toList();
    }

    @Override
    public List<AssessmentPaper> findByAssessmentIdIn(Collection<UUID> assessmentIds) {
        if (assessmentIds.isEmpty()) {
            return List.of();
        }
        return springDataAssessmentPaperRepository.findByAssessmentIdInOrderByVariantAsc(assessmentIds).stream()
            .map(AssessmentPaperMapper::toDomain)
            .toList();
    }

    @Override
    public List<AssessmentPaper> findByAssessmentIdAndStatus(UUID assessmentId, AssessmentPaperStatus status) {
        return springDataAssessmentPaperRepository.findByAssessmentIdAndStatusOrderByVariantAsc(assessmentId, status.name()).stream()
            .map(AssessmentPaperMapper::toDomain)
            .toList();
    }

    @Override
    public boolean existsByAssessmentId(UUID assessmentId) {
        return springDataAssessmentPaperRepository.existsByAssessmentId(assessmentId);
    }
}
