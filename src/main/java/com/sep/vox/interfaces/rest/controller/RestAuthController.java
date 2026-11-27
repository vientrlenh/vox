package com.sep.vox.interfaces.rest.controller;

import com.sep.vox.application.port.input.command.auth.GoogleTokenLoginCommand;
import com.sep.vox.application.port.input.command.auth.LoginCommand;
import com.sep.vox.application.port.input.command.auth.LogoutCommand;
import com.sep.vox.application.port.input.command.auth.RefreshCommand;
import com.sep.vox.application.port.input.command.registration.RegisterBySelfDeclaredCommand;
import com.sep.vox.application.port.input.command.registration.RegisterFromSchoolDirectoryCommand;
import com.sep.vox.application.port.input.command.auth.ResetPasswordCommand;
import com.sep.vox.application.port.input.command.auth.SendResetPasswordOtpCommand;
import com.sep.vox.application.port.input.command.auth.SetUpPasswordCommand;
import com.sep.vox.application.port.input.command.registration.VerifyRegisterFormOtpCommand;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sep.vox.application.port.input.usecase.auth.GoogleTokenLoginUseCase;
import com.sep.vox.application.port.input.usecase.auth.LoginUseCase;
import com.sep.vox.application.port.input.usecase.auth.LogoutUseCase;
import com.sep.vox.application.port.input.usecase.auth.RefreshUseCase;
import com.sep.vox.application.port.input.usecase.auth.SetUpPasswordUseCase;
import com.sep.vox.application.port.input.usecase.auth.SendResetPasswordOtpUseCase;
import com.sep.vox.application.port.input.usecase.auth.ResetPasswordUseCase;
import com.sep.vox.application.port.input.usecase.registration.RegisterBySelfDeclaredUseCase;

import com.sep.vox.application.port.input.usecase.registration.RegisterFromSchoolDirectoryUseCase;
import com.sep.vox.application.port.input.usecase.registration.VerifyRegisterFormOtpUseCase;
import com.sep.vox.interfaces.shared.HttpCookieProvider;
import com.sep.vox.domain.model.devicesession.SessionPlatform;
import com.sep.vox.application.response.input.auth.LoginResponse;
import com.sep.vox.application.response.input.auth.RefreshResponse;
import com.sep.vox.application.response.input.registration.RegisterFromSchoolDirectoryResponse;
import com.sep.vox.interfaces.rest.dto.request.auth.GoogleTokenLoginRequest;
import com.sep.vox.interfaces.rest.dto.request.auth.LoginRequest;
import com.sep.vox.interfaces.rest.dto.request.auth.DeviceIdRequest;
import com.sep.vox.interfaces.rest.dto.request.registration.RegisterBySelfDeclaredRequest;
import com.sep.vox.interfaces.rest.dto.request.registration.RegisterFromSchoolDirectoryRequest;
import com.sep.vox.interfaces.rest.dto.request.auth.ResetPasswordRequest;
import com.sep.vox.interfaces.rest.dto.request.auth.SendResetPasswordOtpRequest;
import com.sep.vox.interfaces.rest.dto.request.auth.SetUpPasswordRequest;

import com.sep.vox.interfaces.rest.dto.request.registration.VerifyRegisterFormOtpRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;

import com.sep.vox.interfaces.shared.IpAddressReceiver;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;



