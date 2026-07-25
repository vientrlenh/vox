package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.exam.ExamResultAppealItem;
import com.sep.vox.domain.repository.ExamResultAppealItemRepository;
import com.sep.vox.infrastructure.persistence.mapper.ExamResultAppealItemMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataExamResultAppealItemRepository;

@Repository
public class ExamResultAppealItemRepositoryImpl implements ExamResultAppealItemRepository {

    private final SpringDataExamResultAppealItemRepository springDataExamResultAppealItemRepository;

    public ExamResultAppealItemRepositoryImpl(
            SpringDataExamResultAppealItemRepository springDataExamResultAppealItemRepository) {
        this.springDataExamResultAppealItemRepository = springDataExamResultAppealItemRepository;
    }

    @Override
    public List<ExamResultAppealItem> saveAll(List<ExamResultAppealItem> items) {
        var entities = items.stream().map(ExamResultAppealItemMapper::toJpa).toList();
        return springDataExamResultAppealItemRepository.saveAll(entities).stream()
            .map(ExamResultAppealItemMapper::toDomain)
            .toList();
    }

    @Override
    public List<ExamResultAppealItem> findByAppealId(UUID appealId) {
        return springDataExamResultAppealItemRepository.findByAppealIdOrderByIdAsc(appealId).stream()
            .map(ExamResultAppealItemMapper::toDomain)
            .toList();
    }

    @Override
    public void deleteByAppealIdIn(Collection<UUID> appealIds) {
        springDataExamResultAppealItemRepository.deleteByAppealIdIn(appealIds);
    }
}
