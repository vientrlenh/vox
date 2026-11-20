package com.sep.vox.infrastructure.persistence.adapter;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.exam.ExamItemResponseTurn;
import com.sep.vox.domain.repository.ExamItemResponseTurnRepository;
import com.sep.vox.infrastructure.persistence.mapper.ExamItemResponseTurnMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataExamItemResponseTurnRepository;

@Repository
public class ExamItemResponseTurnRepositoryImpl implements ExamItemResponseTurnRepository {

    private final SpringDataExamItemResponseTurnRepository springDataExamItemResponseTurnRepository;

    public ExamItemResponseTurnRepositoryImpl(
            SpringDataExamItemResponseTurnRepository springDataExamItemResponseTurnRepository) {
        this.springDataExamItemResponseTurnRepository = springDataExamItemResponseTurnRepository;
    }

    @Override
    public ExamItemResponseTurn upsert(ExamItemResponseTurn turn) {
        var existing = springDataExamItemResponseTurnRepository.findByExamItemResponseIdAndTurnOrder(
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

        var saved = springDataExamItemResponseTurnRepository.save(ExamItemResponseTurnMapper.toJpa(turn));
        return ExamItemResponseTurnMapper.toDomain(saved);
    }

    @Override
    public List<ExamItemResponseTurn> findByExamItemResponseId(UUID examItemResponseId) {
        return springDataExamItemResponseTurnRepository.findByExamItemResponseIdOrderByTurnOrderAsc(examItemResponseId)
            .stream()
            .map(ExamItemResponseTurnMapper::toDomain)
            .toList();
    }

    @Override
    public Optional<ExamItemResponseTurn> findByExamItemResponseIdAndTurnOrder(UUID examItemResponseId, int turnOrder) {
        return springDataExamItemResponseTurnRepository.findByExamItemResponseIdAndTurnOrder(examItemResponseId, turnOrder)
            .map(ExamItemResponseTurnMapper::toDomain);
    }

    @Override
    public Optional<ExamItemResponseTurn> findLatestByExamItemResponseId(UUID examItemResponseId) {
        return springDataExamItemResponseTurnRepository.findTopByExamItemResponseIdOrderByTurnOrderDesc(examItemResponseId)
            .map(ExamItemResponseTurnMapper::toDomain);
    }

    @Override
    public List<SessionFollowupCount> countFollowupsBySessionId(UUID sessionId) {
        return springDataExamItemResponseTurnRepository.countFollowupsBySessionId(sessionId).stream()
            .map(row -> new SessionFollowupCount(row.getExamItemResponseId(), row.getFollowupCount(), row.getTotalTurns()))
            .toList();
    }

    @Override
    public void deleteByExamItemResponseIdIn(Collection<UUID> examItemResponseIds) {
        springDataExamItemResponseTurnRepository.deleteByExamItemResponseIdIn(examItemResponseIds);
    }
}
