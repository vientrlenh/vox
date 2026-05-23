package com.sep.vox.application.mapper.common;

import java.text.Normalizer;
import java.util.Locale;

public class StringNormalization {
    
    public static String nfcNormalize(String input) {
        return Normalizer.normalize(input, Normalizer.Form.NFC);
    }


    public static String nfkcNormalize(String input) {
        return Normalizer.normalize(input, Normalizer.Form.NFKC);
    }

    public static String nfkdNormalize(String input) {
        return Normalizer.normalize(input, Normalizer.Form.NFKD);
    }

    public static String nfdNormalize(String input) {
        var text = input.strip();
        text = text.toLowerCase(Locale.ROOT);
        text = Normalizer.normalize(text, Normalizer.Form.NFD);
        text = text.replaceAll("\\p{M}", "");
        text = text.replaceAll("\\s+", " ");
        return text;
    }
}
