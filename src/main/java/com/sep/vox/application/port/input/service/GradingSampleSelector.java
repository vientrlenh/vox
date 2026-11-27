package com.sep.vox.application.port.input.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.sep.vox.application.query.dto.GradingRiskInfo;
import com.sep.vox.domain.model.assessment.GradingSampleSelectionMode;

/**
 * Picks the set of assessments to hand out automatically for a grading round.
 *
 * <p>Split out of the use case because this is the <em>only</em> part with its own rules worth testing carefully:
 * random draws must be repeatable given a seed, and risk ranking must be explainable
 * to the school ("why was this assessment picked for post-review").
 */
@Service
public class GradingSampleSelector {

    /** Half the score band around the passing threshold counts as "near the threshold". */
    private static final BigDecimal BORDERLINE_BAND = new BigDecimal("0.50");

    private final Random random;

    public GradingSampleSelector() {
        this(new Random());
    }

    /** Lets a test inject a fixed seed — random draws must be verifiable. */
    public GradingSampleSelector(Random random) {
        this.random = random;
    }

    /**
     * @param candidates   every eligible assessment, with no open assignment yet
     * @param percent      only used for {@code RANDOM_PERCENT}; 1..100
     * @param manualIds    only used for {@code MANUAL_LIST}; must be a subset of
     *                     {@code candidates} — an unknown id is rejected rather than silently ignored
     * @param riskInfos    only used for {@code RISK_BASED}
     */
    public List<UUID> select(GradingSampleSelectionMode mode, List<UUID> candidates, Integer percent,
            List<UUID> manualIds, List<GradingRiskInfo> riskInfos) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        return switch (mode) {
            case ALL -> List.copyOf(candidates);
            case RANDOM_PERCENT -> randomPercent(candidates, percent);
            case RISK_BASED -> riskBased(candidates, percent, riskInfos);
            case MANUAL_LIST -> manualList(candidates, manualIds);
        };
    }

    /**
     * Draws {@code percent}% of the assessments, rounded UP: choosing 10% of 5 assessments must give 1,
     * not 0 — a post-review that returns empty would make the admin think the system is broken.
     */
    private List<UUID> randomPercent(List<UUID> candidates, Integer percent) {
        int size = sampleSize(candidates.size(), percent);
        List<UUID> shuffled = new ArrayList<>(candidates);
        java.util.Collections.shuffle(shuffled, random);
        return List.copyOf(shuffled.subList(0, size));
    }

    /**
     * Ranks by risk and takes the top part. Without {@code percent} it takes everything in descending
     * risk order — the admin still sees the suspicious ones on top.
     *
     * <p>Two signals, added together:
     * <ul>
     *   <li>the AI is not confident — {@code 1 - minConfidence}, treated as neutral when data is missing;
     *   <li>a score near the passing threshold — within the {@value #BORDERLINE_BAND} band around
     *       {@code passingScore} an extra amount is added, because this is where a wrong grade has the heaviest consequences.
     * </ul>
     */
    private List<UUID> riskBased(List<UUID> candidates, Integer percent, List<GradingRiskInfo> riskInfos) {
        Map<UUID, GradingRiskInfo> infoById = riskInfos == null ? java.util.Map.<UUID, GradingRiskInfo>of()
            : riskInfos.stream().collect(Collectors.toMap(
                riskInfo -> riskInfo.candidateResultId(), Function.identity(), (left, right) -> left));

        List<UUID> ranked = new ArrayList<>(candidates);
        ranked.sort(Comparator
            .comparing((UUID id) -> riskScore(infoById.get(id))).reversed()
            // On a tie order by id so the result is stable between two calls.
            .thenComparing(Function.identity()));

        int size = percent == null ? ranked.size() : sampleSize(ranked.size(), percent);
        return List.copyOf(ranked.subList(0, size));
    }

    /** Risk score in [0, 2]; higher = more worth a look. */
    private BigDecimal riskScore(GradingRiskInfo info) {
        if (info == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal score = BigDecimal.ZERO;
        if (info.minConfidence() != null) {
            score = score.add(BigDecimal.ONE.subtract(info.minConfidence()));
        }
        if (info.passingScore() != null && info.totalScore() != null) {
            BigDecimal distance = info.totalScore().subtract(info.passingScore()).abs();
            if (distance.compareTo(BORDERLINE_BAND) <= 0) {
                // The closer to the threshold the heavier: exactly at the threshold = 1.0, edge of the band = 0.
                score = score.add(BigDecimal.ONE.subtract(
                    distance.divide(BORDERLINE_BAND, 4, RoundingMode.HALF_UP)));
            }
        }
        return score;
    }

    private List<UUID> manualList(List<UUID> candidates, List<UUID> manualIds) {
        if (manualIds == null || manualIds.isEmpty()) {
            throw new IllegalArgumentException("At least one assessment must be chosen for assignment.");
        }
        Set<UUID> allowed = new LinkedHashSet<>(candidates);
        Set<UUID> picked = new LinkedHashSet<UUID>();
        for (UUID id : manualIds) {
            if (!allowed.contains(id)) {
                throw new IllegalArgumentException(
                    "Some assessment is not eligible for this grading round or is already assigned.");
            }
            picked.add(id);
        }
        return List.copyOf(picked);
    }

    private int sampleSize(int total, Integer percent) {
        if (percent == null || percent < 1 || percent > 100) {
            throw new IllegalArgumentException("The sampling percentage must be within the range 1 - 100.");
        }
        return Math.max(1, (int) Math.ceil(total * percent / 100.0));
    }
}
