package com.sep.vox.application.port.input.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.domain.model.personalization.InterestQuizSeedItem;

/**
 * Picks exactly N triplets to ask, from the existing item bank.
 *
 * Solves two things that "take the first N items" cannot:
 *
 * 1. EXCLUDE items containing a dimension that can no longer be asked (turned off, or quiz_eligible=false). Without
 *    filtering, the student is still asked that triplet and their choice falls on a dimension that
 *    {@link InterestQuizScorer#normalize} does not iterate -> the score is SILENTLY DROPPED, the question is
 *    wasted and nobody knows. This is exactly the "silently ineffective" bug that making the dimension
 *    catalog into data was meant to kill.
 *
 * 2. COVER the dimensions evenly within the fixed question budget. The quiz keeps 5-7 questions
 *    (not widened), so when the admin adds a dimension each dimension naturally gets fewer appearances. Greedy selection by
 *    "whichever dimension has the fewest appearances goes first" spends that
 *    budget as evenly as possible, instead of some dimensions being asked 3 times and others 0 times -- a dimension with
 *    0 appearances has a meaningless score.
 */
@Service
@RequiredArgsConstructor
public class InterestQuizItemSelector {

    private final InterestQuizScorer interestQuizScorer;

    public List<InterestQuizSeedItem> select(List<InterestQuizSeedItem> pool, int count) {
        Set<String> eligible = Set.copyOf(interestQuizScorer.quizDimensionCodes());
        List<InterestQuizSeedItem> usable = pool.stream()
            .filter(item -> item.getDimensionPerStatement() != null
                && item.getDimensionPerStatement().size() == 3
                && eligible.containsAll(item.getDimensionPerStatement()))
            .toList();
        if (usable.size() <= count) {
            return usable;
        }

        return selectGreedilyByCoverage(usable, count);
    }

    private List<InterestQuizSeedItem> selectGreedilyByCoverage(List<InterestQuizSeedItem> usable, int count) {
        Map<String, Integer> coverage = new HashMap<String, Integer>();
        List<InterestQuizSeedItem> remaining = new ArrayList<>(usable);
        List<InterestQuizSeedItem> selected = new ArrayList<InterestQuizSeedItem>();
        while (selected.size() < count && !remaining.isEmpty()) {
            InterestQuizSeedItem best = pickItemWithHighestCoverageScore(remaining, coverage);
            remaining.remove(best);
            selected.add(best);
            for (String dimension : best.getDimensionPerStatement()) {
                coverage.merge(dimension, 1, (current, delta) -> current + delta);
            }
        }
        return selected;
    }

    private InterestQuizSeedItem pickItemWithHighestCoverageScore(
            List<InterestQuizSeedItem> remaining, Map<String, Integer> coverage) {
        InterestQuizSeedItem best = remaining.get(0);
        double bestScore = coverageScore(best, coverage);
        for (InterestQuizSeedItem candidate : remaining) {
            double score = coverageScore(candidate, coverage);
            if (score > bestScore) {
                best = candidate;
                bestScore = score;
            }
        }
        return best;
    }

    /** The more dimensions with few appearances, the higher the score. 1/(1+n) decreases so an item covering a dimension
     * that has never appeared always beats an item covering a dimension that already has a few appearances. */
    private static double coverageScore(
            InterestQuizSeedItem item,
            java.util.Map<String, Integer> coverage) {
        double score = 0.0;
        for (String dimension : item.getDimensionPerStatement()) {
            score += 1.0 / (1 + coverage.getOrDefault(dimension, 0));
        }
        return score;
    }
}
