package com.sep.vox.infrastructure.persistence.adapter.assessment;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.assessment.AssessmentItemAnswerTurn;
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
    public AssessmentItemAnswerTurn upsert(AssessmentItemAnswerTurn turn) {
        var existing = springDataAssessmentItemResponseTurnRepository.findByAssessmentItemAnswerIdAndTurnOrder(
            turn.getAssessmentItemAnswerId(),
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
    public List<AssessmentItemAnswerTurn> findByAssessmentItemAnswerId(UUID assessmentItemAnswerId) {
        return springDataAssessmentItemResponseTurnRepository.findByAssessmentItemAnswerIdOrderByTurnOrderAsc(assessmentItemAnswerId)
            .stream()
            .map(AssessmentItemResponseTurnMapper::toDomain)
            .toList();
    }

    @Override
    public Optional<AssessmentItemAnswerTurn> findByAssessmentItemAnswerIdAndTurnOrder(UUID assessmentItemAnswerId, int turnOrder) {
        return springDataAssessmentItemResponseTurnRepository.findByAssessmentItemAnswerIdAndTurnOrder(assessmentItemAnswerId, turnOrder)
            .map(AssessmentItemResponseTurnMapper::toDomain);
    }

    @Override
    public Optional<AssessmentItemAnswerTurn> findLatestByAssessmentItemAnswerId(UUID assessmentItemAnswerId) {
        return springDataAssessmentItemResponseTurnRepository.findTopByAssessmentItemAnswerIdOrderByTurnOrderDesc(assessmentItemAnswerId)
            .map(AssessmentItemResponseTurnMapper::toDomain);
    }

    @Override
    public List<SessionFollowupCount> countFollowupsBySessionId(UUID sessionId) {
        return springDataAssessmentItemResponseTurnRepository.countFollowupsBySessionId(sessionId).stream()
            .map(row -> new SessionFollowupCount(row.getAssessmentItemAnswerId(), row.getFollowupCount(), row.getTotalTurns()))
            .toList();
    }

    @Override
    public void deleteByAssessmentItemAnswerIdIn(Collection<UUID> assessmentItemAnswerIds) {
        springDataAssessmentItemResponseTurnRepository.deleteByAssessmentItemAnswerIdIn(assessmentItemAnswerIds);
    }
}
