package com.sep.vox.infrastructure.persistence.adapter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.repository.personalization.PracticeResponseTurnRepository;
import com.sep.vox.infrastructure.persistence.entity.PracticeResponseTurnJpaEntity;
import com.sep.vox.infrastructure.persistence.repository.SpringDataPracticeResponseTurnRepository;

@Repository
public class PracticeResponseTurnRepositoryImpl implements PracticeResponseTurnRepository {

    private final SpringDataPracticeResponseTurnRepository repository;

    public PracticeResponseTurnRepositoryImpl(SpringDataPracticeResponseTurnRepository repository) {
        this.repository = repository;
    }

    @Override
    public UUID save(
            UUID practiceResponseId,
            int turnOrder,
            String turnType,
            String promptText,
            String audioUrl,
            String transcript,
            int durationSeconds,
            String wordFeedbackJson,
            Double turnScore) {
        var existing = repository.findByPracticeResponseIdAndTurnOrder(practiceResponseId, turnOrder);
        if (existing.isPresent()) {
            // Same turn already recorded -- a Python retry after a lost HTTP response, not a
            // real second turn (turn_order is client-driven and monotonic per question).
            return existing.get().getId();
        }
        var saved = repository.save(new PracticeResponseTurnJpaEntity(
            UUID.randomUUID(),
            practiceResponseId,
            turnOrder,
            turnType,
            promptText,
            audioUrl,
            transcript,
            durationSeconds,
            wordFeedbackJson,
            turnScore == null ? null : BigDecimal.valueOf(turnScore)
        ));
        return saved.getId();
    }

    @Override
    public int findRemainingQuestionSeconds(UUID sessionId, UUID questionId) {
        var value = repository.findRemainingQuestionSeconds(sessionId, questionId);
        return value == null ? 0 : value;
    }

    @Override
    public List<TurnRecord> findByPracticeResponseIdOrderByTurnOrder(UUID practiceResponseId) {
        return repository.findByPracticeResponseIdOrderByTurnOrder(practiceResponseId).stream()
            .map(this::toRecord)
            .toList();
    }

    @Override
    public List<TurnRecord> findBySessionIdOrderByTurnOrder(UUID sessionId) {
        return repository.findBySessionIdOrderByTurnOrder(sessionId).stream()
            .map(this::toRecord)
            .toList();
    }

    private TurnRecord toRecord(PracticeResponseTurnJpaEntity entity) {
        return new TurnRecord(
            entity.getId(),
            entity.getTurnOrder(),
            entity.getTurnType(),
            entity.getPromptText(),
            entity.getAudioUrl(),
            entity.getTranscript(),
            entity.getDurationSeconds(),
            entity.getWordFeedbackJson(),
            entity.getTurnScore() == null ? null : entity.getTurnScore().doubleValue()
        );
    }
}
