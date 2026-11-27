package com.sep.vox.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Comments and string literals in Java sources are English. A file that still holds Vietnamese because the Vietnamese
 * is data (seed data, import aliases, test fixtures) is named in {@code language-keep-list.txt}, which may only shrink.
 *
 * <p>Why scan source text: the check needs the comment text, which the compiler throws away. Char literals are skipped on purpose: the letter that Unicode normalisation cannot decompose is
 * a char literal in search code. This file spells its Vietnamese snippets with Unicode escapes so it is not on the list.
 */
class LanguageConventionTests {

    private static final List<Path> SOURCE_ROOTS = List.of(Path.of("src/main/java"), Path.of("src/test/java"));

    private static final String KEEP_LIST_RESOURCE = "/architecture/language-keep-list.txt";

    private static final Pattern VIETNAMESE_LETTER = Pattern.compile("[\\u00C0-\\u00C3\\u00C8-\\u00CA\\u00CC\\u00CD"
        + "\\u00D2-\\u00D5\\u00D9\\u00DA\\u00DD\\u00E0-\\u00E3\\u00E8-\\u00EA\\u00EC\\u00ED\\u00F2-\\u00F5\\u00F9\\u00FA"
        + "\\u00FD\\u0102\\u0103\\u0110\\u0111\\u0128\\u0129\\u0168\\u0169\\u01A0\\u01A1\\u01AF\\u01B0\\u1EA0-\\u1EF9]");

    @Test
    @DisplayName("Java files outside the keep list have no Vietnamese in a comment or a string literal")
    void java_files_outside_the_keep_list_are_english() throws IOException {
        List<Path> files = javaFiles();
        Set<String> keepList = keepList();

        assertThat(files.size())
            .as("number of Java files scanned -- a low number means the check is fooling itself")
            .isGreaterThan(1000);
        assertThat(filesWithVietnamese(files, keepList))
            .as("write comments and messages in English; Vietnamese data goes in " + KEEP_LIST_RESOURCE + " only if it is data")
            .isEmpty();
    }

    @Test
    @DisplayName("every keep-list file still exists and still holds Vietnamese")
    void keep_list_has_no_stale_entries() throws IOException {
        Set<String> stale = new TreeSet<String>();
        for (String entry : keepList()) {
            Path file = Path.of(entry);
            if (!Files.exists(file) || !hasVietnameseInCommentOrString(Files.readString(file, StandardCharsets.UTF_8))) {
                stale.add(entry);
            }
        }

        assertThat(stale)
            .as("stale entries: remove them from " + KEEP_LIST_RESOURCE + " (the list may only shrink)")
            .isEmpty();
    }

    @Test
    @DisplayName("the rule can fail: Vietnamese in a comment or a string is detected, a char literal is not")
    void rule_detects_violating_snippets() {
        String vietnamese = "Ti\u1EBFng Vi\u1EC7t";

        assertThat(hasVietnameseInCommentOrString("// " + vietnamese + "\nint a;")).isTrue();
        assertThat(hasVietnameseInCommentOrString("/** " + vietnamese + " */ class A {}")).isTrue();
        assertThat(hasVietnameseInCommentOrString("String s = \"" + vietnamese + "\";")).isTrue();
        assertThat(hasVietnameseInCommentOrString("String s = \"\"\"\n" + vietnamese + "\n\"\"\";")).isTrue();
        assertThat(hasVietnameseInCommentOrString("String s = \"http://x\"; // " + vietnamese)).isTrue();
    }

    @Test
    @DisplayName("the rule has no false positives: English text, a char literal and an escaped quote are accepted")
    void rule_accepts_english_and_char_literals() {
        assertThat(hasVietnameseInCommentOrString("// plain English\nString s = \"plain\";")).isFalse();
        assertThat(hasVietnameseInCommentOrString("String s = value.replace('\u0111', 'd');")).isFalse();
        assertThat(hasVietnameseInCommentOrString("char quote = '\"'; String s = \"a\\\"b\";")).isFalse();
        assertThat(hasVietnameseInCommentOrString("String url = \"http://x\"; int a;")).isFalse();
    }

    private static Set<String> filesWithVietnamese(List<Path> files, Set<String> keepList) throws IOException {
        Set<String> offenders = new TreeSet<String>();
        for (Path file : files) {
            String path = file.toString().replace('\\', '/');
            if (!keepList.contains(path) && hasVietnameseInCommentOrString(Files.readString(file, StandardCharsets.UTF_8))) {
                offenders.add(path);
            }
        }
        return offenders;
    }

    private static boolean hasVietnameseInCommentOrString(String source) {
        return commentAndStringText(source).stream().anyMatch(text -> VIETNAMESE_LETTER.matcher(text).find());
    }

    /** The text of every comment, string literal and text block of the source; char literals are skipped. */
    private static List<String> commentAndStringText(String source) {
        List<String> texts = new ArrayList<String>();
        int index = 0;
        while (index < source.length()) {
            int end = endOfToken(source, index);
            if (end > index && isCommentOrString(source, index)) {
                texts.add(source.substring(index, end));
            }
            index = Math.max(end, index + 1);
        }
        return texts;
    }

    private static boolean isCommentOrString(String source, int index) {
        char first = source.charAt(index);
        return first == '"' || source.startsWith("//", index) || source.startsWith("/*", index);
    }

    /** The index after the comment, string, text block or char literal that starts at {@code index}, else {@code index}. */
    private static int endOfToken(String source, int index) {
        if (source.startsWith("//", index)) {
            return endOfLine(source, index);
        }
        if (source.startsWith("/*", index)) {
            return endAfter(source, "*/", index + 2);
        }
        if (source.startsWith("\"\"\"", index)) {
            return endOfQuoted(source, "\"\"\"", index + 3);
        }
        if (source.charAt(index) == '"' || source.charAt(index) == '\'') {
            return endOfQuoted(source, String.valueOf(source.charAt(index)), index + 1);
        }
        return index;
    }

    private static int endOfLine(String source, int from) {
        int newline = source.indexOf('\n', from);
        return newline < 0 ? source.length() : newline;
    }

    private static int endAfter(String source, String terminator, int from) {
        int found = source.indexOf(terminator, from);
        return found < 0 ? source.length() : found + terminator.length();
    }

    private static int endOfQuoted(String source, String quote, int from) {
        int position = from;
        while (position < source.length() && !source.startsWith(quote, position)) {
            position += source.charAt(position) == '\\' ? 2 : 1;
        }
        return Math.min(position + quote.length(), source.length());
    }

    private static List<Path> javaFiles() throws IOException {
        List<Path> files = new ArrayList<Path>();
        for (Path root : SOURCE_ROOTS) {
            try (Stream<Path> walk = Files.walk(root)) {
                files.addAll(walk.filter(path -> path.toString().endsWith(".java")).toList());
            }
        }
        return files;
    }

    private static Set<String> keepList() throws IOException {
        try (InputStream stream = LanguageConventionTests.class.getResourceAsStream(KEEP_LIST_RESOURCE)) {
            assertThat(stream).as(KEEP_LIST_RESOURCE + " must exist").isNotNull();
            String content = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            return content.lines()
                .map(line -> line.strip())
                .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .collect(Collectors.toCollection(TreeSet::new));
        }
    }
}
