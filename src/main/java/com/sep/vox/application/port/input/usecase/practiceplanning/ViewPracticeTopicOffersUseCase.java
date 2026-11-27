package com.sep.vox.application.port.input.usecase.practiceplanning;

import com.sep.vox.application.query.dto.RankedTopicInfo;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.practiceplanning.ViewPracticeTopicOffersQuery;
import com.sep.vox.application.port.input.service.PracticeTopicOfferEnrichmentService;
import com.sep.vox.application.port.input.service.TopicSuggestionService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.dto.QuestionTopicInfo;
import com.sep.vox.application.query.repository.PracticeTopicQueryRepository;
import com.sep.vox.application.response.input.practiceplanning.PracticePlanningResponses.PracticeTopicOffer;
import com.sep.vox.domain.model.personalization.PracticeTopic;
import com.sep.vox.domain.repository.LearnerProfileRepository;
import com.sep.vox.domain.repository.PracticePaperRepository;
import com.sep.vox.domain.repository.PracticeTopicRepository;
import com.sep.vox.domain.repository.SchoolClassUserRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
@RequiredArgsConstructor
public class ViewPracticeTopicOffersUseCase implements IUseCase<ViewPracticeTopicOffersQuery, List<PracticeTopicOffer>> {

    private static final String ASSESSMENT_TOPIC_INTEREST_DIMENSION = "ACADEMIC_ASSESSMENT";
    private static final String ASSESSMENT_TOPIC_CURRICULUM_GROUP = "ASSESSMENT_BANK";

    private final PracticeTopicQueryRepository practiceTopicQueryRepository;
    private final LearnerProfileRepository learnerProfileRepository;
    private final PracticePaperRepository practicePaperRepository;
    private final PracticeTopicOfferEnrichmentService enrichmentService;
    private final UserContextPort userContextPort;
    private final PracticeTopicRepository practiceTopicRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final SchoolClassUserRepository schoolClassUserRepository;

    @Override
    @Transactional
    public List<PracticeTopicOffer> execute(ViewPracticeTopicOffersQuery input) {
        UUID studentId = userContextPort.getCurrentAuthenticatedUserId();
        if (!hasCompletedInterestQuiz(studentId)) {
            return List.of();
        }
        Set<UUID> excluded = new HashSet<>(
            input.excludeTopicIds() == null ? List.<UUID>of() : input.excludeTopicIds()
        );
        List<RankedTopic> ranked = rankedTopicsFor(studentId, excluded);
        List<RankedTopic> selected = withEpsilonExploration(studentId, ranked, input.round());
        // Do NOT shuffle the order: the client shows `matchPercent` taken from the very `score` used
        // for ranking, so after a shuffle card #1 would no longer be the highest match -- the user
        // would see a list that contradicts the number it prints, in a different order on every open.
        //
        // Diversity is still there, but in its proper place: the epsilon slot replaces the LAST card
        // with a topic outside the top 10. That topic ranks lower, so it still sits at the end of the
        // list and the order by match % stays monotonic.
        int minutes = enrichmentService.minutesForStudent(studentId);
        return selected.stream()
            .map(topic -> toOffer(topic, minutes))
            .toList();
    }

    private List<RankedTopic> rankedTopicsFor(UUID studentId, Set<UUID> excluded) {
        String goal = currentGoal(studentId);
        // `bucket` no longer branches anything: the enum only has FOR_YOU left (see practice-planning.graphqls).
        return ("ASSESSMENT_PREP".equals(goal)
                ? assessmentTopicRanked(studentId)
                : classicRanked(studentId, goal))
            .stream()
            .filter(topic -> !excluded.contains(topic.id()))
            .sorted(Comparator.comparingDouble((RankedTopic topic) -> topic.score()).reversed())
            .toList();
    }

