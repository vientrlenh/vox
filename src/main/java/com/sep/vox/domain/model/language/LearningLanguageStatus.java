package com.sep.vox.domain.model.language;

public enum LearningLanguageStatus {
    AVAILABLE, 
    UNAVAILABLE; 

    public static LearningLanguageStatus from(String status) {
        return status == null ? null : LearningLanguageStatus.valueOf(status);
    }

    public static String value(LearningLanguageStatus status) {
        return status == null ? null : status.name();
    }
}
