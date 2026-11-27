package com.sep.vox.domain.service.rubric;

import java.util.Set;

public final class RubricOrderValidator {

    private RubricOrderValidator() {}

    /**
     * Shared by RubricCriterion and RubricResultBand, for both batch and single updates.
     * ordersSoFar holds the order of the other items (siblings) in the same RubricVersion — pass an empty/accumulating Set
     * for a batch, or the Set of sibling orders (excluding the item itself) for a single update.
     */
    public static void assertNoDuplicateOrder(Set<Integer> ordersSoFar, int newOrder, String itemName) {
        if (!ordersSoFar.add(newOrder)) {
            throw new IllegalArgumentException(
                    "'" + itemName + "': The order " + newOrder
                            + " is already used by another criterion/band in the same Rubric version.");
        }
    }
}