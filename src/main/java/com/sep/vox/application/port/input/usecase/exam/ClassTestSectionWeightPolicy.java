package com.sep.vox.application.port.input.usecase.exam;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import com.sep.vox.application.port.input.command.ClassTestQuestionCommand;
import com.sep.vox.application.port.input.command.ClassTestSectionCommand;
import com.sep.vox.domain.model.exam.ExamPaperSection;

final class ClassTestSectionWeightPolicy {

    private static final BigDecimal WEIGHT_TOLERANCE = new BigDecimal("0.01");

    private ClassTestSectionWeightPolicy() {
    }

    static List<BigDecimal> resolveRequestedWeights(List<ClassTestSectionCommand> sections) {
        var providedCount = sections.stream().filter(section -> section.weight() != null).count();
        if (providedCount == sections.size()) {
            var weights = sections.stream().map(command -> command.weight()).toList();
            validateWeightSum(weights, "Tổng trọng số section phải bằng 1.00");
            return weights;
        }
        return distributeEqualWeights(sections.size());
    }

    // H.6.4: câu hỏi trong section class test PHẢI có weight tường minh (không auto-fill
    // ở backend - nút "Chia trọng số tự động" là tính năng FE, backend chỉ validate).
    static List<BigDecimal> resolveQuestionWeights(List<ClassTestQuestionCommand> questions) {
        var weights = questions.stream().map(command -> command.weight()).toList();
        validateWeightSum(weights, "Tổng trọng số câu hỏi trong phần phải bằng 1.00");
        return weights;
    }

    static void validateWeightSum(List<BigDecimal> weights, String message) {
        var sum = BigDecimal.ZERO;
        for (var weight : weights) {
            if (weight == null) {
                throw new IllegalStateException(message);
            }
            if (weight.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalStateException("Trọng số section không được âm");
            }
            sum = sum.add(weight);
        }
        if (sum.subtract(BigDecimal.ONE).abs().compareTo(WEIGHT_TOLERANCE) > 0) {
            throw new IllegalStateException(message + " (hiện tại " + sum + ")");
        }
    }

    static void validateStoredWeights(List<ExamPaperSection> sections, String message) {
        validateWeightSum(sections.stream().map(section -> section.getWeight()).toList(), message);
    }

    static boolean looksAutoWeighted(List<ExamPaperSection> sections) {
        if (sections.isEmpty() || sections.stream().anyMatch(section -> section.getWeight() == null)) {
            return true;
        }
        var equalWeights = distributeEqualWeights(sections.size());
        for (int i = 0; i < sections.size(); i++) {
            if (sections.get(i).getWeight().subtract(equalWeights.get(i)).abs().compareTo(WEIGHT_TOLERANCE) > 0) {
                return false;
            }
        }
        return true;
    }

    static List<BigDecimal> normalizeStoredWeights(List<ExamPaperSection> sections) {
        var totalWeight = sections.stream()
            .map(section -> section.getWeight() == null ? BigDecimal.ZERO : section.getWeight())
            .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));
        if (totalWeight.compareTo(BigDecimal.ZERO) <= 0) {
            return distributeEqualWeights(sections.size());
        }

        var weights = new ArrayList<BigDecimal>();
        var runningSum = BigDecimal.ZERO;
        for (int i = 0; i < sections.size() - 1; i++) {
            var normalized = (sections.get(i).getWeight() == null ? BigDecimal.ZERO : sections.get(i).getWeight())
                .divide(totalWeight, 2, RoundingMode.HALF_UP);
            weights.add(normalized);
            runningSum = runningSum.add(normalized);
        }
        weights.add(BigDecimal.ONE.subtract(runningSum));
        return weights;
    }

    static List<BigDecimal> distributeEqualWeights(int count) {
        if (count <= 0) {
            return List.of();
        }
        var weights = new ArrayList<BigDecimal>();
        var perItem = BigDecimal.ONE.divide(BigDecimal.valueOf(count), 2, RoundingMode.DOWN);
        var runningSum = BigDecimal.ZERO;
        for (int i = 0; i < count - 1; i++) {
            weights.add(perItem);
            runningSum = runningSum.add(perItem);
        }
        weights.add(BigDecimal.ONE.subtract(runningSum));
        return weights;
    }
}
