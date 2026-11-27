package com.sep.vox.application.mapper.assessmentgrading;

import java.util.Locale;
import java.util.UUID;

/**
 * The submission code shown on the grading screen. The backend has no dedicated column for a
 * submission code, so the code is the first 8 hex characters of {@code candidateResultId},
 * uppercased (e.g. {@code A2041F3C}).
 *
 * <p>For people to read/search only -- NOT to be used as a key: 8 hex characters can collide.
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
