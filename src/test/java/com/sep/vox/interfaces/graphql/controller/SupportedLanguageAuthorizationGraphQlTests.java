package com.sep.vox.interfaces.graphql.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.sep.vox.application.port.input.usecase.supportedlanguage.UpdateSupportedLanguageUseCase;
import com.sep.vox.application.port.input.usecase.supportedlanguage.ViewSupportedLanguageDetailsUseCase;
import com.sep.vox.application.port.input.usecase.supportedlanguage.ViewSupportedLanguagesUseCase;
import com.sep.vox.application.response.input.supportedlanguage.UpdateSupportedLanguageResponse;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.SupportedLanguageDto;
import com.sep.vox.support.GraphQlControllerTest;

/**
 * Runs the {@link SupportedLanguageController} operations through the real schema and {@code @PreAuthorize}, to
 * prove the three role rules: the list admits system admin, school admin and teacher; the detail query admits
 * system admin and school admin; the update mutation is system admin only. The direct-call behaviour stays
 * covered by {@link SupportedLanguageControllerTests}.
 */
@GraphQlControllerTest(SupportedLanguageController.class)
class SupportedLanguageAuthorizationGraphQlTests {

    private static final UUID ID = UUID.randomUUID();

    private static final String LIST_QUERY = "{ supportedLanguages { totalElements } }";
    private static final String DETAIL_QUERY = "{ supportedLanguage(id: \"%s\") { id } }".formatted(ID);
    private static final String UPDATE_MUTATION =
        "mutation { updateSupportedLanguage(id: \"%s\", input: { name: \"English\" }) { supportedLanguageId } }"
            .formatted(ID);

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private ViewSupportedLanguagesUseCase viewSupportedLanguagesUseCase;
    @MockitoBean
    private ViewSupportedLanguageDetailsUseCase viewSupportedLanguageDetailsUseCase;
    @MockitoBean
    private UpdateSupportedLanguageUseCase updateSupportedLanguageUseCase;

    private static SupportedLanguageDto languageWithOnlyId() {
        return new SupportedLanguageDto(ID, null, null, null, true, Instant.EPOCH, Instant.EPOCH);
    }

    private void assertErrorType(String document, ErrorType expectedErrorType) {
        graphQlTester.document(document).execute()
            .errors().expect(error -> error.getErrorType() == expectedErrorType).verify();
    }

    private void verifyNoUseCaseWasCalled() {
        verifyNoInteractions(viewSupportedLanguagesUseCase, viewSupportedLanguageDetailsUseCase,
            updateSupportedLanguageUseCase);
    }

    private void assertListReturnsData() {
        when(viewSupportedLanguagesUseCase.execute(any())).thenReturn(new PageResult<>(List.of(), 1, 20, 0L, 0));

        graphQlTester.document(LIST_QUERY).execute()
            .errors().verify()
            .path("supportedLanguages.totalElements").entity(Integer.class).isEqualTo(0);
    }

    private void assertDetailReturnsData() {
        when(viewSupportedLanguageDetailsUseCase.execute(any())).thenReturn(languageWithOnlyId());

        graphQlTester.document(DETAIL_QUERY).execute()
            .errors().verify()
            .path("supportedLanguage.id").entity(String.class).isEqualTo(ID.toString());
    }

    private static Stream<String> everyOperation() {
        return Stream.of(LIST_QUERY, DETAIL_QUERY, UPDATE_MUTATION);
    }

    @ParameterizedTest
    @MethodSource("everyOperation")
    @WithAnonymousUser
    void every_operation_should_be_unauthorized_without_authentication(String document) {
        assertErrorType(document, ErrorType.UNAUTHORIZED);

        verifyNoUseCaseWasCalled();
    }

    @ParameterizedTest
    @MethodSource("everyOperation")
    @WithMockUser(roles = "STUDENT")
    void every_operation_should_be_forbidden_for_student(String document) {
        assertErrorType(document, ErrorType.FORBIDDEN);

        verifyNoUseCaseWasCalled();
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void detail_should_be_forbidden_for_teacher() {
        assertErrorType(DETAIL_QUERY, ErrorType.FORBIDDEN);

        verifyNoUseCaseWasCalled();
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void update_should_be_forbidden_for_teacher() {
        assertErrorType(UPDATE_MUTATION, ErrorType.FORBIDDEN);

        verifyNoUseCaseWasCalled();
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void update_should_be_forbidden_for_school_admin() {
        assertErrorType(UPDATE_MUTATION, ErrorType.FORBIDDEN);

        verifyNoUseCaseWasCalled();
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void list_should_return_data_for_teacher() {
        assertListReturnsData();
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void list_and_detail_should_return_data_for_school_admin() {
        assertListReturnsData();
        assertDetailReturnsData();
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void detail_should_return_created_and_updated_at_as_date_time() {
        when(viewSupportedLanguageDetailsUseCase.execute(any())).thenReturn(new SupportedLanguageDto(ID, "en",
            "English", null, true, Instant.parse("2026-07-28T02:30:00Z"), Instant.parse("2026-07-29T00:00:00Z")));

        graphQlTester.document("{ supportedLanguage(id: \"%s\") { createdAt updatedAt } }".formatted(ID))
            .execute()
            .errors().verify()
            .path("supportedLanguage.createdAt").entity(String.class).isEqualTo("2026-07-28T02:30:00Z")
            .path("supportedLanguage.updatedAt").entity(String.class).isEqualTo("2026-07-29T00:00:00Z");
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void list_detail_and_update_should_return_data_for_system_admin() {
        when(updateSupportedLanguageUseCase.execute(any())).thenReturn(new UpdateSupportedLanguageResponse(ID));
        assertListReturnsData();
        assertDetailReturnsData();

        graphQlTester.document(UPDATE_MUTATION).execute()
            .errors().verify()
            .path("updateSupportedLanguage.supportedLanguageId").entity(String.class).isEqualTo(ID.toString());
    }
}
