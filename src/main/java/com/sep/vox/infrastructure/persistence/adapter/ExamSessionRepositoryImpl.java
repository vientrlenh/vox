package com.sep.vox.infrastructure.persistence.adapter;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.exam.ExamSession;
import com.sep.vox.domain.model.exam.ExamSessionStatus;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.repository.ExamSessionRepository;
import com.sep.vox.infrastructure.persistence.mapper.ExamSessionMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataExamSessionRepository;

@Repository
public class ExamSessionRepositoryImpl implements ExamSessionRepository {

    private final SpringDataExamSessionRepository springDataExamSessionRepository;

    public ExamSessionRepositoryImpl(SpringDataExamSessionRepository springDataExamSessionRepository) {
        this.springDataExamSessionRepository = springDataExamSessionRepository;
    }

    @Override
    public Optional<ExamSession> findById(UUID id) {
        return springDataExamSessionRepository.findById(id)
            .map(ExamSessionMapper::toDomain);
    }

    @Override
    public Optional<ExamSession> findLatestByExamIdAndCandidateId(UUID examId, UUID candidateId) {
        return springDataExamSessionRepository.findTopByExamIdAndCandidateIdOrderByStartedAtDesc(examId, candidateId)
            .map(ExamSessionMapper::toDomain);
    }

    @Override
    public Optional<ExamSession> findLatestByCandidateId(UUID candidateId) {
        return springDataExamSessionRepository.findTopByCandidateIdOrderByStartedAtDesc(candidateId)
            .map(ExamSessionMapper::toDomain);
    }

    @Override
    public Optional<ExamSession> findLatestByCandidateIdAndStatuses(UUID candidateId, Collection<ExamSessionStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return Optional.empty();
        }
        var rawStatuses = statuses.stream().map(Enum::name).toList();
        return springDataExamSessionRepository.findTopByCandidateIdAndStatusInOrderByStartedAtDesc(candidateId, rawStatuses)
            .map(ExamSessionMapper::toDomain);
    }

    @Override
    public List<ExamSession> findAllByCandidateId(UUID candidateId) {
        return springDataExamSessionRepository.findByCandidateId(candidateId).stream()
            .map(ExamSessionMapper::toDomain)
            .toList();
    }

    @Override
    public List<ExamSession> findAllByCandidateIdIn(Collection<UUID> candidateIds) {
        return springDataExamSessionRepository.findByCandidateIdIn(candidateIds).stream()
            .map(ExamSessionMapper::toDomain)
            .toList();
    }

    @Override
    public List<ExamSession> findDeferredGradingCandidates(OffsetDateTime now) {
        return springDataExamSessionRepository.findDeferredGradingCandidates(now).stream()
            .map(ExamSessionMapper::toDomain)
            .toList();
    }

    @Override
    public List<ExamSession> findPastScheduleEndCandidates(OffsetDateTime threshold) {
        return springDataExamSessionRepository.findPastScheduleEndCandidates(threshold).stream()
            .map(ExamSessionMapper::toDomain)
            .toList();
    }

    @Override
    public boolean existsById(UUID id) {
        return springDataExamSessionRepository.existsById(id);
    }

    @Override
    public ExamSession save(ExamSession session) {
        var saved = springDataExamSessionRepository.save(ExamSessionMapper.toJpa(session));
        return ExamSessionMapper.toDomain(saved);
    }

    @Override
    public void deleteById(UUID id) {
        springDataExamSessionRepository.deleteById(id);
    }

    @Override
    public Optional<ExamSession> findByIdAndInProgress(UUID id) {
        return springDataExamSessionRepository.findByIdAndStatus(id, ExamSessionStatus.IN_PROGRESS.name())
            .map(ExamSessionMapper::toDomain);
    }

    @Override
    public Optional<ExamSession> findActiveByExamIdAndCandidateId(UUID examId, UUID candidateId) {
        return springDataExamSessionRepository.findByExamIdAndCandidateIdAndStatus(examId, candidateId, ExamStatus.IN_PROGRESS.name())
            .map(ExamSessionMapper::toDomain);
    }

    @Override
    public List<ExamSession> findActiveByIdInAndSchoolId(Collection<UUID> ids, OffsetDateTime now, UUID schoolId) {
        return springDataExamSessionRepository.findActiveByIdInAndSchoolId(ids, now, schoolId)
            .stream()
            .map(ExamSessionMapper::toDomain)
            .toList();
    }
}
