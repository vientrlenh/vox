package com.sep.vox.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Keeps one convention for GraphQL DataLoaders:
 * <ul>
 *   <li>{@code getDataLoader(} is called only inside {@code GraphQLDataLoaders}, so a missing loader fails the same
 *       way everywhere;</li>
 *   <li>a loader is registered with {@code forName(CONSTANT)}, where the constant is declared in the same config;</li>
 *   <li>no two registrations use the same name;</li>
 *   <li>a name reads {@code <result>By<KeyField>}.</li>
 * </ul>
 *
 * <p>The scan reads source text because a string literal and a method name leave nothing for ArchUnit to inspect in
 * the bytecode that distinguishes a constant from a literal. Each rule has a self-test that feeds it a violating
 * snippet, so a rule that stops matching fails here and not silently.
 */
class GraphQLDataLoaderConventionTests {

    private static final Path MAIN_SOURCE_DIR = Path.of("src/main/java");

    private static final String LOADER_LOOKUP_FILE_NAME = "GraphQLDataLoaders.java";

    private static final Pattern DIRECT_LOOKUP = Pattern.compile("\\bgetDataLoader\\s*\\(");

    private static final Pattern REGISTRATION = Pattern.compile("\\bforName\\s*\\(\\s*([^)]*?)\\s*\\)");

    private static final Pattern CONSTANT_REFERENCE = Pattern.compile("[A-Z][A-Z0-9_]*");

    private static final Pattern CONSTANT_DECLARATION = Pattern.compile(
        "static\\s+final\\s+String\\s+([A-Z][A-Z0-9_]*)\\s*=\\s*\"([^\"]*)\"\\s*;");

    private static final Pattern RESULT_BY_KEY_FIELD = Pattern.compile("[a-z][A-Za-z0-9]*By[A-Z][A-Za-z0-9]*");

    @Test
    @DisplayName("getDataLoader is called only inside GraphQLDataLoaders")
    void loaders_are_looked_up_only_through_the_shared_class() {
        List<String> offenders = new ArrayList<String>();
        for (SourceFile file : mainSources()) {
            offenders.addAll(directLookups(file.name(), file.text()));
        }
        assertThat(offenders).as("files that call getDataLoader directly").isEmpty();
    }

    @Test
    @DisplayName("every forName argument is a constant declared in the same config")
    void registrations_use_constants_of_their_own_config() {
        List<String> offenders = new ArrayList<String>();
        for (SourceFile file : mainSources()) {
            offenders.addAll(registrationsWithoutOwnConstant(file.name(), file.text()));
        }
        assertThat(offenders).as("registrations whose name is not a constant of the same file").isEmpty();
    }

    @Test
    @DisplayName("no loader name is registered twice")
    void loader_names_are_registered_once() {
        assertThat(duplicateRegistrations(mainSources())).as("loader names registered more than once").isEmpty();
    }

    @Test
    @DisplayName("every loader name reads <result>By<KeyField>")
    void loader_names_follow_the_result_by_key_pattern() {
        List<String> offenders = new ArrayList<String>();
        for (SourceFile file : mainSources()) {
            offenders.addAll(misnamedLoaders(file.name(), file.text()));
        }
        assertThat(offenders).as("loader names that do not match <result>By<KeyField>").isEmpty();
    }

    @Test
    @DisplayName("self-test: a direct getDataLoader call is reported, the shared class is allowed")
    void direct_lookup_rule_matches_a_violating_snippet() {
        String violation = "DataLoader<UUID, UserDto> loader = environment.getDataLoader(\"userByUserId\");";
        assertThat(directLookups("GraphQLUserController.java", violation)).hasSize(1);
        assertThat(directLookups(LOADER_LOOKUP_FILE_NAME, violation)).isEmpty();
        assertThat(directLookups("GraphQLUserController.java", "GraphQLDataLoaders.require(environment, NAME);"))
            .isEmpty();
    }

    @Test
    @DisplayName("self-test: a literal or a foreign constant in forName is reported")
    void constant_rule_matches_a_violating_snippet() {
        String declaration = "public static final String USER_BY_USER_ID = \"userByUserId\";\n";
        assertThat(registrationsWithoutOwnConstant("A.java", "registry.forName(\"userByUserId\")")).hasSize(1);
        assertThat(registrationsWithoutOwnConstant("A.java", "registry.forName(Other.USER_BY_USER_ID)")).hasSize(1);
        assertThat(registrationsWithoutOwnConstant("A.java", "registry.<UUID, X>forName(MISSING)")).hasSize(1);
        assertThat(registrationsWithoutOwnConstant("A.java", declaration + "registry.forName(USER_BY_USER_ID)"))
            .isEmpty();
    }

    @Test
    @DisplayName("self-test: the same name in two files, or twice in one file, is reported")
    void duplicate_rule_matches_a_violating_snippet() {
        String registration = "public static final String USER_BY_USER_ID = \"userByUserId\";\n"
            + "registry.forName(USER_BY_USER_ID)";
        String otherName = "public static final String ROLE_BY_ROLE_ID = \"roleByRoleId\";\n"
            + "registry.forName(ROLE_BY_ROLE_ID)";
        assertThat(duplicateRegistrations(List.of(new SourceFile("A.java", registration),
            new SourceFile("B.java", registration)))).hasSize(1);
        assertThat(duplicateRegistrations(List.of(new SourceFile("A.java", registration + "\n" + registration))))
            .hasSize(1);
        assertThat(duplicateRegistrations(List.of(new SourceFile("A.java", registration),
            new SourceFile("B.java", otherName)))).isEmpty();
    }

    @Test
    @DisplayName("self-test: a name that is not <result>By<KeyField> is reported")
    void naming_rule_matches_a_violating_snippet() {
        assertThat(misnamedLoaders("A.java", constantDeclaration("userById2", "USER"))).isEmpty();
        assertThat(misnamedLoaders("A.java", constantDeclaration("users", "USERS"))).hasSize(1);
        assertThat(misnamedLoaders("A.java", constantDeclaration("ByUserId", "BY"))).hasSize(1);
        assertThat(misnamedLoaders("A.java", constantDeclaration("userByuserId", "USER"))).hasSize(1);
        assertThat(misnamedLoaders("A.java", constantDeclaration("userByUserId", "USER"))).isEmpty();
    }

    private static String constantDeclaration(String value, String constant) {
        return "public static final String " + constant + " = \"" + value + "\";\nregistry.forName(" + constant + ")";
    }

    private static List<String> directLookups(String fileName, String text) {
        List<String> offenders = new ArrayList<String>();
        if (!fileName.equals(LOADER_LOOKUP_FILE_NAME) && DIRECT_LOOKUP.matcher(text).find()) {
            offenders.add(fileName);
        }
        return offenders;
    }

    private static List<String> registrationsWithoutOwnConstant(String fileName, String text) {
        Map<String, String> constants = constantValues(text);
        List<String> offenders = new ArrayList<String>();
        for (String argument : registeredArguments(text)) {
            if (!CONSTANT_REFERENCE.matcher(argument).matches() || !constants.containsKey(argument)) {
                offenders.add(fileName + ": forName(" + argument + ")");
            }
        }
        return offenders;
    }

    private static List<String> duplicateRegistrations(List<SourceFile> files) {
        Map<String, String> firstFileByName = new HashMap<String, String>();
        List<String> offenders = new ArrayList<String>();
        for (SourceFile file : files) {
            for (String name : registeredNames(file.text())) {
                String previous = firstFileByName.putIfAbsent(name, file.name());
                if (previous != null) {
                    offenders.add(name + " in " + previous + " and " + file.name());
                }
            }
        }
        return offenders;
    }

    private static List<String> misnamedLoaders(String fileName, String text) {
        List<String> offenders = new ArrayList<String>();
        for (String name : registeredNames(text)) {
            if (!RESULT_BY_KEY_FIELD.matcher(name).matches()) {
                offenders.add(fileName + ": " + name);
            }
        }
        return offenders;
    }

    private static List<String> registeredNames(String text) {
        Map<String, String> constants = constantValues(text);
        List<String> names = new ArrayList<String>();
        for (String argument : registeredArguments(text)) {
            String value = constants.get(argument);
            names.add(value != null ? value : argument);
        }
        return names;
    }

    private static List<String> registeredArguments(String text) {
        List<String> arguments = new ArrayList<String>();
        Matcher matcher = REGISTRATION.matcher(text);
        while (matcher.find()) {
            arguments.add(matcher.group(1));
        }
        return arguments;
    }

    private static Map<String, String> constantValues(String text) {
        Map<String, String> constants = new HashMap<String, String>();
        Matcher matcher = CONSTANT_DECLARATION.matcher(text);
        while (matcher.find()) {
            constants.put(matcher.group(1), matcher.group(2));
        }
        return constants;
    }

    private static List<SourceFile> mainSources() {
        try (Stream<Path> paths = Files.walk(MAIN_SOURCE_DIR)) {
            List<Path> javaFiles = paths.filter(path -> path.toString().endsWith(".java")).toList();
            List<SourceFile> sources = new ArrayList<SourceFile>();
            for (Path path : javaFiles) {
                sources.add(new SourceFile(path.getFileName().toString(),
                    Files.readString(path, StandardCharsets.UTF_8)));
            }
            return sources;
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot scan " + MAIN_SOURCE_DIR, exception);
        }
    }

    private record SourceFile(String name, String text) {
    }
}
