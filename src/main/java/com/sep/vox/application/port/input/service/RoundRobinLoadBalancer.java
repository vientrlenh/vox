package com.sep.vox.application.port.input.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

/**
 * Distributes a set of jobs among a group of people by their <em>real load</em>, not round-robin
 * from zero.
 *
 * <p>The starting point is the number of jobs each person already holds, so a second run does not pile everything
 * onto the first person in the list -- that is the whole reason this class exists and the part of the
 * old extension most worth keeping.
 */
@Service
public class RoundRobinLoadBalancer {

    /**
     * Returns the teacher chosen for each job, in the same order the jobs were passed in.
     *
     * @param currentLoads the current loads; anyone missing counts as 0
     * @throws IllegalArgumentException when there is nobody to distribute to
     */
    public List<UUID> distribute(List<UUID> workItems, List<UUID> assigneeIds, Map<UUID, Long> currentLoads) {
        if (assigneeIds == null || assigneeIds.isEmpty()) {
            throw new IllegalArgumentException("At least one teacher must be selected for assignment.");
        }
        if (workItems == null || workItems.isEmpty()) {
            return List.of();
        }

        long[] loads = new long[assigneeIds.size()];
        for (int index = 0; index < assigneeIds.size(); index++) {
            loads[index] = currentLoads == null ? 0L : currentLoads.getOrDefault(assigneeIds.get(index), 0L);
        }

        List<UUID> picked = new ArrayList<UUID>(workItems.size());
        for (int item = 0; item < workItems.size(); item++) {
            int target = leastLoadedIndex(loads);
            loads[target]++;
            picked.add(assigneeIds.get(target));
        }
        return picked;
    }

    /** On a tie the earlier person wins — the leftover jobs are spread in sequence in the chosen order. */
    private int leastLoadedIndex(long[] loads) {
        int best = 0;
        for (int index = 1; index < loads.length; index++) {
            if (loads[index] < loads[best]) {
                best = index;
            }
        }
        return best;
    }
}
