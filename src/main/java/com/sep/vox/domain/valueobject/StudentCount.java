package com.sep.vox.domain.valueobject;

public record StudentCount(int value) {

    public StudentCount {
        if (value <= 0) {
            throw new IllegalArgumentException("The number of students must be greater than 0");
        }
    }
}
