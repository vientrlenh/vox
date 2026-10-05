package com.sep.vox.domain.valueobject;

import java.time.LocalDate;

public record BirthDate(
    LocalDate value
) {
    public BirthDate {
        if (value != null) {
            final int ageThresHold = 14;
            final LocalDate now = LocalDate.now();
            final int age = now.getYear() - value.getYear();
            if (age < ageThresHold) {
                throw new IllegalArgumentException("Bạn chưa đủ tuổi để thực hiện đăng ký");
            }
        }
    }

    public static BirthDate from(LocalDate birthDate) {
        return birthDate == null ? null : new BirthDate(birthDate);
    }

    public static LocalDate valueOf(BirthDate birthDate) {
        return birthDate == null ? null : birthDate.value;
    }
}
