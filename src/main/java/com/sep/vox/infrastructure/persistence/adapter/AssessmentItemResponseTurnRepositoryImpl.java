package com.sep.vox.infrastructure.persistence.adapter;

import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentItemResponseTurnJpaEntity;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.assessment.AssessmentItemResponseTurn;
import com.sep.vox.domain.repository.AssessmentItemResponseTurnRepository;
import com.sep.vox.infrastructure.persistence.mapper.AssessmentItemResponseTurnMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataAssessmentItemResponseTurnRepository;

@Repository
@RequiredArgsConstructor
public class AssessmentItemResponseTurnRepositoryImpl implements AssessmentItemResponseTurnRepository {

    private final SpringDataAssessmentItemResponseTurnRepository springDataAssessmentItemResponseTurnRepository;

    @Override
    public AssessmentItemResponseTurn upsert(AssessmentItemResponseTurn turn) {
        AssessmentItemResponseTurnJpaEntity existing = springDataAssessmentItemResponseTurnRepository.findByAssessmentItemResponseIdAndTurnOrder(
            turn.getAssessmentItemResponseId(),
            turn.getTurnOrder()
        ).orElse(null);

        if (existing != null) {
            turn.setId(existing.getId());
            if (turn.getCreatedAt() == null) {
                turn.setCreatedAt(existing.getCreatedAt());
            }
        }

        if (turn.getCreatedAt() == null) {
            turn.setCreatedAt(Instant.now());
        }

        AssessmentItemResponseTurnJpaEntity saved = springDataAssessmentItemResponseTurnRepository.save(AssessmentItemResponseTurnMapper.toJpa(turn));
        return AssessmentItemResponseTurnMapper.toDomain(saved);
    }

    @Override
    public List<AssessmentItemResponseTurn> findByAssessmentItemResponseId(UUID assessmentItemResponseId) {
        return springDataAssessmentItemResponseTurnRepository.findByAssessmentItemResponseIdOrderByTurnOrderAsc(assessmentItemResponseId)
            .stream()
            .map(AssessmentItemResponseTurnMapper::toDomain)
            .toList();
    }

    @Override
    public Optional<AssessmentItemResponseTurn> findByAssessmentItemResponseIdAndTurnOrder(UUID assessmentItemResponseId, int turnOrder) {
        return springDataAssessmentItemResponseTurnRepository.findByAssessmentItemResponseIdAndTurnOrder(assessmentItemResponseId, turnOrder)
            .map(AssessmentItemResponseTurnMapper::toDomain);
    }

    @Override
    public Optional<AssessmentItemResponseTurn> findLatestByAssessmentItemResponseId(UUID assessmentItemResponseId) {
        return springDataAssessmentItemResponseTurnRepository.findTopByAssessmentItemResponseIdOrderByTurnOrderDesc(assessmentItemResponseId)
            .map(AssessmentItemResponseTurnMapper::toDomain);
    }

    @Override
    public List<SessionFollowupCount> countFollowupsBySessionId(UUID sessionId) {
        return springDataAssessmentItemResponseTurnRepository.countFollowupsBySessionId(sessionId).stream()
            .map(row -> new SessionFollowupCount(row.getAssessmentItemResponseId(), row.getFollowupCount(), row.getTotalTurns()))
            .toList();
    }

    @Override
    public void deleteByAssessmentItemResponseIdIn(Collection<UUID> assessmentItemResponseIds) {
        springDataAssessmentItemResponseTurnRepository.deleteByAssessmentItemResponseIdIn(assessmentItemResponseIds);
    }
}
