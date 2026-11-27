package com.sep.vox.application.query.dto;

import java.util.List;
import java.util.Map;

/**
 * The practice focus of one student: the criteria ranked by weakness, with the most worthwhile
 * sub-attribute to practice for each criterion.
 *
 * Before, this record carried EXACTLY TWO criteria (primary/secondary), so the other three criteria
 * were never asked about -- even though the source query
 * ({@code findFocusCriterionCodesOrderedByWeakness}) already returned the whole ranked list. A student
 * weak in grammar would only ever practice grammar, while pronunciation or coherence would never be
 * touched until it dropped to become one of the two weakest.
 */
public record PracticeFocusInfo(
    /** The criteria in descending order of weakness. Never empty -- see resolveFocus. */
    List<String> orderedCriteria,
    /**
     * The sub-attributes of each criterion, ORDERED by descending priority. A missing key or an empty
     * list means "practice this criterion in general".
     */
    Map<String, List<String>> subAttributesByCriterion
) {

    /**
     * The criterion for slot {@code slotIndex}, in a cycle of 4: weakest, weakest, second weakest, then
     * rotating through the rest.
     *
     * The ratio is 50% for the weakest criterion, 25% for the second weakest, 25% spread evenly over
     * the other criteria -- focused on the weak spot but not framed on exactly one thing. Practicing one
     * criterion forever breaks three things at once: the student gets bored, the other criteria get no new
     * observations so the weakness profile stands still, and the band estimate loses its data source for those criteria.
     *
     * The cycle is FIXED rather than random: the same student in the same slot always gets the same
     * criterion, so bugs are reproducible and "occasionally mixed in" is something that is certain to
     * happen rather than a matter of luck.
     */
    public String criterionForSlot(int slotIndex) {
        int safeIndex = Math.max(0, slotIndex);
        return switch (safeIndex % 4) {
            case 0, 1 -> at(0);
            case 2 -> at(1);
            // The mixed-in slot: rotate through the criteria FROM THE THIRD onward, so that each cycle
            // hits a different criterion instead of repeating the third one forever.
            default -> orderedCriteria.size() <= 2
                ? at(1)
                : at(2 + (safeIndex / 4) % (orderedCriteria.size() - 2));
        };
    }

    /**
     * The sub-attribute for that slot; null means practice the criterion in general.
     *
     * The same principle as {@link #criterionForSlot}, one level deeper: the weakest label takes most of
     * the slots but still rotates to the remaining labels. The previous version used {@code findFirst()},
     * so in GRAMMAR every question targeted exactly one label (e.g. tense_control) while article_use or
     * word_form were never practiced -- and because they were not practiced there were no new
     * observations, so their ranking stood still forever.
     *
     * A cycle of 3: two turns for the weakest label, one turn rotating through the rest (~67/33).
     *
     * Counted by the number of times THIS CRITERION has appeared, not by the raw slotIndex: the
     * mixed-in criterion only lands in slots 3, 7, 11... so with slotIndex % 3 it would always be in the
     * same phase and the secondary label would never get its turn.
     */
    public String subAttributeForSlot(int slotIndex) {
        String criterion = criterionForSlot(slotIndex);
        List<String> candidates = subAttributesByCriterion.get(criterion);
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        int occurrence = occurrenceOfCriterion(criterion, slotIndex);
        if (candidates.size() == 1 || occurrence % 3 != 2) {
            return candidates.get(0);
        }
        return candidates.get(1 + (occurrence / 3) % (candidates.size() - 1));
    }

    /** How many times this criterion has been asked, counting from slot 0. */
    private int occurrenceOfCriterion(String criterion, int slotIndex) {
        int count = 0;
        for (int index = 0; index < slotIndex; index++) {
            if (criterion.equals(criterionForSlot(index))) {
                count++;
            }
        }
        return count;
    }

    private String at(int index) {
        return orderedCriteria.get(Math.min(index, orderedCriteria.size() - 1));
    }
}
