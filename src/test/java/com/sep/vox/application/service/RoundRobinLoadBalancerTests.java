package com.sep.vox.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.service.RoundRobinLoadBalancer;

/**
 * Distributes by REAL LOAD, not round-robin from zero -- that is the whole reason this class
 * exists, and also the thing most easily written wrong when someone "simplifies" it.
 */
class RoundRobinLoadBalancerTests {

    private final RoundRobinLoadBalancer balancer = new RoundRobinLoadBalancer();

    private final UUID anh = UUID.randomUUID();
    private final UUID binh = UUID.randomUUID();
    private final UUID chi = UUID.randomUUID();

    private List<UUID> work(int count) {
        return java.util.stream.IntStream.range(0, count)
            .mapToObj(index -> UUID.randomUUID())
            .toList();
    }

    private Map<UUID, Long> countByAssignee(List<UUID> picked) {
        Map<UUID, Long> counts = new java.util.HashMap<UUID, Long>();
        for (UUID assignee : picked) {
            Long current = counts.get(assignee);
            counts.put(assignee, current == null ? 1L : current + 1L);
        }
        return counts;
    }

    @Test
    void should_split_evenly_when_the_count_divides() {
        List<UUID> picked = balancer.distribute(work(6), List.of(anh, binh, chi), Map.of());

        assertThat(countByAssignee(picked).values()).containsExactly(2L, 2L, 2L);
    }

    @Test
    void should_give_the_remainder_to_the_first_assignees_in_order() {
        List<UUID> picked = balancer.distribute(work(7), List.of(anh, binh, chi), Map.of());

        Map<UUID, Long> counts = countByAssignee(picked);
        assertThat(counts.get(anh)).isEqualTo(3L);
        assertThat(counts.get(binh)).isEqualTo(2L);
        assertThat(counts.get(chi)).isEqualTo(2L);
    }

    @Test
    void should_start_from_current_load_instead_of_zero() {
        // Anh is holding 5 jobs; if the second run still started from 0 it would pile onto Anh
        // again.
        List<UUID> picked = balancer.distribute(
            work(3), List.of(anh, binh, chi), Map.of(anh, 5L, binh, 0L, chi, 0L));

        Map<UUID, Long> counts = countByAssignee(picked);
        assertThat(counts).doesNotContainKey(anh);
        assertThat(counts.get(binh)).isEqualTo(2L);
        assertThat(counts.get(chi)).isEqualTo(1L);
    }

    @Test
    void should_treat_a_missing_load_entry_as_zero() {
        List<UUID> picked = balancer.distribute(work(2), List.of(anh, binh), Map.of(anh, 3L));

        assertThat(countByAssignee(picked).get(binh)).isEqualTo(2L);
    }

    @Test
    void should_return_one_assignee_per_work_item_in_order() {
        List<UUID> items = work(4);

        List<UUID> picked = balancer.distribute(items, List.of(anh, binh), Map.of());

        assertThat(picked).hasSize(items.size());
    }

    @Test
    void should_return_empty_when_there_is_no_work() {
        assertThat(balancer.distribute(List.of(), List.of(anh), Map.of())).isEmpty();
    }

    @Test
    void should_reject_an_empty_assignee_group() {
        assertThatThrownBy(() -> balancer.distribute(work(1), List.of(), Map.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("At least one teacher");
    }
}
