package com.sep.vox.infrastructure.persistence.adapter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.repository.PendingEvaluationResponse;
import com.sep.vox.domain.repository.PracticeItemResponseRepository;
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
        var existing = repository
            .findByPracticeSessionIdAndPracticeQuestionId(sessionId, questionId)
            .orElse(null);
        if (existing != null) {
            existing.setAudioUrl(audioUrl != null ? audioUrl : existing.getAudioUrl());
            existing.setTranscript(
                (existing.getTranscript() == null ? "" : existing.getTranscript())
                    + " " + (transcript == null ? "" : transcript)
            );
            // Chỉ đi MỘT chiều false -> true. Câu đã xong thì xong; một lượt nộp lại
            // (retry mạng) mang questionComplete=false không được phép mở lại nó, nếu không
            // câu đó lại rơi vào diện "chưa chấm" và bị xả chấm lần hai.
            existing.setQuestionComplete(existing.isQuestionComplete() || questionComplete);
            return repository.save(existing).getId();
        }
        var saved = repository.save(new PracticeItemResponseJpaEntity(
            UUID.randomUUID(),
            sessionId,
            questionId,
            audioUrl,
            transcript,
            questionComplete
        ));
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
