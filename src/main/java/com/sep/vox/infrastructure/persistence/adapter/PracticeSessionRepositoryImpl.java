package com.sep.vox.infrastructure.persistence.adapter;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.personalization.PracticeSession;
import com.sep.vox.domain.repository.personalization.PracticeSessionRepository;
import com.sep.vox.infrastructure.persistence.mapper.personalization.PracticeSessionMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataPracticeSessionRepository;

@Repository
public class PracticeSessionRepositoryImpl implements PracticeSessionRepository {

    private final SpringDataPracticeSessionRepository repository;

    public PracticeSessionRepositoryImpl(SpringDataPracticeSessionRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<PracticeSession> findById(UUID id) {
        return repository.findById(id).map(PracticeSessionMapper::toDomain);
    }

    @Override
    public Optional<PracticeSession> findByIdForUpdate(UUID id) {
        return repository.findByIdForUpdate(id).map(PracticeSessionMapper::toDomain);
    }

    @Override
    public Optional<PracticeSession> findByIdAndStudentId(UUID id, UUID studentId) {
        return repository.findByIdAndStudentId(id, studentId).map(PracticeSessionMapper::toDomain);
    }

    @Override
    public boolean existsByIdAndStudentIdAndStatus(UUID id, UUID studentId, String status) {
        return repository.existsByIdAndStudentIdAndStatus(id, studentId, status);
    }

    @Override
    public PracticeSession save(PracticeSession session) {
        return PracticeSessionMapper.toDomain(
            repository.save(PracticeSessionMapper.toJpa(session))
        );
    }

    @Override
    public List<PracticeSession> findStaleInProgress(Instant staleBefore) {
        return repository.findStaleInProgressForUpdate(staleBefore).stream()
            .map(PracticeSessionMapper::toDomain)
            .toList();
    }

    @Override
    public void refreshOverallScore(UUID sessionId) {
        repository.refreshOverallScore(sessionId);
    }
}
