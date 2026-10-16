package com.sep.vox.infrastructure.persistence.adapter.assessment;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.assessment.AssessmentPaperItem;
import com.sep.vox.domain.repository.assessment.AssessmentPaperItemRepository;
import com.sep.vox.infrastructure.persistence.mapper.assessment.AssessmentPaperItemMapper;
import com.sep.vox.infrastructure.persistence.repository.assessment.SpringDataAssessmentPaperItemRepository;

@Repository
public class AssessmentPaperItemRepositoryImpl implements AssessmentPaperItemRepository {

    private final SpringDataAssessmentPaperItemRepository springDataAssessmentPaperItemRepository;

    public AssessmentPaperItemRepositoryImpl(SpringDataAssessmentPaperItemRepository springDataAssessmentPaperItemRepository) {
        this.springDataAssessmentPaperItemRepository = springDataAssessmentPaperItemRepository;
    }

    @Override
    public AssessmentPaperItem save(AssessmentPaperItem item) {
        var saved = springDataAssessmentPaperItemRepository.save(AssessmentPaperItemMapper.toJpa(item));
        return AssessmentPaperItemMapper.toDomain(saved);
    }

    @Override
    public Optional<AssessmentPaperItem> findById(UUID id) {
        return springDataAssessmentPaperItemRepository.findById(id)
            .map(AssessmentPaperItemMapper::toDomain);
    }

    @Override
    public List<AssessmentPaperItem> findBySectionId(UUID sectionId) {
        return springDataAssessmentPaperItemRepository.findBySectionIdOrderByOrderAsc(sectionId).stream()
            .map(AssessmentPaperItemMapper::toDomain)
            .toList();
    }

    @Override
    public List<AssessmentPaperItem> findBySectionIdIn(Collection<UUID> sectionIds) {
        if (sectionIds.isEmpty()) {
            return List.of();
        }
        return springDataAssessmentPaperItemRepository.findBySectionIdInOrderByOrderAsc(sectionIds).stream()
            .map(AssessmentPaperItemMapper::toDomain)
            .toList();
    }

    @Override
    public List<AssessmentPaperItem> findByPaperId(UUID paperId) {
        return springDataAssessmentPaperItemRepository.findByPaperId(paperId).stream()
            .map(AssessmentPaperItemMapper::toDomain)
            .toList();
    }

    @Override
    public List<AssessmentPaperItem> findByPaperIdIn(Collection<UUID> paperIds) {
        if (paperIds.isEmpty()) {
            return List.of();
        }
        return springDataAssessmentPaperItemRepository.findByPaperIdIn(paperIds).stream()
            .map(AssessmentPaperItemMapper::toDomain)
            .toList();
    }

    @Override
    public boolean existsUnassignedItemByPaperId(UUID paperId) {
        return springDataAssessmentPaperItemRepository.existsUnassignedItemByPaperId(paperId);
    }

    @Override
    public void deleteById(UUID id) {
        springDataAssessmentPaperItemRepository.deleteById(id);
    }

    @Override
    public void deleteByPaperIdIn(java.util.Collection<UUID> paperIds) {
        if (paperIds == null || paperIds.isEmpty()) {
            return;
        }
        springDataAssessmentPaperItemRepository.deleteByPaperIdIn(paperIds);
    }
}
