package com.sep.vox.application.port.input.service;

import java.util.HashMap;
import java.util.List;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.sep.vox.application.port.output.InterestQuizGenerationPort;
import com.sep.vox.application.port.output.PracticeGenerationExecutorPort;
import com.sep.vox.domain.model.personalization.InterestQuizSeedItem;
import com.sep.vox.domain.repository.InterestQuizItemRepository;

/**
 * Tops up the interest-quiz bank when a dimension is not covered enough.
 *
 * Why needed: adding a new dimension to the catalog does NOT make it appear in the quiz by itself -- every
 * triplet in the bank is already tied to the 3 old dimensions. Without generating more, the new dimension is never
 * asked about, so its score is always empty: exactly the "added but silently ineffective" kind of bug that making the catalog
 * into data was meant to avoid.
 *
 * Runs in the background (through PracticeGenerationExecutorPort) because the LLM call is slow; the admin does not
 * have to wait after clicking to create a new dimension.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InterestQuizBankTopUpService {

    /** Minimum number of appearances for each dimension in the shared bank. Below this threshold
     * the balancing selector has nothing to choose from, and a new dimension rarely makes it into the 7 questions asked. */
    private static final int MIN_COVERAGE_PER_DIMENSION = 3;
    private static final int MAX_ITEMS_PER_RUN = 7;

    private final InterestQuizItemRepository quizItemRepository;
    private final InterestQuizGenerationPort quizGenerationPort;
    private final InterestQuizScorer interestQuizScorer;
    private final PracticeGenerationExecutorPort practiceGenerationExecutorPort;

    public void topUpAsync() {
        practiceGenerationExecutorPort.runAsync(this::topUpSwallowingFailure);
    }

    private void topUpSwallowingFailure() {
        try {
            topUp();
        } catch (RuntimeException exception) {
            // Swallowing the error is deliberate: this is a background job that enriches the bank, and if it fails the quiz still runs
            // on the old items. It must not break the admin's dimension-creation flow.
            log.warn("Topping up the interest quiz bank failed", exception);
        }
    }

    public int topUp() {
        List<String> dimensions = interestQuizScorer.quizDimensionCodes();
        if (dimensions.isEmpty()) {
            return 0;
        }
        List<InterestQuizSeedItem> pool = quizItemRepository.findAllActiveQuizItems();
        List<String> underCovered = findUnderCoveredDimensions(dimensions, pool);
        if (underCovered.isEmpty()) {
            return 0;
        }

        log.info(
            "Interest quiz bank lacks coverage for {} -- generating more",
            underCovered
        );
        List<InterestQuizSeedItem> usable = generateUsableItems(pool, dimensions, underCovered);
        if (usable.isEmpty()) {
            return 0;
        }
        saveShared(usable);
        return usable.size();
    }

    private List<String> findUnderCoveredDimensions(List<String> dimensions, List<InterestQuizSeedItem> pool) {
        Map<String, Integer> coverage = new HashMap<String, Integer>();
        for (InterestQuizSeedItem item : pool) {
            for (String dimension : item.getDimensionPerStatement()) {
                coverage.merge(dimension, 1, (current, delta) -> current + delta);
            }
        }
        return dimensions.stream()
            .filter(dimension -> coverage.getOrDefault(dimension, 0) < MIN_COVERAGE_PER_DIMENSION)
            .toList();
    }

    private List<InterestQuizSeedItem> generateUsableItems(
            List<InterestQuizSeedItem> pool, List<String> dimensions, List<String> underCovered) {
        List<String> existingStatements = pool.stream()
            .flatMap(item -> item.getStatements().stream())
            .toList();
        // Still send the WHOLE catalog, not only the under-covered dimensions: every triplet must use 3
        // DIFFERENT dimensions, so passing just one missing dimension could not build a single triplet.
        List<InterestQuizSeedItem> generated = quizGenerationPort.generate(
            MAX_ITEMS_PER_RUN,
            existingStatements,
            dimensions
        );
        return generated.stream()
            .filter(item -> item.getDimensionPerStatement().stream().anyMatch(underCovered::contains))
            .toList();
    }

    /** Saved to the SHARED bank (student_id = null), not attached to any student. */
    private void saveShared(List<InterestQuizSeedItem> items) {
        quizItemRepository.saveGeneratedForStudent(null, items);
    }
}
