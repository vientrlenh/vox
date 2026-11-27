package com.sep.vox.application.port.input.service;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.assessmentblueprint.CreateAssessmentBlueprintSectionCommand;
import com.sep.vox.application.port.input.command.assessmentblueprint.CreateAssessmentBlueprintSlotCommand;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintSlotType;
import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.domain.repository.QuestionRepository;

/**
 * Checks the sections and slots submitted with a blueprint version: unique orders, at least one slot per section,
 * slot content that matches the slot type, and weights that add up to 1.00 within a tolerance.
 */
@Service
@RequiredArgsConstructor
public class AssessmentBlueprintStructureValidator {

    private static final BigDecimal WEIGHT_TOLERANCE = new BigDecimal("0.01");

    private final QuestionRepository questionRepository;

    public void validateSections(List<CreateAssessmentBlueprintSectionCommand> sections) {
        if (sections.isEmpty()) {
            throw new IllegalArgumentException("A blueprint version must have at least one section");
        }
        Set<Integer> sectionOrders = new HashSet<Integer>();
        BigDecimal sectionWeightSum = BigDecimal.ZERO;
        for (CreateAssessmentBlueprintSectionCommand section : sections) {
            if (!sectionOrders.add(section.order())) {
                throw new IllegalArgumentException("Section order must not be duplicated");
            }
            validateSectionSlots(section);
            sectionWeightSum = sectionWeightSum.add(defaultWeight(section.sectionWeight()));
        }
        if (sectionWeightSum.subtract(BigDecimal.ONE).abs().compareTo(WEIGHT_TOLERANCE) > 0) {
            throw new IllegalArgumentException("The total section weight must equal 1.00");
        }
    }

    private void validateSectionSlots(CreateAssessmentBlueprintSectionCommand section) {
        List<CreateAssessmentBlueprintSlotCommand> slots = section.slots() == null ? List.of() : section.slots();
        if (slots.isEmpty()) {
            throw new IllegalArgumentException("Each section must have at least one slot");
        }
        Set<Integer> slotOrders = new HashSet<Integer>();
        BigDecimal slotWeightSum = BigDecimal.ZERO;
        for (CreateAssessmentBlueprintSlotCommand slot : slots) {
            if (!slotOrders.add(slot.order())) {
                throw new IllegalArgumentException("Slot order within a section must not be duplicated");
            }
            validateSlot(slot);
            slotWeightSum = slotWeightSum.add(defaultWeight(slot.weight()));
        }
        if (slotWeightSum.subtract(BigDecimal.ONE).abs().compareTo(WEIGHT_TOLERANCE) > 0) {
            throw new IllegalArgumentException(
                "The total weight of the question slots in section \"" + section.title() + "\" must equal 1.00");
        }
    }

    private void validateSlot(CreateAssessmentBlueprintSlotCommand slot) {
        if (slot.slotType() == AssessmentBlueprintSlotType.FIXED) {
            validateFixedSlot(slot);
            return;
        }
        if (slot.fixedQuestionId() != null) {
            throw new IllegalArgumentException("A SELECTION slot must not have fixedQuestionId");
        }
        if (slot.selectionSpec() == null) {
            throw new IllegalArgumentException("A SELECTION slot must have selectionSpec");
        }
    }

    private void validateFixedSlot(CreateAssessmentBlueprintSlotCommand slot) {
        if (slot.fixedQuestionId() == null) {
            throw new IllegalArgumentException("A FIXED slot must have fixedQuestionId");
        }
        if (slot.selectionSpec() != null) {
            throw new IllegalArgumentException("A FIXED slot must not have selectionSpec");
        }
        Question question = questionRepository.findById(slot.fixedQuestionId())
            .orElseThrow(() -> new ResourceNotFoundException("Fixed question for the slot not found"));
        if (question.getStatus() != QuestionStatus.PUBLISHED) {
            throw new IllegalArgumentException("The fixed question for a slot must be in PUBLISHED status");
        }
    }

    private BigDecimal defaultWeight(BigDecimal value) {
        return value == null ? BigDecimal.ONE : value;
    }
}
