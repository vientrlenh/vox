package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import com.sep.vox.application.exception.ConflictException;
import com.sep.vox.domain.model.exam.ExamCandidate;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.infrastructure.persistence.mapper.ExamCandidateMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataExamCandidateRepository;

@Repository
public class ExamCandidateRepositoryImpl implements ExamCandidateRepository {

    private final SpringDataExamCandidateRepository springDataExamCandidateRepository;

    public ExamCandidateRepositoryImpl(SpringDataExamCandidateRepository springDataExamCandidateRepository) {
        this.springDataExamCandidateRepository = springDataExamCandidateRepository;
    }

    @Override
    public ExamCandidate save(ExamCandidate candidate) {
        try {
            var saved = springDataExamCandidateRepository.save(ExamCandidateMapper.toJpa(candidate));
            return ExamCandidateMapper.toDomain(saved);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Thí sinh đã tồn tại trong kỳ thi này");
        }
    }

    @Override
    public List<ExamCandidate> saveAll(Collection<ExamCandidate> candidates) {
        try {
            var entities = candidates.stream().map(ExamCandidateMapper::toJpa).toList();
            return springDataExamCandidateRepository.saveAll(entities).stream()
                .map(ExamCandidateMapper::toDomain)
                .toList();
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Thí sinh đã tồn tại trong kỳ thi này");
        }
    }

    @Override
    public List<ExamCandidate> findByExamId(UUID examId) {
        return springDataExamCandidateRepository.findByExamId(examId).stream()
            .map(ExamCandidateMapper::toDomain)
            .toList();
    }

    @Override
    public Optional<ExamCandidate> findById(UUID id) {
        return springDataExamCandidateRepository.findById(id)
            .map(ExamCandidateMapper::toDomain);
    }

    @Override
    public boolean existsByExamIdAndStudentId(UUID examId, UUID studentId) {
        return springDataExamCandidateRepository.existsByExamIdAndStudentId(examId, studentId);
    }

    @Override
    public List<ExamCandidate> findByExamIdAndScheduleIdIsNullOrderByAssignedAtAsc(UUID examId) {
        return springDataExamCandidateRepository.findByExamIdAndScheduleIdIsNullOrderByAssignedAtAsc(examId).stream()
            .map(ExamCandidateMapper::toDomain)
            .toList();
    }

    @Override
    public List<ExamCandidate> findByIdInAndExamId(Collection<UUID> ids, UUID examId) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return springDataExamCandidateRepository.findByIdInAndExamId(ids, examId).stream()
            .map(ExamCandidateMapper::toDomain)
            .toList();
    }

    @Override
    public long countByScheduleId(UUID scheduleId) {
        return springDataExamCandidateRepository.countByScheduleId(scheduleId);
    }

    @Override
    public boolean existsByScheduleIdAndStudentId(UUID scheduleId, UUID studentId) {
        return springDataExamCandidateRepository.existsByScheduleIdAndStudentId(scheduleId, studentId);
    }
}

