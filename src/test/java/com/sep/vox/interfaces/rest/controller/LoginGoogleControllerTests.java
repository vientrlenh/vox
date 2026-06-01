package com.sep.vox.interfaces.rest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep.vox.application.port.input.usecase.auth.LoginGoogleUseCase;
import com.sep.vox.application.port.output.AuthTokenPort;
import com.sep.vox.application.response.input.auth.LoginResponse;
import com.sep.vox.interfaces.rest.dto.request.ClientDeviceRequest;
import com.sep.vox.interfaces.rest.dto.request.LoginGoogleRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = LoginGoogleController.class)
@AutoConfigureMockMvc(addFilters = false) // QUAN TRỌNG: Phải là false để tắt Spring Security tạm thời
public class LoginGoogleControllerTests {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 1. LÀM GIẢ USE CASE CỦA CONTROLLER
    @MockitoBean
    private LoginGoogleUseCase loginGoogleUseCase;

    // 2. LÀM GIẢ CÁC BEAN MÀ SPRING SECURITY ĐÒI HỎI LÚC KHỞI ĐỘNG
    @MockitoBean
    private AuthTokenPort authTokenPort;

    @MockitoBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;


    @Test
    @DisplayName("Thành công: Gọi API /google trả về HTTP 200 và đúng cấu trúc JSON")
    void loginWithGoogle_ValidRequest_ReturnsOk() throws Exception {
        // Given: Request hợp lệ
        ClientDeviceRequest deviceRequest = new ClientDeviceRequest("device-123", "Chrome Tester", "WEB");
        LoginGoogleRequest request = new LoginGoogleRequest("fake-google-id-token", deviceRequest);

        // Given: Dữ liệu trả về giả
        LoginResponse mockResponse = new LoginResponse(
                "mock-access-token",
                "mock-refresh-token",
                List.of("SCHOOL_ADMIN")
        );
        when(loginGoogleUseCase.execute(any())).thenReturn(mockResponse);

        // When & Then: Gọi API
        mockMvc.perform(post("/api/v1/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("User-Agent", "Mozilla/5.0 Test")
                        .with(requestPostProcessor -> {
                            requestPostProcessor.setRemoteAddr("192.168.1.1");
                            return requestPostProcessor;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Đăng nhập Google thành công"))
                .andExpect(jsonPath("$.data.accessToken").value("mock-access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("mock-refresh-token"))
                .andExpect(jsonPath("$.data.roles[0]").value("SCHOOL_ADMIN"));
    }

    @Test
    @DisplayName("Lỗi Validation: Gọi API với Request thiếu idToken trả về HTTP 400")
    void loginWithGoogle_MissingIdToken_ReturnsBadRequest() throws Exception {
        // Given: Request thiếu idToken (chuỗi rỗng)
        ClientDeviceRequest deviceRequest = new ClientDeviceRequest("device-123", "Chrome Tester", "WEB");
        LoginGoogleRequest request = new LoginGoogleRequest("", deviceRequest);

        // When & Then: Spring Boot Validation (@Valid) sẽ chặn lại và trả về 400
        mockMvc.perform(post("/api/v1/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}