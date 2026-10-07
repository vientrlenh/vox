package com.sep.vox.domain.model.language;

public enum LearningLanguage {
    ENGLISH; 

    public static LearningLanguage from(String language) {
        return language == null ? null : LearningLanguage.valueOf(language);
    }

    public static String value(LearningLanguage language) {
        return language == null ? null : language.name();
    }
}
