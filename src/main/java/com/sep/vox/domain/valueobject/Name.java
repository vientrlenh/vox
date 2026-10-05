package com.sep.vox.domain.valueobject;

public record Name(
    String value
) {

    public Name {
        if (value != null && !isValidName(value)) {
            throw new IllegalArgumentException("Invalid name format. Each words must begin with a capital letter");
        }
    }

    public static Name from(String name) {
        return name == null ? null : new Name(name);
    }

    public static String valueOf(Name name) {
        return name == null ? null : name.value;
    }

    private static boolean isValidName(String value) {
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
