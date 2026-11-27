package com.sep.vox.application.port.input.service;

import com.sep.vox.application.response.input.practiceplanning.PracticePlanningResponses;
import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.sep.vox.application.port.output.TopicGenerationPort;
import com.sep.vox.application.port.output.TopicGenerationPort.KeywordEvidence;
import com.sep.vox.application.query.repository.PracticeTopicQueryRepository;
import com.sep.vox.application.response.input.practiceplanning.PracticePlanningResponses.PracticeTopicOffer;
import com.sep.vox.application.response.input.topicsuggestion.TopicSuggestionResponses.TopicFromKeywordResult;
import com.sep.vox.domain.model.personalization.PracticeTopic;
import com.sep.vox.domain.model.personalization.TopicSuggestion;
import com.sep.vox.domain.repository.InterestDimensionRepository;
import com.sep.vox.domain.repository.LearnerProfileRepository;
import com.sep.vox.domain.repository.PracticeTopicRepository;
import com.sep.vox.domain.repository.TopicSuggestionRepository;

/**
 * Suggests / creates / matches practice topics -- merged from TopicSuggestionRepositoryImpl because this is a
 * business algorithm (fuzzy matching, calling the LLM, de-duplication, filtering by school scope) touching many aggregates
 * (TopicSuggestion, PracticeTopic, LearnerProfile), not the job of 1 CRUD adapter.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TopicSuggestionService {

    private static final Set<String> UNSUITABLE = Set.of(
        "porn", "sex", "drugs", "weapon", "gambling", "hate", "suicide"
    );

    private final TopicSuggestionRepository topicSuggestionRepository;
    private final PracticeTopicRepository practiceTopicRepository;
    private final PracticeTopicQueryRepository practiceTopicQueryRepository;
    private final LearnerProfileRepository learnerProfileRepository;
    private final TopicGenerationPort topicGenerationPort;
    private final PracticeTopicOfferEnrichmentService enrichmentService;
    private final KeywordTopicPersistenceService keywordTopicPersistenceService;
    private final InterestDimensionRepository interestDimensionRepository;

    // REMOVED 2026-08-06: respond(studentId, suggestionId, accept) -- accept/skip a topic suggestion.
    // No source creates PENDING rows any more after the path that inferred topics from what the student said was removed,
    // so there is nothing left to review. The interface, GraphQL and the accompanying use case were deleted at the same time.

    // Not @Transactional -- generationClient.propose() is a slow synchronous LLM call to the
    // Python agents service; see PracticeQuestionGenerationService.generateAndStore for the
    // same fix + rationale (HikariCP connection-pool starvation under load). Each repository
    // call here self-transacts via Spring Data's proxy.
    /**
     * There is NO quota of 3 requests/week here any more (removed 2026-08-07). The old condition was
     * `weeklyRequestCount(studentId) >= 3 || unsuitable(normalized)`.
     *
     * <p>It counted EVERY row that has a keyword in the calendar week, without filtering by result -- but
     * recordKeywordRequest writes a row in all four branches, so the three branches that cost NO LLM call
     * (MATCHED_EXISTING, REJECTED_UNSUITABLE, OUT_OF_ASSESSMENT_SCOPE) were still charged against the quota. A student
     * who typed a keyword pointing exactly at an existing topic also lost one of the three requests.
     *
     * <p>The real cost already has its proper place of control: the PRACTICE quota is computed by speaking seconds
     * (SubmitPracticeTurnUseCase -> ConsumeQuotaService). Blocking here as well tightens the same
     * pocket of money twice, by an unrelated unit.
     */
    public TopicFromKeywordResult generateFromKeyword(UUID studentId, String keyword) {
        String normalized = normalize(keyword);
        if (normalized.isBlank()) {
            return new TopicFromKeywordResult(null, "REJECTED_UNSUITABLE");
        }
        TopicNameCard existingTopic = findNearExistingTopic(normalized);
        if (existingTopic != null) {
            return matchedExisting(studentId, keyword, existingTopic);
        }
        if (unsuitable(normalized)) {
            recordKeywordRequest(studentId, keyword, "REJECTED_UNSUITABLE");
            return new TopicFromKeywordResult(null, "REJECTED_UNSUITABLE");
        }
        if ("ASSESSMENT_PREP".equals(currentGoal(studentId))) {
            // ASSESSMENT_PREP only practices on topics taken from the question bank (question_bank/
            // question_topic) of exactly the school + grade -- it does not let the AI generate new topics from
            // free keywords, and keeps the result code OUT_OF_ASSESSMENT_SCOPE (the meaning is still correct).
            recordKeywordRequest(studentId, keyword, "OUT_OF_ASSESSMENT_SCOPE");
            return new TopicFromKeywordResult(null, "OUT_OF_ASSESSMENT_SCOPE");
        }
        return proposeAndCreateTopic(studentId, keyword);
    }

    private TopicFromKeywordResult matchedExisting(UUID studentId, String keyword, TopicNameCard topic) {
        recordKeywordRequest(studentId, keyword, "MATCHED_EXISTING");
        return new TopicFromKeywordResult(
            offerFor(
                studentId, topic.id(), topic.name(),
                topic.interestDimension(), false, null, null
            ),
            "MATCHED_EXISTING"
        );
    }

    private TopicFromKeywordResult proposeAndCreateTopic(UUID studentId, String keyword) {
        List<TopicGenerationPort.TopicProposal> proposals = topicGenerationPort.propose(
            studentId,
            List.of(new KeywordEvidence(keyword, 1)),
            interestScores(studentId),
            List.of(),   // see the note at the removed rejectedTopicNames
            practiceTopicRepository.findExhaustedTopicNames(studentId),
            true,
            1,
            dimensionCodes()
        );
        if (proposals.isEmpty()) {
            recordKeywordRequest(studentId, keyword, "REJECTED_UNSUITABLE");
            return new TopicFromKeywordResult(null, "REJECTED_UNSUITABLE");
        }
        TopicGenerationPort.TopicProposal proposal = proposals.get(0);
        TopicNameCard duplicateTopic = findNearExistingTopic(normalize(proposal.name()));
        if (duplicateTopic != null) {
            return matchedExisting(studentId, keyword, duplicateTopic);
        }
        return createTopicFromProposal(studentId, keyword, proposal);
    }

    /**
     * Creating the topic + writing the request-log row must be atomic together -- split into a separate bean
     * with @Transactional (see KeywordTopicPersistenceService), and NO transaction wraps the whole of
     * generateFromKeyword because generationClient.propose() above is a slow LLM call.
     */
    private TopicFromKeywordResult createTopicFromProposal(
            UUID studentId, String keyword, TopicGenerationPort.TopicProposal proposal) {
        UUID topicId = keywordTopicPersistenceService.createTopicAndRecordRequest(
            studentId,
            keyword,
            proposal.name(),
            proposal.interestDimension(),
            proposal.curriculumGroup(),
            proposal.temporalAffordance()
        );
        topicGenerationPort.index(
            topicId.toString(), proposal.name(), proposal.reasonText(), true, null, "ACTIVE"
        );
        return new TopicFromKeywordResult(
            offerFor(
                studentId, topicId, proposal.name(), proposal.interestDimension(), false,
                (int) Math.round(proposal.confidence() * 100), proposal.reasonText()
            ),
            "CREATED"
        );
    }

    // REMOVED 2026-08-06 -- refreshSuggestions / studentsDueForSuggestionRefresh.
    //
    // The old path: read the last 30 days of transcripts, count content words by number of sessions that mentioned them, take the 30
    // top words as evidence and ask the LLM to propose new topics -- "the AI heard you often mention X".
    // Removed on request: the system no longer infers topics from what students say.
    //
    // The two REMAINING topic-generation paths do not read transcripts, so they are untouched:
    //   - generateFromKeyword  : the student types a keyword, the evidence is that keyword itself
    //   - synchronousOffers    : passes empty evidence, relies only on interest scores
    //
    // The whole suggestion-review flow was deleted too (pendingSuggestions / respond / GraphQL / the Flutter card):
    // with no source creating PENDING rows there is nothing left to review.
    //
    // The topic_suggestions table STILL EXISTS, but is now only a log of keyword requests
    // (recordKeywordRequest) -- written so you can review what students searched, and governs nothing.

    // Not @Transactional -- same reason as generateFromKeyword above. This one is the hottest
    // path (called synchronously from practiceTopicOffers whenever ranked offers are thin), so
    // it's the most load-bearing fix of the three.
    // REMOVED 2026-08-11: MAX_PROPOSALS_PER_RUN = 8. The number of proposals requested from the LLM is now
    // `requestedCount` -- exactly the number needed, computed by TopicOfferBackfillService from the bank size
    // (8 while building, 2 when steady). See the comment at the propose() call.
    /**
     * The candidates are loaded ONCE for the whole loop. The old version called findNearExistingTopic() inside the
     * loop, and that function loads the whole table itself -- i.e. up to MAX_PROPOSALS_PER_RUN loads + re-tokenizing
     * every topic name, on every generation run.
     */
    public List<PracticeTopicOffer> synchronousOffers(UUID studentId, int requestedCount) {
        if (requestedCount <= 0 || "ASSESSMENT_PREP".equals(currentGoal(studentId))) {
            return List.of();
        }
        List<TopicGenerationPort.TopicProposal> proposals = proposeFromInterests(studentId, requestedCount);
        List<PracticePlanningResponses.PracticeTopicOffer> result = new ArrayList<PracticeTopicOffer>();
        List<TopicNameCard> candidates = activeNameCards();
        List<String> collidedWith = new ArrayList<String>();
        int skippedOverQuota = 0;
        for (TopicGenerationPort.TopicProposal proposal : proposals) {
            if (result.size() >= requestedCount) {
                skippedOverQuota++;
                continue;
            }
            TopicNameCard duplicate = findNearExistingTopic(normalize(proposal.name()), candidates);
            if (duplicate != null) {
                collidedWith.add(proposal.name() + " ~ " + duplicate.name());
                continue;
            }
            result.add(createOffer(studentId, proposal, candidates));
        }
        logGenerationRun(studentId, requestedCount, proposals.size(), collidedWith, skippedOverQuota, result.size());
        return result;
    }

    /**
     * Ask for EXACTLY the number needed, no more.
     *
     * <p>The previous version hard-coded MAX_PROPOSALS_PER_RUN = 8 with the reason "the near-duplicate filter cuts very
     * hard, asking for exactly the number needed yields only 1 topic per run". That reason was right at the time,
     * but it compensated by paying UP FRONT for the part that may be thrown away: in steady mode
     * (bank >= POOL_TARGET) requestedCount is only 2, so 6 proposals were generated, EMBEDDED
     * in TopicDedupeNode, then dropped -- every session.
     *
     * <p>Now Python has a re-proposal loop (MAX_PROPOSAL_ROUNDS): a collision is no longer thrown away silently but
     * comes back as a prompt carrying exactly the topic name it just collided with. So we only pay extra
     * WHEN a collision really happens, instead of paying up front on every run.
     *
     * <p>It must still be <= MAX_TOPIC_PROPOSALS on the Python side (schemas/topic_generation.py), otherwise
     * a 422 right at the door -- the largest requestedCount is BACKFILL_COUNT_BUILDING = 8, still within it.
     */
    private List<TopicGenerationPort.TopicProposal> proposeFromInterests(UUID studentId, int requestedCount) {
        return topicGenerationPort.propose(
            studentId,
            List.of(),
            interestScores(studentId),
            List.of(),   // see the note at the removed rejectedTopicNames
            practiceTopicRepository.findExhaustedTopicNames(studentId),
            false,
            requestedCount,
            dimensionCodes()
        );
    }

    /**
     * MANDATORY: append the just-created topic to the candidate list. The old version reloaded the DB on every iteration so it
     * naturally saw what had just been created; hoisting the load out and forgetting this step loses the duplicate guard WITHIN
     * THE SAME RUN -- exactly the situation described at the propose() call: the LLM likes to propose clusters of nearby
     * topics, so the 2nd-3rd one must be compared against the 1st one that was just created.
     */
    private PracticeTopicOffer createOffer(
            UUID studentId, TopicGenerationPort.TopicProposal proposal, List<TopicNameCard> candidates) {
        UUID topicId = createTopic(
            proposal.name(), proposal.interestDimension(), proposal.curriculumGroup(),
            "AI_SUGGESTED", proposal.temporalAffordance()
        );
        candidates.add(new TopicNameCard(
            topicId, proposal.name(), proposal.interestDimension()
        ));
        topicGenerationPort.index(
            topicId.toString(), proposal.name(), proposal.reasonText(), true, null, "ACTIVE"
        );
        return offerFor(
            studentId, topicId, proposal.name(), proposal.interestDimension(), false,
            (int) Math.round(proposal.confidence() * 100), proposal.reasonText()
        );
    }

    /**
     * Accounting for one generation run. Before this, zero productivity was SWALLOWED SILENTLY:
     * backfillAsync only logs the number created, and "asked for 8 and all 8 were duplicates" cannot be told
     * apart from "the bank is full so nothing needed creating". Running a few days with this line answers three
     * questions: is the collision feedback loop worth doing, what MAX_ROUNDS should be, and whether lowering the
     * re-proposal rounds on the Python side is actually rescuing collisions.
     *
     * <p>`proposalCount` is already the number LEFT after TopicDedupeNode on the Python side filtered with Chroma
     * (cosine threshold 0.90), so the difference from the number requested is exactly the part Python cut.
     */
    private void logGenerationRun(
            UUID studentId, int requestedCount, int proposalCount, List<String> collidedWith,
            int skippedOverQuota, int createdCount) {
        log.info(
            "[topic-gen] student={} requested={} python_returned={} java_blocked_duplicates={} over_quota={} created={}{}",
            studentId, requestedCount, proposalCount, collidedWith.size(),
            skippedOverQuota, createdCount,
            collidedWith.isEmpty() ? "" : " | collisions: " + String.join(" ; ", collidedWith)
        );
    }

    /**
     * The catalog of current interest dimensions, sent down to Python to constrain the LLM output.
     * It includes the system dimension (ACADEMIC_ASSESSMENT) as well because this labels a topic, it is not a question to
     * the student -- unlike the quiz, which only uses the quiz_eligible dimensions.
     */
    private List<String> dimensionCodes() {
        return interestDimensionRepository.findActive().stream()
            .map(dimension -> dimension.getCode())
            .toList();
    }

    // Do NOT add confidenceForSessionCount back here. The confidence cap by amount of evidence
    // (≤1 session → 0.5; 2 → 0.7; ≥3 → 0.85) lives on the PYTHON SIDE, in
    // TopicProposalNode/topic_proposal_node_config.py -- where it is applied together with the other
    // caps (ungrounded 0.4; INTEREST 0.6; EXHAUSTED 0.7; SEARCH 0.95). The old Java version here was
    // a copy that nobody called, and two copies would drift apart on the very first edit.

    public static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String decomposed = Normalizer.normalize(
            value.toLowerCase(Locale.ROOT).strip().replace('đ', 'd'),
            Normalizer.Form.NFD
        );
        return decomposed.replaceAll("\\p{M}", "")
            .replaceAll("[^a-z0-9 ]", " ")
            .replaceAll("\\s+", " ")
            .strip();
    }

    static double tokenSimilarity(String left, String right) {
        Set<String> leftTokens = new HashSet<>(List.of(normalize(left).split(" ")));
        Set<String> rightTokens = new HashSet<>(List.of(normalize(right).split(" ")));
        if (leftTokens.isEmpty() || rightTokens.isEmpty()) {
            return 0;
        }
        Set<String> intersection = new HashSet<>(leftTokens);
        intersection.retainAll(rightTokens);
        Set<String> union = new HashSet<>(leftTokens);
        union.addAll(rightTokens);
        return (double) intersection.size() / union.size();
    }

    /** Loads the candidates and then compares -- for the two SINGLE call sites (not inside a loop). */
    private TopicNameCard findNearExistingTopic(String normalized) {
        return findNearExistingTopic(normalized, activeNameCards());
    }

    /**
     * The pure version: compares against the candidate list passed in, does NOT touch the DB.
     *
     * <p>Split out so {@code synchronousOffers} loads the candidates ONCE before the loop instead of once per
     * iteration. The old version called directly inside the loop over up to {@code MAX_PROPOSALS_PER_RUN}
     * proposals, so each generation run reloaded the whole table 8 times and re-tokenized every topic name.
     */
    private TopicNameCard findNearExistingTopic(String normalized, List<TopicNameCard> candidates) {
        return candidates.stream()
            .filter(topic -> normalize(topic.name()).equals(normalized)
                || tokenSimilarity(topic.name(), normalized) >= 0.90)
            .findFirst()
            .orElse(null);
    }

    private List<TopicNameCard> activeNameCards() {
        return practiceTopicQueryRepository.findActiveNameCards().stream()
            .map(card -> new TopicNameCard(card.getId(), card.getName(), card.getInterestDimension()))
            .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    /** A detached record so {@code synchronousOffers} can append the JUST-created topic to the candidate list. */
    private record TopicNameCard(UUID id, String name, String interestDimension) {
    }

    private PracticeTopicOffer offerFor(
            UUID studentId, UUID topicId, String name, String dimension, boolean savedByMe,
            Integer matchPercent, String rationale) {
        return new PracticeTopicOffer(
            topicId,
            name,
            dimension,
            savedByMe,
            matchPercent,
            enrichmentService.minutesForStudent(studentId),
            rationale,
            rationale == null ? List.of() : List.of(rationale)
        );
    }


    private UUID createTopic(
            String name,
            String dimension,
            String curriculumGroup,
            String source,
            String temporalAffordance) {
        PracticeTopic saved = practiceTopicRepository.save(PracticeTopic.generatedFromName(
                source,
                name,
                normalize(name),
                dimension,
                curriculumGroup,
                temporalAffordance,
                Instant.now()));
        return saved.getId();
    }

    private void recordKeywordRequest(UUID studentId, String keyword, String outcome) {
        topicSuggestionRepository.save(TopicSuggestion.keywordRequested(studentId, keyword, outcome, Instant.now()));
    }

    // REMOVED 2026-08-11: topicNames(). It loaded every active topic just to get the names, then sent
    // the whole list down into the prompt. See the comment at the propose() call for why it was dropped.

    // REMOVED 2026-08-06: rejectedTopicNames + nearRejected. Both read rows with status='REJECTED', and
    // the only place that wrote that status was respond() -- deleted. Keeping them means every LLM call has to
    // run two extra queries that are certain to be empty. The corresponding parameter of propose() stays unchanged
    // (pass List.of()) so the contract with Python does not change.

    private Map<String, Double> interestScores(UUID studentId) {
        return practiceTopicRepository.findInterestScoresByDimension(studentId);
    }

    private String currentGoal(UUID studentId) {
        return learnerProfileRepository.findCurrent(studentId)
            .map(profile -> profile.getGoalType() == null ? "ABILITY_IMPROVEMENT" : profile.getGoalType())
            .orElse("ABILITY_IMPROVEMENT");
    }

    private boolean unsuitable(String normalized) {
        return UNSUITABLE.stream().anyMatch(normalized::contains);
    }

}