@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class RestAuthController {
    
    private final LoginUseCase loginUseCase;
    private final RegisterFromSchoolDirectoryUseCase registerFromSchoolDirectoryUseCase;
    private final SetUpPasswordUseCase setUpPasswordUseCase;
    private final RefreshUseCase refreshUseCase;
    private final LogoutUseCase logoutUseCase;
    private final SendResetPasswordOtpUseCase sendResetPasswordOtpUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;
    private final RegisterBySelfDeclaredUseCase registerBySelfDeclaredUseCase;
    private final VerifyRegisterFormOtpUseCase verifyRegisterFormOtpUseCase;
    private final GoogleTokenLoginUseCase googleTokenLoginUseCase;
    private final HttpCookieProvider httpCookieProvider;

    private static final String REFRESH_TOKEN_COOKIE_KEY = "refresh_token";
    private static final long REFRESH_TOKEN_COOKIE_TTL_SECONDS = 259200L;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        String ipAddress = IpAddressReceiver.getClientIp(servletRequest);
        String userAgent = servletRequest.getHeader("User-Agent");

        LoginCommand command = LoginRequest.toCommand(request, ipAddress, userAgent);
        LoginResponse data = loginUseCase.execute(command);
        httpCookieProvider.setCookie(servletResponse, REFRESH_TOKEN_COOKIE_KEY, data.refreshToken(), REFRESH_TOKEN_COOKIE_TTL_SECONDS);

        ApiResponse<LoginResponse> response = ApiResponse.success("Login successful", new LoginResponse(data.accessToken(), null, data.roles()));

        return ResponseEntity.ok(response);
    }


    @PostMapping("/register-school-directory")
    public ResponseEntity<ApiResponse<RegisterFromSchoolDirectoryResponse>> registerSchoolDirectory(@Valid @RequestBody RegisterFromSchoolDirectoryRequest request) {
        RegisterFromSchoolDirectoryCommand command = RegisterFromSchoolDirectoryRequest.toCommand(request);
        RegisterFromSchoolDirectoryResponse data = registerFromSchoolDirectoryUseCase.execute(command);
        ApiResponse<RegisterFromSchoolDirectoryResponse> response = ApiResponse.success("Request successful", data);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/register-self-declared")
    public ResponseEntity<ApiResponse<Object>> registerSelfDeclared(@Valid @RequestBody RegisterBySelfDeclaredRequest request) {
        RegisterBySelfDeclaredCommand command = RegisterBySelfDeclaredRequest.toCommand(request);
        registerBySelfDeclaredUseCase.execute(command);
        ApiResponse<Object> response = ApiResponse.success("Registration form submitted successfully. Please wait for the administrator to approve it");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @PostMapping("/verify-registration")
    public ResponseEntity<ApiResponse<Object>> verifyRegistrationOtp(@Valid @RequestBody VerifyRegisterFormOtpRequest request) {
        VerifyRegisterFormOtpCommand command = VerifyRegisterFormOtpRequest.toCommand(request);
        verifyRegisterFormOtpUseCase.execute(command);
        ApiResponse<Object> response = ApiResponse.success("Registration request verified");
        return ResponseEntity.ok(response);
    }


    @PostMapping("/setup-password")
    public ResponseEntity<ApiResponse<Object>> setUpPassword(@Valid @RequestBody SetUpPasswordRequest request) {
        SetUpPasswordCommand command = SetUpPasswordRequest.toCommand(request);
        setUpPasswordUseCase.execute(command);
        ApiResponse<Object> response = ApiResponse.success("Password set successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshResponse>> refresh(@Valid @RequestBody DeviceIdRequest request, @CookieValue(name = REFRESH_TOKEN_COOKIE_KEY, required = true) String token, HttpServletResponse servletResponse) {
        RefreshCommand command = DeviceIdRequest.toRefreshCommand(request, token);
        RefreshResponse data = refreshUseCase.execute(command);
        httpCookieProvider.setCookie(servletResponse, REFRESH_TOKEN_COOKIE_KEY, data.refreshToken(), REFRESH_TOKEN_COOKIE_TTL_SECONDS);
        ApiResponse<RefreshResponse> response = ApiResponse.success("Request successful", new RefreshResponse(
            data.accessToken(), null
        ));
        return ResponseEntity.ok(response);
    }


    @PostMapping("/reset-password-otp")
    public ResponseEntity<ApiResponse<Object>> sendResetPasswordOtp(@Valid @RequestBody SendResetPasswordOtpRequest request) {
        SendResetPasswordOtpCommand command = SendResetPasswordOtpRequest.toCommand(request);
        sendResetPasswordOtpUseCase.execute(command);
        ApiResponse<Object> response = ApiResponse.success("Password reset OTP has been sent");
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/reset-password")
    public ResponseEntity<ApiResponse<Object>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        ResetPasswordCommand command = ResetPasswordRequest.toCommand(request);
        resetPasswordUseCase.execute(command);
        ApiResponse<Object> response = ApiResponse.success("Password changed successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * Google login for NATIVE apps (Flutter, WPF) -- the counterpart of
     * {@link #startGoogleLogin}, which can only be used from a browser.
     *
     * <p>Why the redirect flow is not shared: that flow ends with a redirect to EXACTLY ONE preconfigured address
     * ({@code app.frontend.oauth2-url}), i.e. the web page. A native app has nowhere to receive that redirect.
     * Letting the client pass its own return address would open an open redirect that CARRIES the access token
     * on the query string -- the most dangerous thing that can go wrong. So the native app obtains the ID token on
     * the user's device itself (SDK on mobile, PKCE + loopback on desktop) and exchanges it for a session here.
     *
     * <p><b>The refresh token is returned in BOTH places</b>: in the body and in the cookie. This is not redundant --
     * {@code POST /api/v1/auth/refresh} reads the refresh token with {@code @CookieValue} and has NO
     * field in the body to replace it, so returning only the body would leave the client holding a string it
     * cannot use for anything. Both clients already keep the cookie (Flutter has PersistCookieJar, WPF reads
     * Set-Cookie by hand), while the copy in the body is for storing in the operating system's secure storage.
     * Unlike {@link #login}, where refreshToken is forced to null because the browser must not be able to read it.
     */
    @PostMapping("/oauth2/google/token")
    public ResponseEntity<ApiResponse<LoginResponse>> googleTokenLogin(
        @Valid @RequestBody GoogleTokenLoginRequest request,
        HttpServletRequest servletRequest,
        HttpServletResponse servletResponse
    ) {
        String ipAddress = IpAddressReceiver.getClientIp(servletRequest);
        String userAgent = servletRequest.getHeader("User-Agent");

        GoogleTokenLoginCommand command = GoogleTokenLoginRequest.toCommand(request, ipAddress, userAgent);
        LoginResponse data = googleTokenLoginUseCase.execute(command);
        httpCookieProvider.setCookie(servletResponse, REFRESH_TOKEN_COOKIE_KEY, data.refreshToken(), REFRESH_TOKEN_COOKIE_TTL_SECONDS);

        ApiResponse<LoginResponse> response = ApiResponse.success("Login successful", data);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/oauth2/google/start")
    public void startGoogleLogin(
        @RequestParam(name = "deviceId", required = true) String deviceId,
        @RequestParam(name = "deviceName", required = true) String deviceName,
        @RequestParam(name = "platform", required = true) SessionPlatform platform,
        HttpServletRequest request,
        HttpServletResponse response
    ) throws IOException {
        HttpSession session = request.getSession();
        session.setAttribute("oauth2_device_id", deviceId);
        session.setAttribute("oauth2_device_name", deviceName);
        session.setAttribute("oauth2_platform", platform);

        response.sendRedirect("/oauth2/authorization/google");
    }

    /**
     * Revokes the device session and clears the refresh_token cookie.
     *
     * <p>Deliberately has NO {@code @PreAuthorize}, even though that sounds backwards for a logout endpoint.
     * {@code JwtAuthenticationFilter} swallows every token error and lets the request continue as anonymous,
     * so an expired access token (15 minutes) would be blocked BEFORE reaching here -- yet an abandoned session,
     * exactly the one that most needs logging out, is always in that state. The real proof for revoking is the
     * refresh_token cookie, not the access token; if a still-valid access token is present,
     * {@link LogoutUseCase} also cleans up the other sessions of the same device.
     *
     * <p>{@code required = false} and ALWAYS returns 200: the client deletes its tokens right after this call,
     * so any way of failing here would leave the client without a token while the server keeps the session
     * alive. Having no cookie is also a valid logout.
     *
     * <p>This endpoint reads a cookie, so it MUST be in the CSRF matcher -- see
     * {@code SecurityConfig#CSRF_PROTECTED_API_PATHS}.
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Object>> logout(@Valid @RequestBody DeviceIdRequest request, @CookieValue(name = REFRESH_TOKEN_COOKIE_KEY, required = false) String token, HttpServletResponse servletResponse) {
        LogoutCommand command = DeviceIdRequest.toLogoutCommand(request, token);
        logoutUseCase.execute(command);
        httpCookieProvider.clearCookie(servletResponse, REFRESH_TOKEN_COOKIE_KEY);

        ApiResponse<Object> response = ApiResponse.success("Logout successful");

        return ResponseEntity.ok(response);
    }
}
