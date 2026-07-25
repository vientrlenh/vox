package com.sep.vox.application.common;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.sep.vox.domain.model.rubric.RubricResultBand;

public class RubricResultBandValidatorTests {

    private RubricResultBand band(String name, double min, double max) {
        return new RubricResultBand(UUID.randomUUID(), UUID.randomUUID(), name.toUpperCase(), name, null,
                BigDecimal.valueOf(min), BigDecimal.valueOf(max), 1, OffsetDateTime.now(), OffsetDateTime.now(),
                UUID.randomUUID(), UUID.randomUUID());
    }

    @Test
    void emptySiblingList_alwaysPasses() {
        assertDoesNotThrow(() -> RubricResultBandValidator.assertNoOverlap(
                List.of(), BigDecimal.valueOf(0), BigDecimal.valueOf(4), "Yếu"));
    }

    @Test
    void adjacentNonOverlappingRanges_pass() {
        List<RubricResultBand> siblings = List.of(band("Yếu", 0, 3.99));
        assertDoesNotThrow(() -> RubricResultBandValidator.assertNoOverlap(
                siblings, BigDecimal.valueOf(4), BigDecimal.valueOf(6), "Trung bình"));
    }

    @Test
    void rangeContainedInsideExisting_throws() {
        List<RubricResultBand> siblings = List.of(band("Yếu", 0, 4));
        assertThrows(IllegalArgumentException.class, () -> RubricResultBandValidator.assertNoOverlap(
                siblings, BigDecimal.valueOf(0), BigDecimal.valueOf(6), "Trung bình"));
    }

    @Test
    void touchingBoundary_isTreatedAsOverlap_throws() {
        List<RubricResultBand> siblings = List.of(band("Yếu", 0, 4));
        assertThrows(IllegalArgumentException.class, () -> RubricResultBandValidator.assertNoOverlap(
                siblings, BigDecimal.valueOf(4), BigDecimal.valueOf(6), "Trung bình"));
    }

    private NavigableMap<BigDecimal, RubricResultBand> byMin(RubricResultBand... bands) {
        NavigableMap<BigDecimal, RubricResultBand> map = new TreeMap<>();
        for (RubricResultBand b : bands) {
            map.put(b.getScoreMin(), b);
        }
        return map;
    }

    @Test
    void treeMap_emptyMap_alwaysPasses() {
        assertDoesNotThrow(() -> RubricResultBandValidator.assertNoOverlap(
                byMin(), BigDecimal.valueOf(0), BigDecimal.valueOf(4), "Yếu"));
    }

    @Test
    void treeMap_adjacentNonOverlappingRanges_pass() {
        NavigableMap<BigDecimal, RubricResultBand> siblings = byMin(band("Yếu", 0, 3.99));
        assertDoesNotThrow(() -> RubricResultBandValidator.assertNoOverlap(
                siblings, BigDecimal.valueOf(4), BigDecimal.valueOf(6), "Trung bình"));
    }

    @Test
    void treeMap_touchingLeftNeighborBoundary_throws() {
        NavigableMap<BigDecimal, RubricResultBand> siblings = byMin(band("Yếu", 0, 4));
        assertThrows(IllegalArgumentException.class, () -> RubricResultBandValidator.assertNoOverlap(
                siblings, BigDecimal.valueOf(4), BigDecimal.valueOf(6), "Trung bình"));
    }

    @Test
    void treeMap_newRangeSwallowsExistingBand_throws() {
        // Band mới (0-10) nuốt trọn 1 band ở giữa (4-6) dù không đụng floor/ceiling theo scoreMin sát nhất
        NavigableMap<BigDecimal, RubricResultBand> siblings = byMin(
                band("Yếu", 0, 3), band("Khá", 4, 6), band("Giỏi", 7, 10));
        assertThrows(IllegalArgumentException.class, () -> RubricResultBandValidator.assertNoOverlap(
                siblings, BigDecimal.valueOf(0), BigDecimal.valueOf(10), "Toàn thang"));
    }

    @Test
    void treeMap_multipleNonOverlappingBands_allPass() {
        NavigableMap<BigDecimal, RubricResultBand> siblings = byMin(
                band("Yếu", 0, 3.99), band("Khá", 4, 5.99), band("Giỏi", 8, 10));
        assertDoesNotThrow(() -> RubricResultBandValidator.assertNoOverlap(
                siblings, BigDecimal.valueOf(6), BigDecimal.valueOf(7.99), "Trung bình khá"));
    }
}
