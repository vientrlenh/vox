package com.sep.vox.domain.valueobject;

public record FullName(String value) {

    public FullName {
        if (value != null && !isValidFullName(value)) {
            throw new IllegalArgumentException("Định dạng tên đầy đủ không hợp lệ");
        }
    }

    private static boolean isValidFullName(String value) {
        String[] words = value.strip().split("\\s+");
        if (words.length == 0) {
            return false;
        }
        for (String word : words) {
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

        int firstCodePoint = word.codePointAt(0);
        if (!Character.isLetter(firstCodePoint) || !Character.isUpperCase(firstCodePoint)) {
            return false;
        }

        for (int i = Character.charCount(firstCodePoint); i < word.length();) {
            int codePoint = word.codePointAt(i);
            if (!Character.isLetter(codePoint)) {
                return false;
            }
            i += Character.charCount(codePoint);
        }
        return true;
    }
}
