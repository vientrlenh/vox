package com.sep.vox.application.response.input.learnerprofile;

import java.util.List;
import java.util.UUID;

public final class LearnerProfileResponses {

    private LearnerProfileResponses() {
    }

    public record LearnerProfile(
        String goalType,
        Double flsaScore,
        String targetFrameworkBandCode,
        String targetFrameworkBandLabel,
                boolean interestAutoUpdateEnabled,
        String quizCompletedAt
    ) {
    }

    public record InterestQuizItem(UUID id, List<String> statements) {
    }

    /**
     * Một bậc để học sinh chọn làm độ khó phiên luyện. Cố tình KHÔNG có cờ "bậc của em":
     * hệ thống không xếp bậc năng lực, nó chỉ ghi lại độ khó em chọn từng phiên.
     */
    public record PracticeBandOption(
        UUID id,
        String code,
        String label,
        String description,
        int order
    ) {
    }
}
