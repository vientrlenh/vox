package com.sep.vox.domain.valueobject;

public record LanguageRank(int value) {
    public LanguageRank {
        if (value <= 0) {
            throw new IllegalArgumentException("Bậc ngôn ngữ không được nhỏ hơn hoặc bằng 0");
        }
    }
}
