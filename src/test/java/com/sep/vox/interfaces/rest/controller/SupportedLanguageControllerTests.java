package com.sep.vox.interfaces.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.sep.vox.application.port.input.command.CreateSupportedLanguageCommand;
import com.sep.vox.application.port.input.command.DeleteSupportedLanguageCommand;
import com.sep.vox.application.port.input.usecase.supportedlanguage.CreateSupportedLanguageUseCase;
import com.sep.vox.application.port.input.usecase.supportedlanguage.DeleteSupportedLanguageUseCase;
import com.sep.vox.application.response.input.supportedlanguage.CreateSupportedLanguageResponse;
import com.sep.vox.support.RestControllerTest;

@RestControllerTest(SupportedLanguageController.class)
class SupportedLanguageControllerTests {

    private static final String CREATE_BODY =
        "{\"code\": \"EN\", \"name\": \"English\", \"description\": \"Main language\"}";

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private CreateSupportedLanguageUseCase createSupportedLanguageUseCase;
    @MockitoBean
    private DeleteSupportedLanguageUseCase deleteSupportedLanguageUseCase;

    private MockMvcTester.MockMvcRequestBuilder createRequest(String body) {
        return mvc.post().uri("/api/v1/supported-languages")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body);
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void create_should_return_created_response() {
        UUID supportedLanguageId = UUID.randomUUID();
        when(createSupportedLanguageUseCase.execute(new CreateSupportedLanguageCommand("EN", "English", "Main language")))
            .thenReturn(new CreateSupportedLanguageResponse(supportedLanguageId));

        MvcTestResult result = createRequest(CREATE_BODY).exchange();

        assertThat(result).hasStatus(201).bodyJson()
            .extractingPath("$.data.supportedLanguageId").isEqualTo(supportedLanguageId.toString());
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void create_should_return_bad_request_when_code_is_longer_than_ten_characters() {
        MvcTestResult result = createRequest("{\"code\": \"ABCDEFGHIJK\", \"name\": \"English\"}").exchange();

        assertThat(result).hasStatus(400);
        verifyNoInteractions(createSupportedLanguageUseCase);
    }

    @Test
    void create_should_return_401_without_authenticated_user() {
        MvcTestResult result = createRequest(CREATE_BODY).exchange();

        assertThat(result).hasStatus(401);
        verifyNoInteractions(createSupportedLanguageUseCase);
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void create_should_return_403_for_role_other_than_system_admin() {
        MvcTestResult result = createRequest(CREATE_BODY).exchange();

        assertThat(result).hasStatus(403);
        verifyNoInteractions(createSupportedLanguageUseCase);
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void delete_should_return_ok_response_for_system_admin() {
        UUID id = UUID.randomUUID();

        MvcTestResult result = mvc.delete().uri("/api/v1/supported-languages/{id}", id).exchange();

        assertThat(result).hasStatusOk().bodyJson()
            .extractingPath("$.message").isEqualTo("Xóa ngôn ngữ thành công");
        verify(deleteSupportedLanguageUseCase).execute(new DeleteSupportedLanguageCommand(id));
    }

    @Test
    void delete_should_return_401_without_authenticated_user() {
        MvcTestResult result = mvc.delete().uri("/api/v1/supported-languages/{id}", UUID.randomUUID()).exchange();

        assertThat(result).hasStatus(401);
        verifyNoInteractions(deleteSupportedLanguageUseCase);
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void delete_should_return_403_for_role_other_than_system_admin() {
        MvcTestResult result = mvc.delete().uri("/api/v1/supported-languages/{id}", UUID.randomUUID()).exchange();

        assertThat(result).hasStatus(403);
        verifyNoInteractions(deleteSupportedLanguageUseCase);
    }
}
