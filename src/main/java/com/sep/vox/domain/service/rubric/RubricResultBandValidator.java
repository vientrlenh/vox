package com.sep.vox.domain.service.rubric;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;

import com.sep.vox.domain.model.rubric.RubricResultBand;

public final class RubricResultBandValidator {

    private RubricResultBandValidator() {}

    /**
     * Dùng cho check đơn lẻ (VD: sửa 1 band, so với danh sách sibling lấy 1 lần từ DB) — O(n).
     * Chạm biên cũng tính là overlap (VD: 0-4 và 4-6 bị chặn vì điểm 4 rơi vào cả 2 dải).
     */
    public static void assertNoOverlap(List<RubricResultBand> siblingBands, BigDecimal newMin, BigDecimal newMax,
            String newBandName) {
        for (RubricResultBand sibling : siblingBands) {
            if (overlaps(newMin, newMax, sibling)) {
                throwOverlap(newBandName, newMin, newMax, sibling);
            }
        }
    }

    /**
     * Dùng cho batch (tạo nhiều band cùng lúc / import file): siblingsByMin là map các band đã có,
     * key = scoreMin, PHẢI luôn giữ bất biến "không overlap lẫn nhau" giữa các band trong map.
     * Với bất biến đó, band mới chỉ có thể đụng độ với đúng 2 ứng viên: band liền trước (floorEntry)
     * và band liền sau/trùng (ceilingEntry) theo scoreMin — nên chỉ cần 2 lượt tra O(log n) thay vì
     * quét tuần tự O(n) toàn bộ danh sách. Gọi lại nhiều lần trong 1 batch n band cho tổng độ phức tạp
     * O(n log n) thay vì O(n^2) như quét List.
     */
    public static void assertNoOverlap(NavigableMap<BigDecimal, RubricResultBand> siblingsByMin, BigDecimal newMin,
            BigDecimal newMax, String newBandName) {
        Map.Entry<BigDecimal, RubricResultBand> floor = siblingsByMin.floorEntry(newMin);
        if (floor != null && overlaps(newMin, newMax, floor.getValue())) {
            throwOverlap(newBandName, newMin, newMax, floor.getValue());
        }

        Map.Entry<BigDecimal, RubricResultBand> ceiling = siblingsByMin.ceilingEntry(newMin);
        if (ceiling != null && ceiling.getKey().compareTo(newMax) <= 0) {
            throwOverlap(newBandName, newMin, newMax, ceiling.getValue());
        }
    }

    private static boolean overlaps(BigDecimal newMin, BigDecimal newMax, RubricResultBand sibling) {
        return newMin.compareTo(sibling.getScoreMax()) <= 0 && newMax.compareTo(sibling.getScoreMin()) >= 0;
    }

    private static void throwOverlap(String newBandName, BigDecimal newMin, BigDecimal newMax, RubricResultBand sibling) {
        throw new IllegalArgumentException(
                "Thang điểm '" + newBandName + "' (" + newMin + " - " + newMax
                        + ") bị chồng lấn (kể cả chạm biên) với thang điểm '" + sibling.getName() + "' ("
                        + sibling.getScoreMin() + " - " + sibling.getScoreMax() + ").");
    }
}