package com.sep.vox.domain.model.user;

public enum Gender {
    MALE,
    FEMALE;

    public static Gender from(String gender) {
        return gender == null ? null : Gender.valueOf(gender);
    }

    public static String value(Gender gender) {
        return gender == null ? null : gender.name();
    }
}
