package com.sep.vox.domain.service.personalization;

public final class SessionDiagnosisPolicy {

    private SessionDiagnosisPolicy() {
    }

    /**
     * Suy luận lý do học sinh bỏ dở phiên luyện tập, dựa trên điểm gần nhất và tín hiệu
     * xin trợ giúp/tạm dừng lâu trong phiên.
     */
    public static String diagnose(
            Double normalizedScore,
            int helpRequestCount,
            int longPauseCount) {
        if (normalizedScore == null) {
            return "UNKNOWN";
        }
        if (normalizedScore >= 0.65
                && helpRequestCount == 0
                && longPauseCount <= 1) {
            return "BORED";
        }
        if (normalizedScore < 0.50
                || helpRequestCount >= 1
                || longPauseCount >= 2) {
            return "TOO_HARD";
        }
        return "UNKNOWN";
    }
}
