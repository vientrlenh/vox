package com.sep.vox.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.exam.ExamPaperItem;
import com.sep.vox.domain.repository.ExamPaperItemRepository;
import com.sep.vox.infrastructure.persistence.mapper.ExamPaperItemMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataExamPaperItemRepository;

@Repository
public class ExamPaperItemRepositoryImpl implements ExamPaperItemRepository {

    private final SpringDataExamPaperItemRepository springDataExamPaperItemRepository;

    public ExamPaperItemRepositoryImpl(SpringDataExamPaperItemRepository springDataExamPaperItemRepository) {
        this.springDataExamPaperItemRepository = springDataExamPaperItemRepository;
    }

    @Override
    public ExamPaperItem save(ExamPaperItem item) {
        var saved = springDataExamPaperItemRepository.save(ExamPaperItemMapper.toJpa(item));
        return ExamPaperItemMapper.toDomain(saved);
    }

    @Override
    public Optional<ExamPaperItem> findById(UUID id) {
        return springDataExamPaperItemRepository.findById(id)
            .map(ExamPaperItemMapper::toDomain);
    }

    @Override
    public List<ExamPaperItem> findBySectionId(UUID sectionId) {
        return springDataExamPaperItemRepository.findBySectionIdOrderByOrderAsc(sectionId).stream()
            .map(ExamPaperItemMapper::toDomain)
            .toList();
    }

    @Override
    public List<ExamPaperItem> findByPaperId(UUID paperId) {
        return springDataExamPaperItemRepository.findByPaperId(paperId).stream()
            .map(ExamPaperItemMapper::toDomain)
            .toList();
    }

    @Override
    public boolean existsUnassignedItemByPaperId(UUID paperId) {
        return springDataExamPaperItemRepository.existsUnassignedItemByPaperId(paperId);
    }

    @Override
    public void deleteById(UUID id) {
        springDataExamPaperItemRepository.deleteById(id);
    }
}
