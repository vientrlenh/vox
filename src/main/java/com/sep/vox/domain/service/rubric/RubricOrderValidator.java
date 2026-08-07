package com.sep.vox.domain.service.rubric;

import java.util.Set;

public final class RubricOrderValidator {

    private RubricOrderValidator() {}

    /**
     * Dùng chung cho RubricCriterion và RubricResultBand, cả batch lẫn update đơn lẻ.
     * ordersSoFar chứa order của các item khác (sibling) trong cùng RubricVersion — truyền vào Set rỗng/tích luỹ
     * dần cho batch, hoặc Set order của các sibling (không gồm chính nó) cho update đơn lẻ.
     */
    public static void assertNoDuplicateOrder(Set<Integer> ordersSoFar, int newOrder, String itemName) {
        if (!ordersSoFar.add(newOrder)) {
            throw new IllegalArgumentException(
                    "'" + itemName + "': Thứ tự (order) " + newOrder
                            + " đã được sử dụng bởi một tiêu chí/thang điểm khác trong cùng phiên bản Rubric.");
        }
    }
}