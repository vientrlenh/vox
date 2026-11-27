package com.sep.vox.domain.valueobject;

import java.time.LocalDate;

public record DateOfBirth(
    LocalDate value
) {
    public DateOfBirth {
        if (value != null) {
            final int ageThresHold = 14;
            final LocalDate now = LocalDate.now();
            final int age = now.getYear() - value.getYear();
            if (age < ageThresHold) {
                throw new IllegalArgumentException("You are not old enough to register");
            }
        }
    }
}
