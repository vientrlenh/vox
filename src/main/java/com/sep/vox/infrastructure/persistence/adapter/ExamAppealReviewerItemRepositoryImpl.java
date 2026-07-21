package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.exam.ExamAppealReviewerItem;
import com.sep.vox.domain.repository.ExamAppealReviewerItemRepository;
import com.sep.vox.infrastructure.persistence.mapper.ExamAppealReviewerItemMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataExamAppealReviewerItemRepository;

@Repository
public class ExamAppealReviewerItemRepositoryImpl implements ExamAppealReviewerItemRepository {

    private final SpringDataExamAppealReviewerItemRepository springDataExamAppealReviewerItemRepository;

    public ExamAppealReviewerItemRepositoryImpl(
            SpringDataExamAppealReviewerItemRepository springDataExamAppealReviewerItemRepository) {
        this.springDataExamAppealReviewerItemRepository = springDataExamAppealReviewerItemRepository;
    }

    @Override
    public List<ExamAppealReviewerItem> saveAll(List<ExamAppealReviewerItem> items) {
        var entities = items.stream().map(ExamAppealReviewerItemMapper::toJpa).toList();
        return springDataExamAppealReviewerItemRepository.saveAll(entities).stream()
            .map(ExamAppealReviewerItemMapper::toDomain)
            .toList();
    }

    @Override
    public List<ExamAppealReviewerItem> findByAppealReviewerId(UUID appealReviewerId) {
        return springDataExamAppealReviewerItemRepository
            .findByAppealReviewerIdOrderByIdAsc(appealReviewerId).stream()
            .map(ExamAppealReviewerItemMapper::toDomain)
            .toList();
    }

    @Override
    public void deleteByAppealIdIn(Collection<UUID> appealIds) {
        springDataExamAppealReviewerItemRepository.deleteByAppealIdIn(appealIds);
    }
}
