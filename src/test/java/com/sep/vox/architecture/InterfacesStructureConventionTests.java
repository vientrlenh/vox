package com.sep.vox.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;

/**
 * Keeps the layout of the interfaces layer and of the commands and queries it feeds:
 * <ul>
 *   <li>REST controllers are named {@code RestXxxController}, GraphQL controllers {@code GraphQLXxxController};</li>
 *   <li>no class of this project spells the word {@code Graphql} or {@code GraphQl} (Spring's own types are not
 *       declared here, so they never appear);</li>
 *   <li>commands, queries and request records live in a feature sub-package, never directly in
 *       {@code port/input/command}, {@code port/input/query} or a {@code dto/request} package;</li>
 *   <li>an interfaces {@code mapper} package holds only the classes in {@link #ALLOWED_MAPPERS}, which may only
 *       shrink: a request record converts itself with a static {@code toCommand} or {@code toQuery};</li>
 *   <li>such a conversion returns a command (or a query), not another type.</li>
 * </ul>
 */
class InterfacesStructureConventionTests {

    private static final String ROOT_PACKAGE = "com.sep.vox";

    private static final String INTERFACES_PACKAGE = ROOT_PACKAGE + ".interfaces";

    private static final String COMMAND_PACKAGE = ROOT_PACKAGE + ".application.port.input.command";

    private static final String QUERY_PACKAGE = ROOT_PACKAGE + ".application.port.input.query";

    private static final Pattern MISSPELLED_GRAPHQL = Pattern.compile("Graphql|GraphQl");

    private static final Pattern CONVERSION_METHOD_NAME = Pattern.compile("to[A-Za-z]*(Command|Query)");

    /** Mapper classes that hold logic or have no request record to move onto (D4, D5). The list may only shrink. */
    private static final Set<String> ALLOWED_MAPPERS = Set.of(
        "com.sep.vox.interfaces.rest.mapper.PreviewAssessmentPolicyImportFromFileCommandMapper",
        "com.sep.vox.interfaces.rest.mapper.PreviewRubricCriterionImportCommandMapper",
        "com.sep.vox.interfaces.rest.mapper.PreviewRubricResultBandImportCommandMapper",
        "com.sep.vox.interfaces.rest.mapper.PreviewRubricVersionImportFromFileCommandMapper",
        "com.sep.vox.interfaces.rest.mapper.ViewMyAssessmentsQueryMapper",
        "com.sep.vox.interfaces.kafka.mapper.ProctoringAlertCommandMapper",
        "com.sep.vox.interfaces.kafka.mapper.RecordAssessmentAttemptEvaluationCommandMapper",
        "com.sep.vox.interfaces.kafka.mapper.RecordPracticeAttemptEvaluationCommandMapper",
        "com.sep.vox.interfaces.kafka.mapper.RecordingPartChangedCommandMapper");

    private static final JavaClasses ALL_CLASSES = new ClassFileImporter()
        .importPackages(ROOT_PACKAGE);

    private static final JavaClasses MAIN_CLASSES = new ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages(ROOT_PACKAGE);

    @Test
    @DisplayName("controllers carry the Rest or GraphQL prefix")
    void controllers_are_named_after_their_transport() {
        List<String> offenders = new ArrayList<String>();
        for (JavaClass javaClass : MAIN_CLASSES) {
            offenders.addAll(controllersWithWrongPrefix(javaClass.getPackageName(), javaClass.getSimpleName()));
        }
        assertThat(offenders).as("controllers whose name does not start with Rest or GraphQL").isEmpty();
    }

    @Test
    @DisplayName("no class name spells the word Graphql or GraphQl")
    void class_names_spell_graphql_with_two_capital_letters() {
        List<String> offenders = new ArrayList<String>();
        for (JavaClass javaClass : ALL_CLASSES) {
            if (MISSPELLED_GRAPHQL.matcher(javaClass.getName()).find()) {
                offenders.add(javaClass.getName());
            }
        }
        assertThat(offenders).as("classes that must spell GraphQL").isEmpty();
    }

    @Test
    @DisplayName("commands, queries and request records sit in a feature sub-package")
    void commands_queries_and_requests_are_not_flat() {
        List<String> offenders = new ArrayList<String>();
        for (JavaClass javaClass : MAIN_CLASSES) {
            if (isFlatPackage(javaClass.getPackageName())) {
                offenders.add(javaClass.getName());
            }
        }
        assertThat(offenders).as("classes directly in a command, query or dto.request package").isEmpty();
    }

    @Test
    @DisplayName("interfaces mapper packages hold only the allowed classes, and every allowed class still exists")
    void mapper_packages_hold_only_the_allowed_classes() {
        Set<String> existing = new TreeSet<String>();
        for (JavaClass javaClass : MAIN_CLASSES) {
            if (isInterfacesMapperPackage(javaClass.getPackageName())) {
                existing.add(javaClass.getName());
            }
        }
        assertThat(existing).as("mapper classes: remove from the list when deleted, never add").isEqualTo(ALLOWED_MAPPERS);
    }

    @Test
    @DisplayName("toCommand and toQuery on a request record return a command or a query")
    void conversions_return_commands_or_queries() {
        List<String> offenders = new ArrayList<String>();
        for (JavaClass javaClass : MAIN_CLASSES) {
            if (isRequestPackage(javaClass.getPackageName())) {
                offenders.addAll(wronglyTypedConversions(javaClass.reflect()));
            }
        }
        assertThat(offenders).as("conversion methods that are not static or return the wrong type").isEmpty();
    }

