package com.sep.vox.infrastructure.persistence.adapter.assessment;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.assessment.AssessmentRequiredStreamType;
import com.sep.vox.domain.model.assessment.AssessmentSession;
import com.sep.vox.domain.model.assessment.AssessmentSessionStatus;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.repository.assessment.AssessmentSessionRepository;
import com.sep.vox.infrastructure.persistence.mapper.assessment.AssessmentSessionMapper;
import com.sep.vox.infrastructure.persistence.repository.assessment.SpringDataAssessmentSessionRepository;

@Repository
public class AssessmentSessionRepositoryImpl implements AssessmentSessionRepository {

    private final SpringDataAssessmentSessionRepository springDataAssessmentSessionRepository;

    public AssessmentSessionRepositoryImpl(SpringDataAssessmentSessionRepository springDataAssessmentSessionRepository) {
        this.springDataAssessmentSessionRepository = springDataAssessmentSessionRepository;
    }



    @Override
    public Optional<AssessmentSession> findById(UUID id) {
        return springDataAssessmentSessionRepository.findById(id)
            .map(AssessmentSessionMapper::toDomain);
    }

    @Override
    public Optional<AssessmentSession> findLatestByAssessmentIdAndCandidateId(UUID assessmentId, UUID candidateId) {
        return springDataAssessmentSessionRepository.findTopByAssessmentIdAndCandidateIdOrderByStartedAtDesc(assessmentId, candidateId)
            .map(AssessmentSessionMapper::toDomain);
    }

    @Override
    public Optional<AssessmentSession> findLatestByCandidateId(UUID candidateId) {
        return springDataAssessmentSessionRepository.findTopByCandidateIdOrderByStartedAtDesc(candidateId)
            .map(AssessmentSessionMapper::toDomain);
    }

    @Override
    public Optional<AssessmentSession> findLatestByCandidateIdAndStatuses(UUID candidateId, Collection<AssessmentSessionStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return Optional.empty();
        }
        var rawStatuses = statuses.stream().map(s -> s.name()).toList();
        return springDataAssessmentSessionRepository.findTopByCandidateIdAndStatusInOrderByStartedAtDesc(candidateId, rawStatuses)
            .map(AssessmentSessionMapper::toDomain);
    }

    @Override
    public List<AssessmentSession> findByCandidateId(UUID candidateId) {
        return springDataAssessmentSessionRepository.findByCandidateId(candidateId).stream()
            .map(AssessmentSessionMapper::toDomain)
            .toList();
    }

    @Override
    public List<AssessmentSession> findByCandidateIdIn(Collection<UUID> candidateIds) {
        return springDataAssessmentSessionRepository.findByCandidateIdIn(candidateIds).stream()
            .map(AssessmentSessionMapper::toDomain)
            .toList();
    }

    @Override
    public long countActiveByAssessmentId(UUID assessmentId) {
        return springDataAssessmentSessionRepository.countActiveByAssessmentId(assessmentId);
    }

    @Override
    public List<AssessmentSession> findDeferredGradingCandidates(Instant now) {
        return springDataAssessmentSessionRepository.findDeferredGradingCandidates(now).stream()
            .map(AssessmentSessionMapper::toDomain)
            .toList();
    }

    @Override
    public List<AssessmentSession> findPastScheduleEndCandidates(Instant threshold) {
        return springDataAssessmentSessionRepository.findPastScheduleEndCandidates(threshold).stream()
            .map(AssessmentSessionMapper::toDomain)
            .toList();
    }

    @Override
    public boolean existsById(UUID id) {
        return springDataAssessmentSessionRepository.existsById(id);
    }

    @Override
    public boolean existsByPaperId(UUID paperId) {
        return springDataAssessmentSessionRepository.existsByPaperId(paperId);
    }

    @Override
    public AssessmentSession save(AssessmentSession session) {
        var saved = springDataAssessmentSessionRepository.save(AssessmentSessionMapper.toJpa(session));
        return AssessmentSessionMapper.toDomain(saved);
    }

    @Override
    public void deleteById(UUID id) {
        springDataAssessmentSessionRepository.deleteById(id);
    }

    @Override
    public int softDelete(UUID id, Instant deletedAt, String reason) {
        return springDataAssessmentSessionRepository.softDelete(id, deletedAt, reason);
    }

    @Override
    public Optional<AssessmentSession> findByIdAndResumable(UUID id) {
        var rawStatuses = AssessmentSessionStatus.RESUMABLE.stream().map(s -> s.name()).toList();
        return springDataAssessmentSessionRepository.findByIdAndStatusIn(id, rawStatuses)
            .map(AssessmentSessionMapper::toDomain);
    }

    @Override
    public Optional<AssessmentSession> findActiveByAssessmentIdAndCandidateId(UUID assessmentId, UUID candidateId) {
        return springDataAssessmentSessionRepository.findByAssessmentIdAndCandidateIdAndStatus(assessmentId, candidateId, AssessmentStatus.IN_PROGRESS.name())
            .map(AssessmentSessionMapper::toDomain);
    }

    @Override
    public int lockChosenStreamType(UUID id, AssessmentRequiredStreamType chosenStreamType) {
        return springDataAssessmentSessionRepository.lockChosenStreamType(id, chosenStreamType.name());
    }

    @Override
    public int checkpointRemainingSeconds(UUID id, int remainingSeconds) {
        return springDataAssessmentSessionRepository.checkpointRemainingSeconds(id, remainingSeconds);
    }

    @Override
    public List<AssessmentSession> findActiveByIdInAndSchoolId(Collection<UUID> ids, Instant now, UUID schoolId) {
        return springDataAssessmentSessionRepository.findActiveByIdInAndSchoolId(ids, now, schoolId)
            .stream()
            .map(AssessmentSessionMapper::toDomain)
            .toList();
    }

    @Override
    public boolean tryTransitionStatus(UUID sessionId, AssessmentSessionStatus from, AssessmentSessionStatus to) {
        return springDataAssessmentSessionRepository.tryTransitionStatus(sessionId, from.name(), to.name()) > 0;
    }

}
