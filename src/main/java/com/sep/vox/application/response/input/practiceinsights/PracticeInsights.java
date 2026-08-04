package com.sep.vox.application.response.input.practiceinsights;

import java.util.List;
import java.util.UUID;

public final class PracticeInsights {

    private PracticeInsights() {
    }

    public record WeaknessProfile(
        List<CriterionWeakness> criteria,
        List<SubAttributeWeakness> subAttributes,
        int sessionsAnalysed,
        int nearlyFixed,
        int newlyFound
    ) {
    }

    public record CriterionWeakness(
        String criterionCode,
        String criterionName,
        double weakness,
        int observationCount,
        boolean reliable
    ) {
    }

    public record SubAttributeWeakness(
        String criterionCode,
        String subAttribute,
        int occurrenceCount,
        String severity,
        boolean practiceable,
        /** Đổi nhịp so với cửa sổ trước, %. null = mẫu quá nhỏ hoặc lỗi mới xuất hiện. */
        Double trendPercent,
        /** Từng lặp lại nhưng gần đây không còn -- đang trên đà khỏi. */
        boolean nearlyFixed,
        /** Mọi lần xuất hiện đều trong cửa sổ gần đây -- lỗi mới lộ ra. */
        boolean newlyFound,
        /** Bằng chứng thật kèm số lần: "read x3". Rỗng với nhãn suy từ số đo. */
        List<WeaknessExample> examples
    ) {
    }

    public record WeaknessExample(String text, int times) {
    }

    public record CriterionProgressPoint(
        String criterionCode,
        String date,
        double value,
        String source
    ) {
    }

    public record ClassPracticeOverview(
        UUID classId,
        int studentCount,
        int activeStudentCount,
        List<ClassPracticeRow> rows
    ) {
    }

    public record ClassPracticeRow(
        UUID studentId,
        String fullName,
        int sessionCount,
        int totalPracticeSeconds,
        String lastPracticedAt,
        Double averageScore,
        String weakestCriterionCode
    ) {
    }
}
