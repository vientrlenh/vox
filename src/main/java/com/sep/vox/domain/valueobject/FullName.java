package com.sep.vox.domain.valueobject;

public record FullName(String value) {

    public FullName {
        if (value != null && !isValidFullName(value)) {
            throw new IllegalArgumentException("Định dạng tên đầy đủ không hợp lệ");
        }
    }

    private static boolean isValidFullName(String value) {
        var words = value.strip().split("\\s+");
        if (words.length == 0) {
            return false;
        }
        for (var word : words) {
            if (!isValidNameWord(word)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isValidNameWord(String word) {
        if (word.isBlank()) {
            return false;
        }

        var firstCodePoint = word.codePointAt(0);
        if (!Character.isLetter(firstCodePoint) || !Character.isUpperCase(firstCodePoint)) {
            return false;
        }

        for (var i = Character.charCount(firstCodePoint); i < word.length();) {
            var codePoint = word.codePointAt(i);
            if (!Character.isLetter(codePoint)) {
                return false;
            }
            i += Character.charCount(codePoint);
        }
        return true;
    }
}
