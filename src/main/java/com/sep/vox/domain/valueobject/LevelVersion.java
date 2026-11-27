package com.sep.vox.domain.valueobject;

public record LevelVersion(int value) {
    public LevelVersion {
        if (value <= 0) {
            throw new IllegalArgumentException("The version of the level must not be less than or equal to 0");
        }
    }
}
