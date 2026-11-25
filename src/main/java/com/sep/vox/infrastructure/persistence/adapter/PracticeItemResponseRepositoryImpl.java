package com.sep.vox.infrastructure.persistence.adapter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.repository.PendingEvaluationResponse;
import com.sep.vox.domain.repository.PracticeItemResponseRepository;
import com.sep.vox.infrastructure.persistence.entity.personalization.PracticeItemResponseJpaEntity;
import com.sep.vox.infrastructure.persistence.repository.SpringDataPracticeItemResponseRepository;

@Repository
@RequiredArgsConstructor
public class PracticeItemResponseRepositoryImpl implements PracticeItemResponseRepository {

    private final SpringDataPracticeItemResponseRepository repository;

    @Override
    public UUID findRubricVersionIdByResponseId(UUID practiceResponseId) {
        return repository.findRubricVersionIdByResponseId(practiceResponseId);
    }

    @Override
    public UUID findSessionIdByResponseId(UUID practiceResponseId) {
        return repository.findSessionIdByResponseId(practiceResponseId);
    }

    @Override
    public UUID upsertResponse(
            UUID sessionId,
            UUID questionId,
            String audioUrl,
            String transcript,
            boolean questionComplete) {
        PracticeItemResponseJpaEntity existing = repository
            .findByPracticeSessionIdAndPracticeQuestionId(sessionId, questionId)
            .orElse(null);
        if (existing != null) {
            existing.setAudioUrl(audioUrl != null ? audioUrl : existing.getAudioUrl());
            existing.setTranscript(
                (existing.getTranscript() == null ? "" : existing.getTranscript())
                    + " " + (transcript == null ? "" : transcript)
            );
            // Only moves ONE way, false -> true. A finished question stays finished; a resubmission
            // (network retry) carrying questionComplete=false must not reopen it, otherwise the
            // question would fall back into the "not graded yet" set and be graded a second time.
            existing.setQuestionComplete(existing.isQuestionComplete() || questionComplete);
            return repository.save(existing).getId();
        }
        PracticeItemResponseJpaEntity saved = repository.save(PracticeItemResponseJpaEntity.builder()
                .practiceSessionId(sessionId)
                .practiceQuestionId(questionId)
                .audioUrl(audioUrl)
                .transcript(transcript)
                .questionComplete(questionComplete)
                .build());
        return saved.getId();
    }

    @Override
    public int countAwaitingEvaluation(UUID sessionId) {
        return repository.countAwaitingEvaluation(sessionId);
    }

    @Override
    public Double findAverageDifficultyRank(UUID sessionId) {
        return repository.findAverageDifficultyRank(sessionId);
    }

    @Override
    public List<PendingEvaluationResponse> findResponsesAwaitingFlush(
            UUID sessionId, Instant requestedBefore, int maxAttempts) {
        return repository.findResponsesAwaitingFlush(sessionId, requestedBefore, maxAttempts);
    }

    @Override
    @Transactional
    public void markGradingRequested(UUID responseId, Instant requestedAt) {
        repository.markGradingRequested(responseId, requestedAt);
    }

    @Override
    @Transactional
    public void markGraded(UUID responseId) {
        repository.markGraded(responseId);
    }

    @Override
    @Transactional
    public void markGradingFailed(UUID responseId) {
        repository.markGradingFailed(responseId);
    }

    @Override
    public int countGradingGaveUp(UUID sessionId, int maxAttempts) {
        return repository.countGradingGaveUp(sessionId, maxAttempts);
    }

    @Override
    public List<UUID> findEndedSessionsWithUngradedResponses(Instant since) {
        return repository.findEndedSessionsWithUngradedResponses(since);
    }

    @Override
    public boolean existsResponse(UUID sessionId, UUID questionId) {
        return repository.findByPracticeSessionIdAndPracticeQuestionId(sessionId, questionId).isPresent();
    }
}