    private List<RankedTopic> withEpsilonExploration(UUID studentId, List<RankedTopic> ranked, int round) {
        List<RankedTopic> selected = new ArrayList<>(ranked.subList(0, Math.min(4, ranked.size())));
        double epsilon = round >= 2 ? 0.30 : 0.10;
        if (selected.size() == 4
                && ranked.size() > 10
                && explorationAllowed(studentId)
                && ThreadLocalRandom.current().nextDouble() < epsilon) {
            List<RankedTopic> exploratory = ranked.subList(10, ranked.size());
            selected.set(
                selected.size() - 1,
                exploratory.get(ThreadLocalRandom.current().nextInt(exploratory.size()))
            );
        }
        return selected;
    }

    private PracticeTopicOffer toOffer(RankedTopic topic, int minutes) {
        String rationale = rationaleFor(topic);
        return new PracticeTopicOffer(
            topic.id(),
            topic.name(),
            topic.dimension(),
            topic.saved(),
            clampPercent(topic.score()),
            minutes,
            rationale,
            List.of(rationale)
        );
    }

    /** The number of cards at the top of the ranking that count as "in sight" -- a topic outside this threshold
     * can only get into the offered batch through ε-greedy (see execute()). */
    private static final int TOP_RANK_WINDOW = 10;

    /**
     * Whether this topic is OUTSIDE the top-{@value #TOP_RANK_WINDOW} by Final(t).
     *
     * Used to infer whether a session was pushed by system exploration, instead of having to REMEMBER which epsilon
     * slot was offered to whom: remembering needs a map living forever in RAM (leaking with students × topics) or a
     * TTL/LRU with all the bugs that come with it (state lost after a restart, evicting wrongly while the student is
     * still choosing). The ranking is deterministic anyway, so recomputing is cheaper and not wrong.
     */
    @Transactional(readOnly = true)
    public boolean isOutsideTopRanked(UUID studentId, UUID topicId) {
        String goal = currentGoal(studentId);
        if ("ASSESSMENT_PREP".equals(goal)) {
            // The ASSESSMENT_PREP path does not run epsilon-greedy (only the FOR_YOU bucket of the AI-generated pool has it).
            return false;
        }
        List<RankedTopic> ranked = classicRanked(studentId, goal).stream()
            .sorted(Comparator.comparingDouble((RankedTopic topic) -> topic.score()).reversed())
            .toList();
        int position = -1;
        for (int index = 0; index < ranked.size(); index++) {
            if (ranked.get(index).id().equals(topicId)) {
                position = index;
                break;
            }
        }
        // Not found (the topic was just switched off/changed) -> do not dare conclude that it was exploration.
        return position >= TOP_RANK_WINDOW;
    }

    private static int clampPercent(double score) {
        return Math.max(0, Math.min(100, (int) Math.round(score * 100)));
    }

    private static String rationaleFor(RankedTopic topic) {
        if (topic.fromSchool()) {
            return "Matches your curriculum";
        }
        return "Suggested for you";
    }

    
    private static double scoreFor(String goal, double interest, double curriculum) {
        return "ASSESSMENT_PREP".equals(goal)
            ? 0.50 * interest + 0.50 * curriculum
            : 0.80 * interest + 0.20 * curriculum;
    }

    private List<RankedTopic> classicRanked(UUID studentId, String goal) {
        List<RankedTopicInfo> rows = practiceTopicQueryRepository.findRankedTopics(studentId, goal);
        return rows.stream()
            .map(row -> {
                double gamma = (double) row.getMentions() / (row.getMentions() + 2);
                double interest = gamma * row.getTopicScore() + (1 - gamma) * row.getDimensionScore();
                double curriculum = "IN_GDPT2018".equals(row.getCurriculumGroup()) ? 1.0 : 0.3;
                double base = scoreFor(goal, interest, curriculum);
                return new RankedTopic(
                    row.getId(),
                    row.getName(),
                    row.getInterestDimension(),
                    row.getSavedByMe(),
                    base * (1 - 0.4 * row.getRecency()),
                    interest,
                    curriculum,
                    false
                );
            })
            .toList();
    }

