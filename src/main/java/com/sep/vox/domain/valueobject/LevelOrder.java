package com.sep.vox.domain.valueobject;

public record LevelOrder(int value) {
    public LevelOrder {
        if (value <= 0) {
            throw new IllegalArgumentException("The order of the level must not be less than or equal to 0");
        }
    }
}
