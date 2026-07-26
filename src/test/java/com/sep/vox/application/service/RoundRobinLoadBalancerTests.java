package com.sep.vox.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.service.RoundRobinLoadBalancer;

/**
 * Chia theo TẢI THẬT, không phải chia vòng tròn từ số 0 — đó là toàn bộ lý do lớp này
 * tồn tại, và cũng là thứ dễ viết sai nhất khi ai đó "đơn giản hoá" nó.
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
        var counts = new java.util.HashMap<UUID, Long>();
        picked.forEach(assignee -> counts.merge(assignee, 1L, Long::sum));
        return counts;
    }

    @Test
    void should_split_evenly_when_the_count_divides() {
        var picked = balancer.distribute(work(6), List.of(anh, binh, chi), Map.of());

        assertThat(countByAssignee(picked).values()).containsExactly(2L, 2L, 2L);
    }

    @Test
    void should_give_the_remainder_to_the_first_assignees_in_order() {
        var picked = balancer.distribute(work(7), List.of(anh, binh, chi), Map.of());

        var counts = countByAssignee(picked);
        assertThat(counts.get(anh)).isEqualTo(3L);
        assertThat(counts.get(binh)).isEqualTo(2L);
        assertThat(counts.get(chi)).isEqualTo(2L);
    }

    @Test
    void should_start_from_current_load_instead_of_zero() {
        // Anh đang giữ 5 việc; chạy lần hai mà vẫn chia từ 0 sẽ dồn tiếp vào Anh.
        var picked = balancer.distribute(
            work(3), List.of(anh, binh, chi), Map.of(anh, 5L, binh, 0L, chi, 0L));

        var counts = countByAssignee(picked);
        assertThat(counts).doesNotContainKey(anh);
        assertThat(counts.get(binh)).isEqualTo(2L);
        assertThat(counts.get(chi)).isEqualTo(1L);
    }

    @Test
    void should_treat_a_missing_load_entry_as_zero() {
        var picked = balancer.distribute(work(2), List.of(anh, binh), Map.of(anh, 3L));

        assertThat(countByAssignee(picked).get(binh)).isEqualTo(2L);
    }

    @Test
    void should_return_one_assignee_per_work_item_in_order() {
        var items = work(4);

        var picked = balancer.distribute(items, List.of(anh, binh), Map.of());

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
            .hasMessageContaining("ít nhất một giáo viên");
    }
}
