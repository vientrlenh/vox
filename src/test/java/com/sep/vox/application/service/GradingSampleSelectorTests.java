package com.sep.vox.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.service.GradingSampleSelector;
import com.sep.vox.application.query.dto.GradingRiskInfo;
import com.sep.vox.domain.model.assessment.GradingSampleSelectionMode;

/**
 * Bốn chế độ chọn bài. Bốc ngẫu nhiên được test bằng seed cố định — nếu không thì
 * chính cái tính năng "chọn mẫu hậu kiểm" là thứ duy nhất không ai kiểm được.
 */
class GradingSampleSelectorTests {

    private final GradingSampleSelector selector = new GradingSampleSelector(new Random(2026));

    private List<UUID> ids(int count) {
        return java.util.stream.IntStream.range(0, count)
            .mapToObj(index -> UUID.randomUUID())
            .toList();
    }

    @Test
    void should_take_everything_in_all_mode() {
        List<UUID> candidates = ids(7);

        assertThat(selector.select(GradingSampleSelectionMode.ALL, candidates, null, null, null))
            .containsExactlyElementsOf(candidates);
    }

    @Test
    void should_return_empty_when_there_is_nothing_to_pick() {
        assertThat(selector.select(GradingSampleSelectionMode.ALL, List.of(), null, null, null)).isEmpty();
    }

    @Test
    void should_round_the_random_sample_up() {
        // 10% của 5 bài = 0.5. Làm tròn XUỐNG sẽ trả rỗng và admin tưởng hệ thống hỏng.
        assertThat(selector.select(GradingSampleSelectionMode.RANDOM_PERCENT, ids(5), 10, null, null))
            .hasSize(1);
        assertThat(selector.select(GradingSampleSelectionMode.RANDOM_PERCENT, ids(10), 30, null, null))
            .hasSize(3);
    }

    @Test
    void should_pick_distinct_results_when_sampling() {
        List<UUID> picked = selector.select(GradingSampleSelectionMode.RANDOM_PERCENT, ids(20), 50, null, null);

        assertThat(picked).hasSize(10).doesNotHaveDuplicates();
    }

    @Test
    void should_reject_a_percentage_outside_one_to_hundred() {
        List<UUID> candidates = ids(5);

        assertThatThrownBy(() ->
            selector.select(GradingSampleSelectionMode.RANDOM_PERCENT, candidates, 0, null, null))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() ->
            selector.select(GradingSampleSelectionMode.RANDOM_PERCENT, candidates, 101, null, null))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() ->
            selector.select(GradingSampleSelectionMode.RANDOM_PERCENT, candidates, null, null, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_rank_low_confidence_results_first() {
        UUID confident = UUID.randomUUID();
        UUID shaky = UUID.randomUUID();
        List<GradingRiskInfo> risks = List.of(
            new GradingRiskInfo(confident, new BigDecimal("0.95"), new BigDecimal("8.00"), null),
            new GradingRiskInfo(shaky, new BigDecimal("0.30"), new BigDecimal("8.00"), null));

        List<UUID> picked = selector.select(
            GradingSampleSelectionMode.RISK_BASED, List.of(confident, shaky), 50, null, risks);

        assertThat(picked).containsExactly(shaky);
    }

    @Test
    void should_rank_borderline_scores_first() {
        UUID clear = UUID.randomUUID();
        UUID borderline = UUID.randomUUID();
        // Same confidence; a score right at the pass threshold is where a grading mistake has the
        // heaviest consequences.
        List<GradingRiskInfo> risks = List.of(
            new GradingRiskInfo(clear, new BigDecimal("0.80"), new BigDecimal("8.50"), new BigDecimal("5.00")),
            new GradingRiskInfo(borderline, new BigDecimal("0.80"), new BigDecimal("5.05"), new BigDecimal("5.00")));

        List<UUID> picked = selector.select(
            GradingSampleSelectionMode.RISK_BASED, List.of(clear, borderline), 50, null, risks);

        assertThat(picked).containsExactly(borderline);
    }

    @Test
    void should_keep_risk_ranking_stable_across_calls() {
        List<GradingRiskInfo> risks = List.of(
            new GradingRiskInfo(UUID.randomUUID(), new BigDecimal("0.50"), null, null));
        List<UUID> candidates = ids(4);

        List<UUID> first = selector.select(GradingSampleSelectionMode.RISK_BASED, candidates, null, null, risks);
        List<UUID> second = selector.select(GradingSampleSelectionMode.RISK_BASED, candidates, null, null, risks);

        // With no risk signal it is a tie; a tie must be ordered by id so two calls do not produce
        // two different orders.
        assertThat(first).containsExactlyElementsOf(second);
    }

    @Test
    void should_take_only_the_ids_the_admin_listed() {
        List<UUID> candidates = ids(5);
        List<UUID> chosen = List.of(candidates.get(1), candidates.get(3));

        assertThat(selector.select(GradingSampleSelectionMode.MANUAL_LIST, candidates, null, chosen, null))
            .containsExactlyElementsOf(chosen);
    }

    @Test
    void should_reject_a_manual_id_that_is_not_eligible() {
        List<UUID> candidates = ids(3);

        // A submission outside the pool means it is in the wrong state or someone has already
        // graded it -- skipping it silently would make the admin think the assignment is done.
        assertThatThrownBy(() -> selector.select(
            GradingSampleSelectionMode.MANUAL_LIST, candidates, null, List.of(UUID.randomUUID()), null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("không đủ điều kiện");
    }

    @Test
    void should_reject_an_empty_manual_list() {
        assertThatThrownBy(() ->
            selector.select(GradingSampleSelectionMode.MANUAL_LIST, ids(3), null, List.of(), null))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
