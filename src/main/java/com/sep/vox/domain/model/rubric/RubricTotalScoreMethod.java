package com.sep.vox.domain.model.rubric;

public enum RubricTotalScoreMethod {
    WEIGHTED_AVERAGE,
    SUM;

    public static RubricTotalScoreMethod from(String value) {
        return value == null ? null : RubricTotalScoreMethod.valueOf(value);
    }

    public static String value(RubricTotalScoreMethod value) {
        return value == null ? null : value.name();
    }
}
