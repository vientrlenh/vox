package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.exam.ExamGradingAssignment;
import com.sep.vox.domain.repository.ExamGradingAssignmentRepository;
import com.sep.vox.infrastructure.persistence.mapper.ExamGradingAssignmentMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataExamGradingAssignmentRepository;

@Repository
public class ExamGradingAssignmentRepositoryImpl implements ExamGradingAssignmentRepository {

    private final SpringDataExamGradingAssignmentRepository springDataExamGradingAssignmentRepository;

    public ExamGradingAssignmentRepositoryImpl(
            SpringDataExamGradingAssignmentRepository springDataExamGradingAssignmentRepository) {
        this.springDataExamGradingAssignmentRepository = springDataExamGradingAssignmentRepository;
    }

    @Override
    public Optional<ExamGradingAssignment> findById(UUID id) {
        return springDataExamGradingAssignmentRepository.findById(id)
            .map(ExamGradingAssignmentMapper::toDomain);
    }

    @Override
    public Optional<ExamGradingAssignment> findByCandidateResultId(UUID candidateResultId) {
        return springDataExamGradingAssignmentRepository.findByCandidateResultId(candidateResultId)
            .map(ExamGradingAssignmentMapper::toDomain);
    }

    @Override
    public boolean existsByCandidateResultId(UUID candidateResultId) {
        return springDataExamGradingAssignmentRepository.existsByCandidateResultId(candidateResultId);
    }

    @Override
    public List<ExamGradingAssignment> findByCandidateResultIdIn(Collection<UUID> candidateResultIds) {
        if (candidateResultIds.isEmpty()) {
            return List.of();
        }
        return springDataExamGradingAssignmentRepository.findByCandidateResultIdIn(candidateResultIds).stream()
            .map(ExamGradingAssignmentMapper::toDomain)
            .toList();
    }

    @Override
    public ExamGradingAssignment save(ExamGradingAssignment assignment) {
        var saved = springDataExamGradingAssignmentRepository.save(ExamGradingAssignmentMapper.toJpa(assignment));
        return ExamGradingAssignmentMapper.toDomain(saved);
    }

    @Override
    public List<ExamGradingAssignment> saveAll(List<ExamGradingAssignment> assignments) {
        var entities = assignments.stream().map(ExamGradingAssignmentMapper::toJpa).toList();
        return springDataExamGradingAssignmentRepository.saveAll(entities).stream()
            .map(ExamGradingAssignmentMapper::toDomain)
            .toList();
    }

    @Override
    public void deleteById(UUID id) {
        springDataExamGradingAssignmentRepository.deleteById(id);
    }
}
