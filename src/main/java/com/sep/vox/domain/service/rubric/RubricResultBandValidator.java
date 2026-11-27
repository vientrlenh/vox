package com.sep.vox.domain.service.rubric;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;

import com.sep.vox.domain.model.rubric.RubricResultBand;

public final class RubricResultBandValidator {

    private RubricResultBandValidator() {}

    /**
     * For single checks (e.g. editing 1 band, against the list of siblings fetched once from the DB) — O(n).
     * Touching at the boundary also counts as overlap (e.g. 0-4 and 4-6 are blocked because score 4 falls in both ranges).
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
     * For batches (creating many bands at once / file import): siblingsByMin is a map of the existing bands,
     * key = scoreMin, MUST always keep the invariant "no mutual overlap" between the bands in the map.
     * With that invariant, a new band can only collide with exactly 2 candidates: the band just before (floorEntry)
     * and the band just after/equal (ceilingEntry) by scoreMin — so only 2 O(log n) lookups are needed instead of
     * a sequential O(n) scan of the whole list. Called repeatedly within a batch of n bands the total complexity is
     * O(n log n) instead of O(n^2) as with scanning a List.
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
                "Band '" + newBandName + "' (" + newMin + " - " + newMax
                        + ") overlaps (including touching at the boundary) with band '" + sibling.getName() + "' ("
                        + sibling.getScoreMin() + " - " + sibling.getScoreMax() + ").");
    }
}