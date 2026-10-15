package com.sep.vox.infrastructure.persistence.adapter.assessment;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.assessment.AssessmentItemResponseTurn;
import com.sep.vox.domain.repository.assessment.AssessmentItemResponseTurnRepository;
import com.sep.vox.infrastructure.persistence.mapper.assessment.AssessmentItemResponseTurnMapper;
import com.sep.vox.infrastructure.persistence.repository.assessment.SpringDataAssessmentItemResponseTurnRepository;

@Repository
public class AssessmentItemResponseTurnRepositoryImpl implements AssessmentItemResponseTurnRepository {

    private final SpringDataAssessmentItemResponseTurnRepository springDataAssessmentItemResponseTurnRepository;

    public AssessmentItemResponseTurnRepositoryImpl(
            SpringDataAssessmentItemResponseTurnRepository springDataAssessmentItemResponseTurnRepository) {
        this.springDataAssessmentItemResponseTurnRepository = springDataAssessmentItemResponseTurnRepository;
    }

    @Override
    public AssessmentItemResponseTurn upsert(AssessmentItemResponseTurn turn) {
        var existing = springDataAssessmentItemResponseTurnRepository.findByExamItemResponseIdAndTurnOrder(
            turn.getExamItemResponseId(),
            turn.getTurnOrder()
        ).orElse(null);

        if (existing != null) {
            turn.setId(existing.getId());
            if (turn.getCreatedAt() == null) {
                turn.setCreatedAt(existing.getCreatedAt());
            }
        } else if (turn.getId() == null) {
            turn.setId(UUID.randomUUID());
        }

        if (turn.getCreatedAt() == null) {
            turn.setCreatedAt(Instant.now());
        }

        var saved = springDataAssessmentItemResponseTurnRepository.save(AssessmentItemResponseTurnMapper.toJpa(turn));
        return AssessmentItemResponseTurnMapper.toDomain(saved);
    }

    @Override
    public List<AssessmentItemResponseTurn> findByExamItemResponseId(UUID examItemResponseId) {
        return springDataAssessmentItemResponseTurnRepository.findByExamItemResponseIdOrderByTurnOrderAsc(examItemResponseId)
            .stream()
            .map(AssessmentItemResponseTurnMapper::toDomain)
            .toList();
    }

    @Override
    public Optional<AssessmentItemResponseTurn> findByExamItemResponseIdAndTurnOrder(UUID examItemResponseId, int turnOrder) {
        return springDataAssessmentItemResponseTurnRepository.findByExamItemResponseIdAndTurnOrder(examItemResponseId, turnOrder)
            .map(AssessmentItemResponseTurnMapper::toDomain);
    }

    @Override
    public Optional<AssessmentItemResponseTurn> findLatestByExamItemResponseId(UUID examItemResponseId) {
        return springDataAssessmentItemResponseTurnRepository.findTopByExamItemResponseIdOrderByTurnOrderDesc(examItemResponseId)
            .map(AssessmentItemResponseTurnMapper::toDomain);
    }

    @Override
    public List<SessionFollowupCount> countFollowupsBySessionId(UUID sessionId) {
        return springDataAssessmentItemResponseTurnRepository.countFollowupsBySessionId(sessionId).stream()
            .map(row -> new SessionFollowupCount(row.getExamItemResponseId(), row.getFollowupCount(), row.getTotalTurns()))
            .toList();
    }

    @Override
    public void deleteByExamItemResponseIdIn(Collection<UUID> examItemResponseIds) {
        springDataAssessmentItemResponseTurnRepository.deleteByExamItemResponseIdIn(examItemResponseIds);
    }
}
