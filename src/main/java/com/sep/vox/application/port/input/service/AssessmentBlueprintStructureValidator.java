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
            throw new IllegalArgumentException("Blueprint version phải có ít nhất một section");
        }
        Set<Integer> sectionOrders = new HashSet<Integer>();
        BigDecimal sectionWeightSum = BigDecimal.ZERO;
        for (CreateAssessmentBlueprintSectionCommand section : sections) {
            if (!sectionOrders.add(section.order())) {
                throw new IllegalArgumentException("Thứ tự section không được trùng lặp");
            }
            validateSectionSlots(section);
            sectionWeightSum = sectionWeightSum.add(defaultWeight(section.sectionWeight()));
        }
        if (sectionWeightSum.subtract(BigDecimal.ONE).abs().compareTo(WEIGHT_TOLERANCE) > 0) {
            throw new IllegalArgumentException("Tổng trọng số section phải bằng 1.00");
        }
    }

    private void validateSectionSlots(CreateAssessmentBlueprintSectionCommand section) {
        List<CreateAssessmentBlueprintSlotCommand> slots = section.slots() == null ? List.of() : section.slots();
        if (slots.isEmpty()) {
            throw new IllegalArgumentException("Mỗi section phải có ít nhất một slot");
        }
        Set<Integer> slotOrders = new HashSet<Integer>();
        BigDecimal slotWeightSum = BigDecimal.ZERO;
        for (CreateAssessmentBlueprintSlotCommand slot : slots) {
            if (!slotOrders.add(slot.order())) {
                throw new IllegalArgumentException("Thứ tự slot trong section không được trùng lặp");
            }
            validateSlot(slot);
            slotWeightSum = slotWeightSum.add(defaultWeight(slot.weight()));
        }
        if (slotWeightSum.subtract(BigDecimal.ONE).abs().compareTo(WEIGHT_TOLERANCE) > 0) {
            throw new IllegalArgumentException(
                "Tổng trọng số ô câu hỏi trong phần \"" + section.title() + "\" phải bằng 1.00");
        }
    }

    private void validateSlot(CreateAssessmentBlueprintSlotCommand slot) {
        if (slot.slotType() == AssessmentBlueprintSlotType.FIXED) {
            validateFixedSlot(slot);
            return;
        }
        if (slot.fixedQuestionId() != null) {
            throw new IllegalArgumentException("Slot SELECTION không được có fixedQuestionId");
        }
        if (slot.selectionSpec() == null) {
            throw new IllegalArgumentException("Slot SELECTION bắt buộc phải có selectionSpec");
        }
    }

    private void validateFixedSlot(CreateAssessmentBlueprintSlotCommand slot) {
        if (slot.fixedQuestionId() == null) {
            throw new IllegalArgumentException("Slot FIXED bắt buộc phải có fixedQuestionId");
        }
        if (slot.selectionSpec() != null) {
            throw new IllegalArgumentException("Slot FIXED không được có selectionSpec");
        }
        Question question = questionRepository.findById(slot.fixedQuestionId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu hỏi fixed cho slot"));
        if (question.getStatus() != QuestionStatus.PUBLISHED) {
            throw new IllegalArgumentException("Câu hỏi fixed cho slot phải ở trạng thái PUBLISHED");
        }
    }

    private BigDecimal defaultWeight(BigDecimal value) {
        return value == null ? BigDecimal.ONE : value;
    }
}
