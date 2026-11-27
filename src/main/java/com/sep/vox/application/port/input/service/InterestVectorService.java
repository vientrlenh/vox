package com.sep.vox.application.port.input.service;

import com.sep.vox.domain.model.personalization.LearnerProfile;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.model.personalization.TopicInterestEvent;
import com.sep.vox.domain.model.personalization.TopicInterestScoreEntry;
import com.sep.vox.domain.repository.DimensionInterestScoreRepository;
import com.sep.vox.domain.repository.LearnerProfileRepository;
import com.sep.vox.domain.repository.PracticeTopicRepository;
import com.sep.vox.domain.repository.TopicInterestEventRepository;
import com.sep.vox.domain.repository.TopicInterestScoreRepository;
import com.sep.vox.domain.service.personalization.SessionDiagnosisPolicy;

/**
 * Computes the topic interest vector (EMA per topic) and the interest vector per dimension.
 * Merged from PracticePlanningRepositoryImpl because this is an algorithm touching many tables/aggregates
 * (interest event, interest score, dimension score, learner profile, topic) -- not
 * the job of a single CRUD adapter.
 */
@Service
@RequiredArgsConstructor
public class InterestVectorService {

    private final TopicInterestEventRepository eventRepository;
    private final TopicInterestScoreRepository scoreRepository;
    private final DimensionInterestScoreRepository dimensionScoreRepository;
    private final LearnerProfileRepository learnerProfileRepository;
    private final PracticeTopicRepository topicRepository;

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
            UUID studentId, UUID topicId, UUID sessionId, String origin, String diagnosis,
            boolean completed, int spokenSeconds, List<UUID> offeredTopicIds, List<UUID> previousOfferedTopicIds) {
        if (!completed && !"BORED".equals(diagnosis)) {
            return;
        }
        // ONE straight line joins "abandoned" (0 seconds spoken) to "full session", replacing two separate
        // levels split by a cliff at exactly 1 second -- see SessionDiagnosisPolicy.signal.
        //
        // The abandoned branch always has spokenSeconds == 0 (ABANDONED <=> gradedSeconds == 0), so it falls
        // onto the lower end of the same formula by itself; it needs no branch of its own, and that way the
        // two ends cannot drift apart when the value table is tuned later.
        double signal = SessionDiagnosisPolicy.signal(origin, spokenSeconds);
        appendInterestEvent(studentId, topicId, sessionId, "SESSION_OUTCOME", signal);
        Set<UUID> alreadyRecorded = new HashSet<UUID>();
        alreadyRecorded.add(topicId);
        appendSkippedOffers(studentId, sessionId, offeredTopicIds, 0.30, alreadyRecorded);
        appendSkippedOffers(studentId, sessionId, previousOfferedTopicIds, 0.20, alreadyRecorded);
        recomputeInterest(studentId);
    }

    @Transactional
    public void recomputeInterest(UUID studentId) {
        List<TopicInterestEvent> events = eventRepository.findByStudent(studentId);
        scoreRepository.replaceForStudent(studentId, aggregateTopicScores(events));
        recomputeDimensionInterest(studentId, events);
    }

    private List<TopicInterestScoreEntry> aggregateTopicScores(List<TopicInterestEvent> events) {
        Map<UUID, Double> scores = new LinkedHashMap<UUID, Double>();
        Map<UUID, Set<UUID>> sessions = new HashMap<UUID, Set<UUID>>();
        // Evidence NOT attached to a session -- see countEvidence.
        Map<UUID, Integer> standalone = new HashMap<UUID, Integer>();
        Map<UUID, Instant> last = new HashMap<UUID, Instant>();
        for (TopicInterestEvent event : events) {
            updateTopicScore(scores, event);
            countEvidence(sessions, standalone, event);
            recordLastSessionOutcome(last, event);
        }
        return toScoreEntries(scores, sessions, standalone, last);
    }

    /**
     * The SCORE counts every event -- including "offered but not chosen", which is exactly the negative
     * information we want to learn from.
     */
    private void updateTopicScore(Map<UUID, Double> scores, TopicInterestEvent event) {
        scores.compute(
            event.getTopicId(),
            (key, value) -> 0.3 * event.getSignal() + 0.7 * (value == null ? 0.5 : value)
        );
    }

    /**
     * sessions_mentioned ALSO counts every event -- it is the AMOUNT OF EVIDENCE, not the number of
     * sessions practised.
     *
     * <p>It goes into exactly one place: gamma = n/(n+2) in ViewPracticeTopicOffersUseCase, which answers "how much
     * do I know about this topic, enough to trust its own score instead of borrowing the score of the whole
     * dimension". Being offered once and then skipped IS evidence about that topic.
     *
     * <p>Filtering it out is shooting yourself in the foot: a topic never practised would stay at n = 0 -> gamma = 0
     * -> interest = d_t entirely -> the topic's own score s_t (pulled down by the 0.30 signal) is NOT used at
     * all. That means skipping it any number of times never changes the ranking.
     *
     * <p>An event NOT attached to any session -- currently only TOPIC_DISMISSED (the student dismisses a card
     * on the topic selection screen). Each click is separate evidence and must be counted.
     *
     * <p>Dropping that branch makes the dismiss feature RUN WITH NO EFFECT: mentions stay at 0
     * -> gamma = 0/(0+2) = 0 -> interest = dimension score entirely -> the topic's own score that was just
     * lowered is not used at all. However many times it is dismissed, the ranking stays exactly the same.
     */
    private void countEvidence(
            Map<UUID, Set<UUID>> sessions, Map<UUID, Integer> standalone, TopicInterestEvent event) {
        if (event.getSessionId() != null) {
            sessions.computeIfAbsent(event.getTopicId(), ignored -> new HashSet<>())
                .add(event.getSessionId());
        } else {
            standalone.merge(event.getTopicId(), 1, (current, delta) -> current + delta);
        }
    }

    /**
     * Only the LAST-event timestamp has to be filtered by type.
     *
     * <p>last_mentioned_at goes into recency = e^(-dt/7 days), then into the penalty factor
     * (1 - 0.4*recency) meaning "just practised, so hold off on offering it again". A topic that was only
     * SHOWN and then scrolled past was never practised, so stamping it "very recent" would
     * penalise it by 40% for something that did not happen.
     *
     * <p>Harmless while the table only holds SESSION_OUTCOME; once OFFERED_NOT_CHOSEN appears
     * it becomes a real bug. This column is nullable and the ranking SQL already has
     * CASE WHEN last_mentioned_at IS NULL THEN 0.0 -- never practised means recency = 0, no
     * penalty, which is the right meaning.
     */
    private void recordLastSessionOutcome(Map<UUID, Instant> last, TopicInterestEvent event) {
        if (event.isSessionOutcome()) {
            last.put(event.getTopicId(), event.getOccurredAt());
        }
    }

    private List<TopicInterestScoreEntry> toScoreEntries(
            Map<UUID, Double> scores,
            Map<UUID, Set<UUID>> sessions,
            Map<UUID, Integer> standalone,
            Map<UUID, Instant> last) {
        List<TopicInterestScoreEntry> newScores = new ArrayList<TopicInterestScoreEntry>();
        for (Map.Entry<UUID, Double> entry : scores.entrySet()) {
            newScores.add(TopicInterestScoreEntry.scored(
                    entry.getKey(),
                    entry.getValue(),
                    sessions.getOrDefault(entry.getKey(), Set.of()).size()
                            + standalone.getOrDefault(entry.getKey(), 0),
                    last.get(entry.getKey())));
        }
        return newScores;
    }

    private void appendSkippedOffers(
            UUID studentId,
            UUID sessionId,
            List<UUID> topicIds,
            double signal,
            Set<UUID> alreadyRecorded) {
        for (UUID offeredId : topicIds == null ? List.<UUID>of() : topicIds) {
            if (offeredId != null && alreadyRecorded.add(offeredId)) {
                appendInterestEvent(studentId, offeredId, sessionId, "OFFERED_NOT_CHOSEN", signal);
            }
        }
    }

    private void recomputeDimensionInterest(
            UUID studentId,
            List<TopicInterestEvent> events) {
        LearnerProfile profile = learnerProfileRepository.findCurrent(studentId).orElse(null);
        if (profile == null) {
            return;
        }
        UUID profileId = profile.getId();
        dimensionScoreRepository.primeBaselineFromScoreWhereMissing(profileId);
        Map<UUID, String> dimensions = findDimensionsOfEventTopics(events);
        Map<String, Double> scores = blendDimensionScores(profileId, events, dimensions);
        for (Map.Entry<String, Double> entry : scores.entrySet()) {
            dimensionScoreRepository.upsertScore(profileId, entry.getKey(), entry.getValue());
        }
    }

    /**
     * Only asks for the dimensions of the topics that are REALLY in this student's events, instead of loading
     * the whole table like the old version (findAllTopicDimensions -> findAll(), full entities, no filter,
     * run after EVERY practice session). The result is identical: an id missing from the table still returns null and falls
     * into the `continue` branch right below, exactly as before.
     */
    private Map<UUID, String> findDimensionsOfEventTopics(List<TopicInterestEvent> events) {
        return topicRepository.findDimensionsByIds(
            events.stream()
                .map(event -> event.getTopicId())
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet())
        );
    }

    private Map<String, Double> blendDimensionScores(
            UUID profileId, List<TopicInterestEvent> events, Map<UUID, String> dimensions) {
        Map<String, Double> scores = new HashMap<>(dimensionScoreRepository.findByLearnerProfile(profileId));
        for (TopicInterestEvent event : events) {
            String dimension = dimensions.get(event.getTopicId());
            if (dimension == null) {
                continue;
            }
            scores.compute(
                dimension,
                (key, value) -> 0.1 * event.getSignal() + 0.9 * (value == null ? 0.5 : value)
            );
        }
        return scores;
    }
}
