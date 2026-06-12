package com.sep.vox.domain.valueobject;

import java.time.LocalDate;

public record DateOfBirth(
    LocalDate value
) {
    public DateOfBirth {
        if (value != null) {
            final var ageThresHold = 14;
            final var now = LocalDate.now();
            final var age = now.getYear() - value.getYear();
            if (age < ageThresHold) {
                throw new IllegalArgumentException("Bạn chưa đủ tuổi để thực hiện đăng ký");
            }
        }
    }
}
