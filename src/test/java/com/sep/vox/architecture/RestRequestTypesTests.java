package com.sep.vox.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;

/**
 * Keeps REST inputs typed: a request record component or a controller {@code @RequestParam} /
 * {@code @PathVariable} that carries an enum, a date or an instant must be declared with that type,
 * never as a {@code String}. A {@code String} field passes every behavioural test, but it moves
 * parsing back into use cases and brings back a 500 for malformed input.
 *
 * <p>A {@code String} is flagged when the last word of its name is one of {@link #TYPED_NAME_SUFFIXES}
 * (for example {@code openAt}, {@code effectiveFrom}, {@code status}, {@code platform}). The only
 * exceptions are the fields in {@link #ALLOWED_STRING_INPUTS}: they have no matching Java enum, or are
 * free-form on purpose, so they stay {@code String} with their own validation.
 */
class RestRequestTypesTests {

    private static final String REST_PACKAGE = "com.sep.vox.interfaces.rest";

    private static final String REQUEST_PACKAGE = REST_PACKAGE + ".dto.request";

    private static final Set<String> TYPED_NAME_SUFFIXES = Set.of(
        "at", "date", "from", "to", "status", "type", "platform", "mode", "role", "method", "kind",
        "provider", "scope", "permission", "sharing", "deadline", "action", "level", "format", "category",
        "birth");

    /** {@code Class.field} entries that stay String: no matching enum exists (D5) or the value is free-form. */
    private static final Set<String> ALLOWED_STRING_INPUTS = Set.of(
        "BulkUpdateQuestionScopeStatusRequest.action",
        "BulkUpdateQuestionStatusRequest.action",
        "UpdateAssessmentBlueprintVersionStatusRequest.action",
        "UpdateAssessmentPaperStatusRequest.action",
        "UpdateAssessmentScheduleStatusRequest.action",
        "UpdateAssessmentSecurePoolStatusRequest.action",
        "UpdateAssessmentStatusRequest.action",
        "UpdateQuestionBankStatusRequest.action",
        "UpdateQuestionStatusRequest.action",
        "UpdateQuestionTopicStatusRequest.action",
        "BuyTokensRequest.paymentMethod",
        "PaymentMethodRequest.paymentMethod",
        "QuestionSelectionSpecRequest.targetBandLevel",
        "ImportFieldMappingRequest.dateFormat",
        "AiUsageEventItemRequest.provider",
        "AssessmentController.status",
        "QuestionController.scope",
        "QuestionController.contentType");

    private static final JavaClasses REST_CLASSES = new ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages(REST_PACKAGE);

    @Test
    @DisplayName("request records and controller parameters do not use String for enum or date inputs")
    void rest_inputs_named_like_enums_or_dates_must_not_be_strings() {
        List<String> offenders = new ArrayList<String>();
        for (JavaClass javaClass : REST_CLASSES) {
            Class<?> type = javaClass.reflect();
            if (javaClass.getPackageName().startsWith(REQUEST_PACKAGE)) {
                offenders.addAll(stringRecordComponentsNamedLikeTypedInputs(type));
            }
            if (javaClass.getPackageName().startsWith(REST_PACKAGE + ".controller")) {
                offenders.addAll(stringRequestParametersNamedLikeTypedInputs(type));
            }
        }
        assertThat(offenders)
            .as("String inputs that should be an enum, LocalDate or Instant (or be added to the allow-list)")
            .isEmpty();
    }

    @Test
    @DisplayName("the allow-list has no entry that no longer exists")
    void allow_list_entries_must_still_be_string_inputs() {
        Set<String> stillStringInputs = new TreeSet<String>();
        for (JavaClass javaClass : REST_CLASSES) {
            Class<?> type = javaClass.reflect();
            stillStringInputs.addAll(stringInputsNamedLikeTypedInputs(type));
        }
        assertThat(stillStringInputs).containsAll(ALLOWED_STRING_INPUTS);
    }

    @Test
    @DisplayName("a request record with String openAt or dateOfBirth is reported")
    void guard_reports_a_string_date_component() {
        assertThat(stringRecordComponentsNamedLikeTypedInputs(SampleRequest.class))
            .containsExactlyInAnyOrder("SampleRequest.openAt", "SampleRequest.dateOfBirth");
    }

    @Test
    @DisplayName("a controller @RequestParam String status is reported")
    void guard_reports_a_string_request_parameter() {
        assertThat(stringRequestParametersNamedLikeTypedInputs(SampleController.class))
            .containsExactly("SampleController.status");
    }

    private static Set<String> stringInputsNamedLikeTypedInputs(Class<?> type) {
        Set<String> names = new TreeSet<String>();
        names.addAll(stringRecordComponentsNamedLikeTypedInputsIgnoringAllowList(type));
        names.addAll(stringRequestParametersNamedLikeTypedInputsIgnoringAllowList(type));
        return names;
    }

    private static List<String> stringRecordComponentsNamedLikeTypedInputs(Class<?> type) {
        return stringRecordComponentsNamedLikeTypedInputsIgnoringAllowList(type).stream()
            .filter(entry -> !ALLOWED_STRING_INPUTS.contains(entry))
            .toList();
    }

    private static List<String> stringRequestParametersNamedLikeTypedInputs(Class<?> type) {
        return stringRequestParametersNamedLikeTypedInputsIgnoringAllowList(type).stream()
            .filter(entry -> !ALLOWED_STRING_INPUTS.contains(entry))
            .toList();
    }

    private static List<String> stringRecordComponentsNamedLikeTypedInputsIgnoringAllowList(Class<?> type) {
        List<String> entries = new ArrayList<String>();
        if (!type.isRecord()) {
            return entries;
        }
        for (RecordComponent component : type.getRecordComponents()) {
            if (component.getType() == String.class && isNamedLikeTypedInput(component.getName())) {
                entries.add(type.getSimpleName() + "." + component.getName());
            }
        }
        return entries;
    }

    private static List<String> stringRequestParametersNamedLikeTypedInputsIgnoringAllowList(Class<?> type) {
        List<String> entries = new ArrayList<String>();
        for (Method method : type.getDeclaredMethods()) {
            for (Parameter parameter : method.getParameters()) {
                String name = requestInputName(parameter);
                if (name != null && parameter.getType() == String.class && isNamedLikeTypedInput(name)) {
                    entries.add(type.getSimpleName() + "." + name);
                }
            }
        }
        return entries;
    }

    private static String requestInputName(Parameter parameter) {
        RequestParam requestParam = parameter.getAnnotation(RequestParam.class);
        if (requestParam != null) {
            return requestParam.name().isEmpty() ? parameter.getName() : requestParam.name();
        }
        PathVariable pathVariable = parameter.getAnnotation(PathVariable.class);
        if (pathVariable != null) {
            return pathVariable.name().isEmpty() ? parameter.getName() : pathVariable.name();
        }
        return null;
    }

    private static boolean isNamedLikeTypedInput(String name) {
        int lastWordStart = 0;
        for (int index = name.length() - 1; index > 0; index--) {
            if (Character.isUpperCase(name.charAt(index))) {
                lastWordStart = index;
                break;
            }
        }
        return TYPED_NAME_SUFFIXES.contains(name.substring(lastWordStart).toLowerCase());
    }

    private record SampleRequest(String openAt, String dateOfBirth, String note) {
    }

    private static class SampleController {

        @SuppressWarnings("unused")
        void list(@RequestParam(name = "status") String status, @RequestParam(name = "keyword") String keyword) {
        }
    }
}
