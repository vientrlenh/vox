package com.sep.vox.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Keeps Lombok out of the inner layers: domain uses none, application uses only {@code @RequiredArgsConstructor} and
 * {@code @Slf4j}. Infrastructure and interfaces have no restriction.
 *
 * <p>Why scan source text instead of using ArchUnit like {@link LayeredArchitectureTests}: Lombok's annotations have
 * source retention, so they are gone from the bytecode and no bytecode rule can see them.
 */
class LombokUsageConventionTests {

    private static final Path MAIN_ROOT = Path.of("src/main/java/com/sep/vox");

    private static final Path DOMAIN_DIR = MAIN_ROOT.resolve("domain");

    private static final Path APPLICATION_DIR = MAIN_ROOT.resolve("application");

    /** Any Lombok type, imported, wildcard-imported or written fully qualified. */
    private static final Pattern LOMBOK_REFERENCE = Pattern.compile("\\blombok(?:\\.[\\w*]+)+");

    private static final Set<String> APPLICATION_ALLOWED_LOMBOK_TYPES =
        Set.of("lombok.RequiredArgsConstructor", "lombok.extern.slf4j.Slf4j");

    /**
     * Known debt, not a permission: the list may only shrink. UpdateSchoolRoomCommand uses {@code @Builder}, which
     * AGENTS.md does not allow on a command anyway.
     */
    private static final Set<String> APPLICATION_EXEMPTIONS = Set.of("UpdateSchoolRoomCommand.java");

    @Test
    @DisplayName("domain uses no Lombok")
    void domain_uses_no_lombok() throws IOException {
        List<Path> files = javaFilesUnder(DOMAIN_DIR);

        assertScannedEnough(files);
        assertThat(disallowedLombokReferences(files, Set.of(), Set.of()))
            .as("domain is plain Java: no Lombok type may be referenced")
            .isEmpty();
    }

    @Test
    @DisplayName("application uses only @RequiredArgsConstructor and @Slf4j from Lombok")
    void application_uses_only_allowed_lombok_types() throws IOException {
        List<Path> files = javaFilesUnder(APPLICATION_DIR);

        assertScannedEnough(files);
        assertThat(disallowedLombokReferences(files, APPLICATION_ALLOWED_LOMBOK_TYPES, APPLICATION_EXEMPTIONS))
            .as("application may use only " + APPLICATION_ALLOWED_LOMBOK_TYPES + " from Lombok")
            .isEmpty();
    }

    @Test
    @DisplayName("every exempted application file still uses a Lombok type that is not allowed")
    void application_exemptions_still_apply() throws IOException {
        List<Path> files = javaFilesUnder(APPLICATION_DIR);
        List<String> stale = new ArrayList<String>(APPLICATION_EXEMPTIONS);

        for (Path file : files) {
            if (!disallowedLombokReferences(List.of(file), APPLICATION_ALLOWED_LOMBOK_TYPES, Set.of()).isEmpty()) {
                stale.remove(file.getFileName().toString());
            }
        }

        assertThat(stale)
            .as("exempted files that no longer use a disallowed Lombok type: remove them from APPLICATION_EXEMPTIONS")
            .isEmpty();
    }

    private static void assertScannedEnough(List<Path> files) {
        assertThat(files.size())
            .as("number of source files scanned -- a low number means the check is fooling itself")
            .isGreaterThan(100);
    }

    private static List<String> disallowedLombokReferences(
        List<Path> files, Set<String> allowedTypes, Set<String> exemptFileNames) throws IOException {
        List<String> offenders = new ArrayList<String>();
        for (Path file : files) {
            if (exemptFileNames.contains(file.getFileName().toString())) {
                continue;
            }
            Matcher matcher = LOMBOK_REFERENCE.matcher(readWithoutComments(file));
            while (matcher.find()) {
                if (!allowedTypes.contains(matcher.group())) {
                    offenders.add(file.getFileName() + ": `" + matcher.group() + "`");
                }
            }
        }
        return offenders;
    }

    private static List<Path> javaFilesUnder(Path directory) throws IOException {
        try (Stream<Path> files = Files.walk(directory)) {
            return files.filter(path -> path.toString().endsWith(".java")).toList();
        }
    }

    private static String readWithoutComments(Path file) throws IOException {
        String source = Files.readString(file, StandardCharsets.UTF_8);
        return source.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)//[^\n]*", "");
    }
}
