package com.sep.vox.application.response.input.learnerprofile;

import java.util.List;
import java.util.UUID;

public final class LearnerProfileResponses {

    private LearnerProfileResponses() {
    }

    public record LearnerProfile(
        String goalType,
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

    /**
     * Một khung đánh giá để học sinh chọn TRƯỚC khi chọn bậc.
     *
     * <p>{@code versionId} là bản đã ban hành mới nhất của khung, do server tự chọn -- học sinh
     * chỉ thấy tên khung, không phải hiểu khái niệm "phiên bản". Bậc chọn sau đó thuộc đúng bản
     * này, và vì chấm bài luyện suy framework từ chính bậc đích
     * ({@code SpringDataPracticeSessionRepository.findCriteriaFrameworks}), cả chuỗi tự nhất quán.
     */
    public record PracticeFrameworkOption(
        UUID versionId,
        String code,
        String name,
        String description,
        int bandCount
    ) {
    }
}
