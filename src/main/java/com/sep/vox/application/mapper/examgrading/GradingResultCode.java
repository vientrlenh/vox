package com.sep.vox.application.mapper.examgrading;

import java.util.Locale;
import java.util.UUID;

/**
 * Mã bài hiển thị trên màn chấm. BE không có cột mã riêng cho bài nộp, nên mã là
 * 8 ký tự hex đầu của {@code candidateResultId} viết hoa (vd {@code A2041F3C}).
 *
 * <p>Chỉ để người dùng đọc/tìm kiếm — KHÔNG dùng làm khoá: 8 hex có thể trùng.
 */
public final class GradingResultCode {

    private static final int LENGTH = 8;

    private GradingResultCode() {}

    public static String of(UUID candidateResultId) {
        if (candidateResultId == null) {
            return null;
        }
        return candidateResultId.toString().replace("-", "")
            .substring(0, LENGTH).toUpperCase(Locale.ROOT);
    }
}
