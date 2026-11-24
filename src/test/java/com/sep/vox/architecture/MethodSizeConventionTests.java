package com.sep.vox.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sep.vox.architecture.MethodLengthScanner.MeasuredMethod;

/**
 * A method or constructor is at most 25 lines, counted from its return type (or constructor name) to its closing brace
 * (AGENTS.md, "Small functions"). Methods that are still longer are listed in {@code method-size-exemptions.txt}; the
 * list may only shrink, so a listed method that is short enough fails the test.
 */
class MethodSizeConventionTests {

    private static final Path MAIN_ROOT = Path.of("src/main/java/com/sep/vox");

    private static final String EXEMPTIONS_RESOURCE = "/architecture/method-size-exemptions.txt";

    private static final int MAXIMUM_LINES = 25;

    @Test
    @DisplayName("no method is longer than 25 lines unless it is listed in method-size-exemptions.txt")
    void no_unlisted_method_is_too_long() throws IOException {
        List<MeasuredMethod> all = MethodLengthScanner.measureDirectory(MAIN_ROOT);

        assertThat(all.size())
            .as("number of methods measured -- a low number means the check is fooling itself")
            .isGreaterThan(5000);
        assertThat(unlistedLongMethods(all, exemptions()))
            .as("split the method into private methods of at most 25 lines")
            .isEmpty();
    }

    @Test
    @DisplayName("every listed method is still longer than 25 lines")
    void listed_methods_are_still_too_long() throws IOException {
        Map<String, Integer> stale = new TreeMap<String, Integer>(exemptions());

        for (MeasuredMethod method : MethodLengthScanner.measureDirectory(MAIN_ROOT)) {
            if (method.lines() > MAXIMUM_LINES) {
                stale.computeIfPresent(method.key(), (key, count) -> count == 1 ? null : count - 1);
            }
        }

        assertThat(stale.keySet())
            .as("listed methods that are short enough or gone: remove them from " + EXEMPTIONS_RESOURCE)
            .isEmpty();
    }

    @Test
    @DisplayName("the rule can fail: a 26-line method is detected and a 25-line method is not")
    void rule_detects_a_violating_snippet() throws IOException {
        List<MeasuredMethod> measured = MethodLengthScanner.measureSource("Sample.java", sampleWithMethods(26, 25));

        assertThat(unlistedLongMethods(measured, Map.of())).containsExactly("Sample.java#tooLong()");
    }

    private static List<String> unlistedLongMethods(List<MeasuredMethod> measured, Map<String, Integer> exemptions) {
        Map<String, Integer> remainingExemptions = new TreeMap<String, Integer>(exemptions);
        List<String> offenders = new ArrayList<String>();
        for (MeasuredMethod method : measured) {
            if (method.lines() > MAXIMUM_LINES && !consumeExemption(remainingExemptions, method.key())) {
                offenders.add(method.key());
            }
        }
        return offenders;
    }

    private static boolean consumeExemption(Map<String, Integer> remainingExemptions, String key) {
        Integer remaining = remainingExemptions.get(key);
        if (remaining == null) {
            return false;
        }
        remainingExemptions.put(key, remaining - 1);
        if (remaining == 1) {
            remainingExemptions.remove(key);
        }
        return true;
    }

    private static Map<String, Integer> exemptions() throws IOException {
        try (InputStream stream = MethodSizeConventionTests.class.getResourceAsStream(EXEMPTIONS_RESOURCE)) {
            assertThat(stream).as(EXEMPTIONS_RESOURCE + " must exist").isNotNull();
            String content = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            return content.lines()
                .map(String::strip)
                .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .collect(Collectors.toMap(line -> line, line -> 1, Integer::sum, TreeMap::new));
        }
    }

    private static String sampleWithMethods(int tooLongLines, int allowedLines) {
        return "class Sample {\n" + methodOfLines("tooLong", tooLongLines) + methodOfLines("allowed", allowedLines) + "}\n";
    }

    private static String methodOfLines(String name, int lines) {
        StringBuilder method = new StringBuilder("    void " + name + "() {\n");
        for (int line = 0; line < lines - 2; line++) {
            method.append("        int value").append(line).append(" = ").append(line).append(";\n");
        }
        return method.append("    }\n").toString();
    }
}
