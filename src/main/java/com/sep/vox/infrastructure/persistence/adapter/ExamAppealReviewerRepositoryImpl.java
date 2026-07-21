package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.exam.ExamAppealReviewer;
import com.sep.vox.domain.model.exam.ExamAppealReviewerStatus;
import com.sep.vox.domain.repository.ExamAppealReviewerRepository;
import com.sep.vox.infrastructure.persistence.mapper.ExamAppealReviewerMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataExamAppealReviewerRepository;

@Repository
public class ExamAppealReviewerRepositoryImpl implements ExamAppealReviewerRepository {

    private final SpringDataExamAppealReviewerRepository springDataExamAppealReviewerRepository;

    public ExamAppealReviewerRepositoryImpl(
            SpringDataExamAppealReviewerRepository springDataExamAppealReviewerRepository) {
        this.springDataExamAppealReviewerRepository = springDataExamAppealReviewerRepository;
    }

    @Override
    public Optional<ExamAppealReviewer> findById(UUID id) {
        return springDataExamAppealReviewerRepository.findById(id)
            .map(ExamAppealReviewerMapper::toDomain);
    }

    @Override
    public Optional<ExamAppealReviewer> findByAppealIdAndReviewerId(UUID appealId, UUID reviewerId) {
        return springDataExamAppealReviewerRepository.findByAppealIdAndReviewerId(appealId, reviewerId)
            .map(ExamAppealReviewerMapper::toDomain);
    }

    @Override
    public List<ExamAppealReviewer> findByAppealId(UUID appealId) {
        return springDataExamAppealReviewerRepository.findByAppealIdOrderByAssignedAtAsc(appealId).stream()
            .map(ExamAppealReviewerMapper::toDomain)
            .toList();
    }

    @Override
    public List<ExamAppealReviewer> saveAll(List<ExamAppealReviewer> reviewers) {
        var entities = reviewers.stream().map(ExamAppealReviewerMapper::toJpa).toList();
        return springDataExamAppealReviewerRepository.saveAll(entities).stream()
            .map(ExamAppealReviewerMapper::toDomain)
            .toList();
    }

    @Override
    public ExamAppealReviewer save(ExamAppealReviewer reviewer) {
        var saved = springDataExamAppealReviewerRepository.save(ExamAppealReviewerMapper.toJpa(reviewer));
        return ExamAppealReviewerMapper.toDomain(saved);
    }

    @Override
    public void deleteById(UUID id) {
        springDataExamAppealReviewerRepository.deleteById(id);
    }

    @Override
    public void deleteByAppealIdIn(Collection<UUID> appealIds) {
        springDataExamAppealReviewerRepository.deleteByAppealIdIn(appealIds);
    }

    @Override
    public long countAssignedByReviewerId(UUID reviewerId) {
        return springDataExamAppealReviewerRepository.countByReviewerIdAndStatus(
            reviewerId, ExamAppealReviewerStatus.ASSIGNED.name());
    }
}
