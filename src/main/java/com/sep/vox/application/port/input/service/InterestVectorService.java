package com.sep.vox.application.port.input.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.model.personalization.TopicInterestEvent;
import com.sep.vox.domain.model.personalization.TopicInterestScoreEntry;
import com.sep.vox.domain.repository.personalization.DimensionInterestScoreRepository;
import com.sep.vox.domain.repository.personalization.LearnerProfileRepository;
import com.sep.vox.domain.repository.personalization.PracticeTopicRepository;
import com.sep.vox.domain.repository.personalization.TopicInterestEventRepository;
import com.sep.vox.domain.repository.personalization.TopicInterestScoreRepository;

/**
 * Tính vector sở thích chủ đề (EMA theo topic) và vector sở thích theo dimension.
 * Gộp từ PracticePlanningRepositoryImpl vì đây là thuật toán chạm nhiều bảng/nhiều aggregate
 * (interest event, interest score, dimension score, learner profile, topic) -- không phải
 * việc của một adapter CRUD đơn lẻ.
 */
@Service
public class InterestVectorService {

    private final TopicInterestEventRepository eventRepository;
    private final TopicInterestScoreRepository scoreRepository;
    private final DimensionInterestScoreRepository dimensionScoreRepository;
    private final LearnerProfileRepository learnerProfileRepository;
    private final PracticeTopicRepository topicRepository;

    public InterestVectorService(
            TopicInterestEventRepository eventRepository,
            TopicInterestScoreRepository scoreRepository,
            DimensionInterestScoreRepository dimensionScoreRepository,
            LearnerProfileRepository learnerProfileRepository,
            PracticeTopicRepository topicRepository) {
        this.eventRepository = eventRepository;
        this.scoreRepository = scoreRepository;
        this.dimensionScoreRepository = dimensionScoreRepository;
        this.learnerProfileRepository = learnerProfileRepository;
        this.topicRepository = topicRepository;
    }

    @Transactional
    public void appendInterestEvent(
            UUID studentId,
            UUID topicId,
            UUID sessionId,
            String eventType,
            double signal) {
        eventRepository.save(studentId, topicId, sessionId, eventType, signal);
    }

    @Transactional
    public void recordSessionOutcome(
            UUID studentId,
            UUID topicId,
            UUID sessionId,
            String origin,
            String diagnosis,
            boolean completed,
            List<UUID> offeredTopicIds,
            List<UUID> previousOfferedTopicIds) {
        if (!completed && !"BORED".equals(diagnosis)) {
            return;
        }
        var signal = completed
            ? switch (origin) {
                case "KEYWORD" -> 1.00;
                case "EXPLORATION", "RANDOM" -> 0.60;
                default -> 0.95;
            }
            : switch (origin) {
                case "KEYWORD" -> 0.20;
                case "EXPLORATION", "RANDOM" -> 0.10;
                default -> 0.15;
            };
        appendInterestEvent(studentId, topicId, sessionId, "SESSION_OUTCOME", signal);
        var alreadyRecorded = new HashSet<UUID>();
        alreadyRecorded.add(topicId);
        appendSkippedOffers(studentId, sessionId, offeredTopicIds, 0.30, alreadyRecorded);
        appendSkippedOffers(studentId, sessionId, previousOfferedTopicIds, 0.20, alreadyRecorded);
        recomputeInterest(studentId);
    }

    @Transactional
    public void recomputeInterest(UUID studentId) {
        var events = eventRepository.findByStudent(studentId);
        var scores = new LinkedHashMap<UUID, Double>();
        var sessions = new HashMap<UUID, Set<UUID>>();
        var last = new HashMap<UUID, Instant>();
        for (var event : events) {
            scores.compute(
                event.getTopicId(),
                (key, value) -> 0.3 * event.getSignal() + 0.7 * (value == null ? 0.5 : value)
            );
            if (event.getSessionId() != null) {
                sessions.computeIfAbsent(event.getTopicId(), ignored -> new HashSet<>())
                    .add(event.getSessionId());
            }
            last.put(event.getTopicId(), event.getOccurredAt());
        }
        var newScores = new ArrayList<TopicInterestScoreEntry>();
        for (var entry : scores.entrySet()) {
            newScores.add(new TopicInterestScoreEntry(
                entry.getKey(),
                entry.getValue(),
                sessions.getOrDefault(entry.getKey(), Set.of()).size(),
                last.get(entry.getKey())
            ));
        }
        scoreRepository.replaceForStudent(studentId, newScores);
        recomputeDimensionInterest(studentId, events);
    }

    private void appendSkippedOffers(
            UUID studentId,
            UUID sessionId,
            List<UUID> topicIds,
            double signal,
            Set<UUID> alreadyRecorded) {
        for (var offeredId : topicIds == null ? List.<UUID>of() : topicIds) {
            if (offeredId != null && alreadyRecorded.add(offeredId)) {
                appendInterestEvent(studentId, offeredId, sessionId, "OFFERED_NOT_CHOSEN", signal);
            }
        }
    }

    private void recomputeDimensionInterest(
            UUID studentId,
            List<TopicInterestEvent> events) {
        var profile = learnerProfileRepository.findCurrent(studentId).orElse(null);
        if (profile == null) {
            return;
        }
        var profileId = profile.getId();
        dimensionScoreRepository.primeBaselineFromScoreWhereMissing(profileId);
        var dimensions = topicRepository.findAllTopicDimensions();
        var scores = new HashMap<>(dimensionScoreRepository.findByLearnerProfile(profileId));
        for (var event : events) {
            var dimension = dimensions.get(event.getTopicId());
            if (dimension == null) {
                continue;
            }
            scores.compute(
                dimension,
                (key, value) -> 0.1 * event.getSignal() + 0.9 * (value == null ? 0.5 : value)
            );
        }
        for (var entry : scores.entrySet()) {
            dimensionScoreRepository.upsertScore(profileId, entry.getKey(), entry.getValue());
        }
    }
}
