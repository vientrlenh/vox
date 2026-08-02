package com.sep.vox.infrastructure.persistence.adapter;

import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.repository.personalization.PracticeItemResponseRepository;
import com.sep.vox.infrastructure.persistence.entity.PracticeItemResponseJpaEntity;
import com.sep.vox.infrastructure.persistence.repository.SpringDataPracticeItemResponseRepository;

@Repository
public class PracticeItemResponseRepositoryImpl implements PracticeItemResponseRepository {

    private final SpringDataPracticeItemResponseRepository repository;

    public PracticeItemResponseRepositoryImpl(
            SpringDataPracticeItemResponseRepository repository) {
        this.repository = repository;
    }

    @Override
    public UUID findRubricVersionIdByResponseId(UUID practiceResponseId) {
        return repository.findRubricVersionIdByResponseId(practiceResponseId);
    }

    @Override
    public UUID upsertResponse(
            UUID sessionId,
            UUID questionId,
            String audioUrl,
            String transcript) {
        var existing = repository
            .findByPracticeSessionIdAndPracticeQuestionId(sessionId, questionId)
            .orElse(null);
        if (existing != null) {
            existing.setAudioUrl(audioUrl != null ? audioUrl : existing.getAudioUrl());
            existing.setTranscript(
                (existing.getTranscript() == null ? "" : existing.getTranscript())
                    + " " + (transcript == null ? "" : transcript)
            );
            return repository.save(existing).getId();
        }
        var saved = repository.save(new PracticeItemResponseJpaEntity(
            UUID.randomUUID(),
            sessionId,
            questionId,
            audioUrl,
            transcript
        ));
        return saved.getId();
    }

    @Override
    public boolean existsResponse(UUID sessionId, UUID questionId) {
        return repository.findByPracticeSessionIdAndPracticeQuestionId(sessionId, questionId).isPresent();
    }
}
