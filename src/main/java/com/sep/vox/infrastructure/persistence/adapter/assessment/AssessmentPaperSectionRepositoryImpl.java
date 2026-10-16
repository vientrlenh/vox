package com.sep.vox.infrastructure.persistence.adapter.assessment;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.assessment.AssessmentPaperSection;
import com.sep.vox.domain.repository.assessment.AssessmentPaperSectionRepository;
import com.sep.vox.infrastructure.persistence.mapper.assessment.AssessmentPaperSectionMapper;
import com.sep.vox.infrastructure.persistence.repository.assessment.SpringDataAssessmentPaperSectionRepository;

@Repository
public class AssessmentPaperSectionRepositoryImpl implements AssessmentPaperSectionRepository {

    private final SpringDataAssessmentPaperSectionRepository springDataAssessmentPaperSectionRepository;

    public AssessmentPaperSectionRepositoryImpl(SpringDataAssessmentPaperSectionRepository springDataAssessmentPaperSectionRepository) {
        this.springDataAssessmentPaperSectionRepository = springDataAssessmentPaperSectionRepository;
    }

    @Override
    public AssessmentPaperSection save(AssessmentPaperSection section) {
        var saved = springDataAssessmentPaperSectionRepository.save(AssessmentPaperSectionMapper.toJpa(section));
        return AssessmentPaperSectionMapper.toDomain(saved);
    }

    @Override
    public Optional<AssessmentPaperSection> findById(UUID id) {
        return springDataAssessmentPaperSectionRepository.findById(id).map(AssessmentPaperSectionMapper::toDomain);
    }

    @Override
    public List<AssessmentPaperSection> findByPaperId(UUID paperId) {
        return springDataAssessmentPaperSectionRepository.findByPaperIdOrderByOrderAsc(paperId).stream()
            .map(AssessmentPaperSectionMapper::toDomain)
            .toList();
    }

    @Override
    public List<AssessmentPaperSection> findByPaperIdIn(Collection<UUID> paperIds) {
        if (paperIds.isEmpty()) {
            return List.of();
        }
        return springDataAssessmentPaperSectionRepository.findByPaperIdInOrderByOrderAsc(paperIds).stream()
            .map(AssessmentPaperSectionMapper::toDomain)
            .toList();
    }

    @Override
    public void deleteById(UUID id) {
        springDataAssessmentPaperSectionRepository.deleteById(id);
    }

    @Override
    public void deleteByPaperIdIn(java.util.Collection<UUID> paperIds) {
        if (paperIds == null || paperIds.isEmpty()) {
            return;
        }
        springDataAssessmentPaperSectionRepository.deleteByPaperIdIn(paperIds);
    }
}
