package com.sep.vox.interfaces.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.sep.vox.application.port.input.command.CreateNotificationDeviceCommand;
import com.sep.vox.application.port.input.usecase.notification.CreateNotificationDeviceUseCase;
import com.sep.vox.application.port.input.usecase.notification.DeleteNotificationDeviceUseCase;
import com.sep.vox.application.port.input.usecase.notification.MarkAllNotificationsAsReadUseCase;
import com.sep.vox.application.port.input.usecase.notification.MarkNotificationAsReadUseCase;
import com.sep.vox.domain.model.notification.NotificationDevicePlatform;
import com.sep.vox.support.RestControllerTest;

@RestControllerTest(NotificationController.class)
class NotificationControllerTests {

    private static final String DEVICES_URI = "/api/v1/notifications/devices";

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private CreateNotificationDeviceUseCase createNotificationDeviceUseCase;
    @MockitoBean
    private DeleteNotificationDeviceUseCase deleteNotificationDeviceUseCase;
    @MockitoBean
    private MarkNotificationAsReadUseCase markNotificationAsReadUseCase;
    @MockitoBean
    private MarkAllNotificationsAsReadUseCase markAllNotificationsAsReadUseCase;

    @Test
    @WithMockUser
    void createDevice_should_pass_typed_platform_to_use_case() {
        MvcTestResult result = postDevice("{\"deviceId\":\"device-1\",\"platform\":\"ANDROID\",\"installationId\":\"fid-1\"}");

        assertThat(result).hasStatusOk();
        verify(createNotificationDeviceUseCase).execute(
            new CreateNotificationDeviceCommand("device-1", NotificationDevicePlatform.ANDROID, "fid-1"));
    }

    @Test
    @WithMockUser
    void createDevice_should_bind_lower_case_platform() {
        MvcTestResult result = postDevice("{\"deviceId\":\"device-1\",\"platform\":\"ios\",\"installationId\":\"fid-1\"}");

        assertThat(result).hasStatusOk();
        verify(createNotificationDeviceUseCase).execute(
            new CreateNotificationDeviceCommand("device-1", NotificationDevicePlatform.IOS, "fid-1"));
    }

    @Test
    @WithMockUser
    void createDevice_should_answer_400_naming_platform_and_accepted_values_when_platform_is_unknown() {
        MvcTestResult result = postDevice("{\"deviceId\":\"device-1\",\"platform\":\"WINDOWS\",\"installationId\":\"fid-1\"}");

        assertThat(result).hasStatus(400).bodyJson().extractingPath("$.errors.platform").asString()
            .contains("WEB", "ANDROID", "IOS").doesNotContain("com.sep");
        verifyNoInteractions(createNotificationDeviceUseCase);
    }

    @Test
    @WithMockUser
    void createDevice_should_answer_400_naming_platform_when_platform_is_missing() {
        MvcTestResult result = postDevice("{\"deviceId\":\"device-1\",\"installationId\":\"fid-1\"}");

        assertThat(result).hasStatus(400).bodyJson().extractingPath("$.errors.platform").isNotNull();
        verifyNoInteractions(createNotificationDeviceUseCase);
    }

    @Test
    @WithAnonymousUser
    void createDevice_should_answer_401_for_anonymous_caller() {
        MvcTestResult result = postDevice("{\"deviceId\":\"device-1\",\"platform\":\"WEB\",\"installationId\":\"fid-1\"}");

        assertThat(result).hasStatus(401);
        verifyNoInteractions(createNotificationDeviceUseCase);
    }

    private MvcTestResult postDevice(String json) {
        return mvc.post().uri(DEVICES_URI).contentType(MediaType.APPLICATION_JSON).content(json).exchange();
    }
}