    /**
     * Topic source for ASSESSMENT_PREP: the PUBLISHED question bank (question_bank/question_topic)
     * of exactly the student's current school + grade -- the AI-generated pool for ABILITY_IMPROVEMENT is not used.
     * Each topic is lazily materialized into 1 practice_topics row (if it does not exist yet) so that the whole downstream pipeline of
     * question selection / weakness tracking / interest vector runs intact without changes.
     */
    private List<RankedTopic> assessmentTopicRanked(UUID studentId) {
        UUID schoolId = schoolUserRepository.findSchoolIdByUserId(studentId).orElse(null);
        if (schoolId == null) {
            return List.of();
        }
        UUID gradeId = schoolClassUserRepository.findCurrentSchoolGradeId(studentId).orElse(null);
        if (gradeId == null) {
            return List.of();
        }
        return practiceTopicQueryRepository.findPublishedAssessmentTopics(schoolId, gradeId).stream()
            .map(row -> toAssessmentRankedTopic(row))
            .toList();
    }

    private RankedTopic toAssessmentRankedTopic(QuestionTopicInfo row) {
        UUID topicId = materializeAssessmentTopic(row);
        double interest = 0.5;
        double curriculum = 1.0;
        double base = scoreFor("ASSESSMENT_PREP", interest, curriculum);
        return new RankedTopic(
            topicId,
            row.getName(),
            ASSESSMENT_TOPIC_INTEREST_DIMENSION,
            false,
            base,
            interest,
            curriculum,
            true
        );
    }

    private UUID materializeAssessmentTopic(QuestionTopicInfo row) {
        return practiceTopicRepository.findBySourceQuestionTopicId(row.getId())
            .map(existing -> existing.getId())
            .orElseGet(() -> practiceTopicRepository.save(PracticeTopic.fromAssessmentQuestionTopic(
                row.getId(),
                row.getName(),
                TopicSuggestionService.normalize(row.getName()),
                row.getDescription(),
                ASSESSMENT_TOPIC_INTEREST_DIMENSION,
                ASSESSMENT_TOPIC_CURRICULUM_GROUP,
                Instant.now())).getId());
    }

    private boolean explorationAllowed(UUID studentId) {
        return practicePaperRepository.countRecentEpsilonPapers(studentId) < 2;
    }

    private String currentGoal(UUID studentId) {
        return learnerProfileRepository.findCurrent(studentId)
            .map(profile -> profile.getGoalType() == null ? "ABILITY_IMPROVEMENT" : profile.getGoalType())
            .orElse("ABILITY_IMPROVEMENT");
    }

    /**
     * If the interest quiz has not been submitted, offer NO topic at all.
     *
     * <p>This gate must live on the server, not in the app. The Flutter gate only guards the "Practice" tab, while the
     * Home screen -- the tab shown right after login -- also calls {@code practiceTopicOffers} to build the
     * "Topic of the day" card (see {@code PersonalizeRepository.getDashboard}). So logging in alone is enough to
     * run the offer, without touching any tab.
     *
     * <p>Without the gate, {@code dimension_interest_scores} is empty and the ranking query fills it with
     * {@code COALESCE(..., 0.5)}: every topic in the curriculum gets exactly the same score, and the client still
     * prints "% match" from that number. The list looks personalized when there is not a single signal -- a silent
     * error, no failure.
     *
     * <p>Another consequence, and a more important one: an empty offer makes the controller trigger a backfill
     * generation, and that generation sends an EMPTY interest map to the LLM. Measured on production on 2026-08-15:
     * 111 students, 0 profiles, 0 practice sessions, yet the pool already had 6 generic topics generated at 10:25 --
     * and the pool is shared, so all 111 students received them. See also the paired gate in
     * {@code TopicOfferBackfillService.backfillAsync}.
     */
    private boolean hasCompletedInterestQuiz(UUID studentId) {
        return learnerProfileRepository.findCurrent(studentId)
            .map(profile -> profile.getQuizCompletedAt() != null)
            .orElse(false);
    }

    private record RankedTopic(
        UUID id,
        String name,
        String dimension,
        boolean saved,
        double score,
        double interest,
        double curriculum,
        boolean fromSchool) {
    }
}