    @Test
    @DisplayName("a REST controller without the Rest prefix and a GraphQL controller without the prefix are reported")
    void guard_reports_controllers_with_the_wrong_prefix() {
        assertThat(controllersWithWrongPrefix(INTERFACES_PACKAGE + ".rest.controller", "AssessmentController"))
            .hasSize(1);
        assertThat(controllersWithWrongPrefix(INTERFACES_PACKAGE + ".graphql.controller", "RestAssessmentController"))
            .hasSize(1);
        assertThat(controllersWithWrongPrefix(INTERFACES_PACKAGE + ".rest.controller", "RestAssessmentController"))
            .isEmpty();
        assertThat(controllersWithWrongPrefix(INTERFACES_PACKAGE + ".graphql.controller", "GraphQLAssessmentController"))
            .isEmpty();
    }

    @Test
    @DisplayName("Graphql and GraphQl are reported, GraphQL is not")
    void guard_reports_the_misspelled_graphql_word() {
        assertThat(MISSPELLED_GRAPHQL.matcher("AssessmentGraphqlTests").find()).isTrue();
        assertThat(MISSPELLED_GRAPHQL.matcher("GraphQlAssessmentController").find()).isTrue();
        assertThat(MISSPELLED_GRAPHQL.matcher("GraphQLAssessmentController").find()).isFalse();
    }

    @Test
    @DisplayName("a class directly in a command, query or request package is reported")
    void guard_reports_flat_packages() {
        assertThat(isFlatPackage(COMMAND_PACKAGE)).isTrue();
        assertThat(isFlatPackage(QUERY_PACKAGE)).isTrue();
        assertThat(isFlatPackage(INTERFACES_PACKAGE + ".rest.dto.request")).isTrue();
        assertThat(isFlatPackage(INTERFACES_PACKAGE + ".graphql.input")).isTrue();
        assertThat(isFlatPackage(COMMAND_PACKAGE + ".assessment")).isFalse();
        assertThat(isFlatPackage(QUERY_PACKAGE + ".key")).isFalse();
        assertThat(isFlatPackage(INTERFACES_PACKAGE + ".rest.dto.request.assessment")).isFalse();
    }

    @Test
    @DisplayName("a class in an interfaces mapper package is recognised, other packages are not")
    void guard_recognises_mapper_packages() {
        assertThat(isInterfacesMapperPackage(INTERFACES_PACKAGE + ".rest.mapper")).isTrue();
        assertThat(isInterfacesMapperPackage(INTERFACES_PACKAGE + ".graphql.mapper")).isTrue();
        assertThat(isInterfacesMapperPackage(INTERFACES_PACKAGE + ".kafka.mapper")).isTrue();
        assertThat(isInterfacesMapperPackage(INTERFACES_PACKAGE + ".rest.dto.request.assessment")).isFalse();
    }

    @Test
    @DisplayName("a toCommand that returns a String, a toQuery that returns a command and a non-static one are reported")
    void guard_reports_wrongly_typed_conversions() {
        assertThat(wronglyTypedConversions(SampleRequest.class))
            .containsExactlyInAnyOrder(
                "SampleRequest.toCommand",
                "SampleRequest.toQuery",
                "SampleRequest.toSampleCommand");
    }

    private static List<String> controllersWithWrongPrefix(String packageName, String simpleName) {
        List<String> offenders = new ArrayList<String>();
        if (!simpleName.endsWith("Controller")) {
            return offenders;
        }
        if (packageName.startsWith(INTERFACES_PACKAGE + ".rest.controller") && !simpleName.startsWith("Rest")) {
            offenders.add(packageName + "." + simpleName);
        }
        if (packageName.startsWith(INTERFACES_PACKAGE + ".graphql.controller") && !simpleName.startsWith("GraphQL")) {
            offenders.add(packageName + "." + simpleName);
        }
        return offenders;
    }

    private static boolean isFlatPackage(String packageName) {
        return packageName.equals(COMMAND_PACKAGE)
            || packageName.equals(QUERY_PACKAGE)
            || packageName.equals(INTERFACES_PACKAGE + ".rest.dto.request")
            || packageName.equals(INTERFACES_PACKAGE + ".graphql.input");
    }

    private static boolean isInterfacesMapperPackage(String packageName) {
        return packageName.equals(INTERFACES_PACKAGE + ".rest.mapper")
            || packageName.equals(INTERFACES_PACKAGE + ".graphql.mapper")
            || packageName.equals(INTERFACES_PACKAGE + ".kafka.mapper");
    }

    private static boolean isRequestPackage(String packageName) {
        return packageName.startsWith(INTERFACES_PACKAGE + ".rest.dto.request")
            || packageName.startsWith(INTERFACES_PACKAGE + ".graphql.input");
    }

    private static List<String> wronglyTypedConversions(Class<?> type) {
        List<String> offenders = new ArrayList<String>();
        for (Method method : type.getDeclaredMethods()) {
            String name = method.getName();
            if (!CONVERSION_METHOD_NAME.matcher(name).matches()) {
                continue;
            }
            String expectedPackage = name.endsWith("Command") ? COMMAND_PACKAGE : QUERY_PACKAGE;
            boolean isStatic = Modifier.isStatic(method.getModifiers());
            boolean returnsExpectedType = method.getReturnType().getPackageName().startsWith(expectedPackage)
                && method.getReturnType().getSimpleName().endsWith(name.endsWith("Command") ? "Command" : "Query");
            if (!isStatic || !returnsExpectedType) {
                offenders.add(type.getSimpleName() + "." + name);
            }
        }
        return offenders;
    }

    private record SampleRequest(String name) {

        @SuppressWarnings("unused")
        static void toCommand() {}

        @SuppressWarnings("unused")
        static void toQuery() {}

        @SuppressWarnings("unused")
        static void toSampleCommand() {}
    }
}
