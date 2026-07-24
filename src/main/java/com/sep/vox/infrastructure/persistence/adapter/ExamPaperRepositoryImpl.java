package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.exam.ExamPaper;
import com.sep.vox.domain.model.exam.ExamPaperStatus;
import com.sep.vox.domain.repository.ExamPaperRepository;
import com.sep.vox.infrastructure.persistence.mapper.ExamPaperMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataExamPaperRepository;

@Repository
public class ExamPaperRepositoryImpl implements ExamPaperRepository {

    private final SpringDataExamPaperRepository springDataExamPaperRepository;

    public ExamPaperRepositoryImpl(SpringDataExamPaperRepository springDataExamPaperRepository) {
        this.springDataExamPaperRepository = springDataExamPaperRepository;
    }

    @Override
    public ExamPaper save(ExamPaper paper) {
        var saved = springDataExamPaperRepository.save(ExamPaperMapper.toJpa(paper));
        return ExamPaperMapper.toDomain(saved);
    }

    @Override
    public Optional<ExamPaper> findById(UUID id) {
        return springDataExamPaperRepository.findById(id)
            .map(ExamPaperMapper::toDomain);
    }

    @Override
    public List<ExamPaper> findByIdIn(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return springDataExamPaperRepository.findAllById(ids).stream()
            .map(ExamPaperMapper::toDomain)
            .toList();
    }

    @Override
    public int nextVariant(UUID examId) {
        return springDataExamPaperRepository.nextVariant(examId);
    }

    @Override
    public void deleteById(UUID id) {
        springDataExamPaperRepository.deleteById(id);
    }

    @Override
    public List<ExamPaper> findByExamId(UUID examId) {
        return springDataExamPaperRepository.findByExamIdOrderByVariantAsc(examId).stream()
            .map(ExamPaperMapper::toDomain)
            .toList();
    }

    @Override
    public List<ExamPaper> findByExamIdIn(Collection<UUID> examIds) {
        if (examIds.isEmpty()) {
            return List.of();
        }
        return springDataExamPaperRepository.findByExamIdInOrderByVariantAsc(examIds).stream()
            .map(ExamPaperMapper::toDomain)
            .toList();
    }

    @Override
    public List<ExamPaper> findByExamIdAndStatus(UUID examId, ExamPaperStatus status) {
        return springDataExamPaperRepository.findByExamIdAndStatusOrderByVariantAsc(examId, status.name()).stream()
            .map(ExamPaperMapper::toDomain)
            .toList();
    }

    @Override
    public boolean existsByExamId(UUID examId) {
        return springDataExamPaperRepository.existsByExamId(examId);
    }
}
