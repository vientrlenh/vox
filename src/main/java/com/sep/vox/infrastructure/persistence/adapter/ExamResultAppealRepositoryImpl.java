package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.exam.ExamAppealStatus;
import com.sep.vox.domain.model.exam.ExamResultAppeal;
import com.sep.vox.domain.repository.ExamResultAppealRepository;
import com.sep.vox.infrastructure.persistence.mapper.ExamResultAppealMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataExamResultAppealRepository;

@Repository
public class ExamResultAppealRepositoryImpl implements ExamResultAppealRepository {

    private final SpringDataExamResultAppealRepository springDataExamResultAppealRepository;

    public ExamResultAppealRepositoryImpl(
            SpringDataExamResultAppealRepository springDataExamResultAppealRepository) {
        this.springDataExamResultAppealRepository = springDataExamResultAppealRepository;
    }

    @Override
    public Optional<ExamResultAppeal> findById(UUID id) {
        return springDataExamResultAppealRepository.findById(id)
            .map(ExamResultAppealMapper::toDomain);
    }

    @Override
    public ExamResultAppeal save(ExamResultAppeal appeal) {
        var saved = springDataExamResultAppealRepository.save(ExamResultAppealMapper.toJpa(appeal));
        return ExamResultAppealMapper.toDomain(saved);
    }

    @Override
    public boolean existsOpenByCandidateResultId(UUID candidateResultId) {
        return springDataExamResultAppealRepository.existsOpenByCandidateResultId(candidateResultId);
    }

    @Override
    public long countPublishedByCandidateResultId(UUID candidateResultId) {
        return springDataExamResultAppealRepository.countPublishedByCandidateResultId(candidateResultId);
    }

    @Override
    public List<ExamResultAppeal> findByCandidateResultId(UUID candidateResultId) {
        return springDataExamResultAppealRepository.findByCandidateResultId(candidateResultId).stream()
            .map(ExamResultAppealMapper::toDomain)
            .toList();
    }

    @Override
    public void deleteByIdIn(Collection<UUID> ids) {
        springDataExamResultAppealRepository.deleteAllByIdInBatch(ids);
    }

    @Override
    public long countBySchoolIdAndStatusIn(UUID schoolId, Collection<ExamAppealStatus> statuses) {
        var statusNames = statuses.stream().map(Enum::name).toList();
        return springDataExamResultAppealRepository.countBySchoolIdAndStatusIn(schoolId, statusNames);
    }
}
