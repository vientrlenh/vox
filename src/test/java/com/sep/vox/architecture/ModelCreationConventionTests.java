package com.sep.vox.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Application code does not build a domain model with {@code Model.builder()}: it calls a named factory or behaviour
 * method on the model. Infrastructure mappers, domain code and tests keep using the builder.
 *
 * <p>Why scan source text: the call is a plain static method call, and the exemption list is a list of files, so a text
 * scan with the comments removed is enough and needs no bytecode.
 */
class ModelCreationConventionTests {

    private static final Path MAIN_ROOT = Path.of("src/main/java/com/sep/vox");

    private static final Path MODEL_DIR = MAIN_ROOT.resolve("domain/model");

    private static final Path APPLICATION_DIR = MAIN_ROOT.resolve("application");

    private static final String EXEMPTIONS_RESOURCE = "/architecture/model-creation-exemptions.txt";

    private static final Pattern CONCRETE_CLASS_DECLARATION =
        Pattern.compile("(?m)^(?:public\\s+)?(abstract\\s+)?(class|interface|enum|record)\\s+(\\w+)");

    @Test
    @DisplayName("application does not call builder() on a domain model")
    void application_does_not_call_model_builder() throws IOException {
        List<Path> files = javaFilesUnder(APPLICATION_DIR);
        Pattern call = modelBuilderCall(concreteModelNames());

        assertThat(files.size())
            .as("number of application files scanned -- a low number means the check is fooling itself")
            .isGreaterThan(100);
        assertThat(filesCalling(files, call, exemptedFiles()))
            .as("call a named factory or behaviour method on the model instead of Model.builder()")
            .isEmpty();
    }

    @Test
    @DisplayName("every exempted application file still calls a model builder")
    void exemptions_still_apply() throws IOException {
        Pattern call = modelBuilderCall(concreteModelNames());
        Set<String> stale = new TreeSet<String>(exemptedFiles());

        stale.removeAll(filesCalling(javaFilesUnder(APPLICATION_DIR), call, Set.of()));

        assertThat(stale)
            .as("exempted files without a model builder call: remove them from " + EXEMPTIONS_RESOURCE)
            .isEmpty();
    }

    @Test
    @DisplayName("the rule can fail: a snippet that builds a model with builder() is detected")
    void rule_detects_a_violating_snippet() {
        Pattern call = modelBuilderCall(Set.of("School"));

        assertThat(call.matcher(withoutComments("var s = School.builder().name(\"x\").build();")).find()).isTrue();
        assertThat(call.matcher(withoutComments("// School.builder() is only mentioned here")).find()).isFalse();
        assertThat(call.matcher(withoutComments("School.named(\"x\");")).find()).isFalse();
    }

    private static Set<String> concreteModelNames() throws IOException {
        Set<String> names = new TreeSet<String>();
        for (Path file : javaFilesUnder(MODEL_DIR)) {
            Matcher declaration = CONCRETE_CLASS_DECLARATION.matcher(readWithoutComments(file));
            if (declaration.find() && declaration.group(1) == null && declaration.group(2).equals("class")) {
                names.add(declaration.group(3));
            }
        }
        assertThat(names.size()).as("number of domain models found").isGreaterThan(50);
        return names;
    }

    private static Pattern modelBuilderCall(Set<String> modelNames) {
        return Pattern.compile("\\b(?:" + String.join("|", modelNames) + ")\\s*\\.\\s*builder\\s*\\(\\s*\\)");
    }

    private static Set<String> filesCalling(List<Path> files, Pattern call, Set<String> exemptedFiles)
        throws IOException {
        Set<String> offenders = new TreeSet<String>();
        for (Path file : files) {
            String relativePath = APPLICATION_DIR.relativize(file).toString().replace('\\', '/');
            if (!exemptedFiles.contains(relativePath) && call.matcher(readWithoutComments(file)).find()) {
                offenders.add(relativePath);
            }
        }
        return offenders;
    }

    private static Set<String> exemptedFiles() throws IOException {
        try (InputStream stream = ModelCreationConventionTests.class.getResourceAsStream(EXEMPTIONS_RESOURCE)) {
            assertThat(stream).as(EXEMPTIONS_RESOURCE + " must exist").isNotNull();
            String content = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            return content.lines()
                .map(String::strip)
                .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .map(line -> line.replaceFirst("^application/", ""))
                .collect(Collectors.toCollection(TreeSet::new));
        }
    }

    private static List<Path> javaFilesUnder(Path directory) throws IOException {
        try (Stream<Path> files = Files.walk(directory)) {
            return new ArrayList<Path>(files.filter(path -> path.toString().endsWith(".java")).toList());
        }
    }

    private static String readWithoutComments(Path file) {
        try {
            return withoutComments(Files.readString(file, StandardCharsets.UTF_8));
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static String withoutComments(String source) {
        return source.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)//[^\n]*", "");
    }
}
