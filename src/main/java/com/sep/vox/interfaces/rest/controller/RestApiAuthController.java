package com.sep.vox.interfaces.rest.controller;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sep.vox.application.port.input.command.GoogleTokenLoginCommand;
import com.sep.vox.application.port.input.command.LoginCommand;
import com.sep.vox.application.port.input.command.LogoutCommand;
import com.sep.vox.application.port.input.command.RefreshCommand;
import com.sep.vox.application.port.input.command.RegisterBySelfDeclaredCommand;
import com.sep.vox.application.port.input.command.RegisterFromSchoolDirectoryCommand;
import com.sep.vox.application.port.input.command.ResetPasswordCommand;
import com.sep.vox.application.port.input.command.SendResetPasswordOtpCommand;
import com.sep.vox.application.port.input.command.SetUpPasswordCommand;
import com.sep.vox.application.port.input.command.VerifyRegisterFormOtpCommand;
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
import com.sep.vox.application.response.input.auth.LoginResponse;
import com.sep.vox.application.response.input.auth.RefreshResponse;
import com.sep.vox.application.response.input.registration.RegisterFromSchoolDirectoryResponse;
import com.sep.vox.interfaces.rest.dto.request.GoogleTokenLoginRequest;
import com.sep.vox.interfaces.rest.dto.request.LoginRequest;
import com.sep.vox.interfaces.rest.dto.request.LogoutRequest;
import com.sep.vox.interfaces.rest.dto.request.RefreshRequest;
import com.sep.vox.interfaces.rest.dto.request.RegisterBySelfDeclaredRequest;
import com.sep.vox.interfaces.rest.dto.request.RegisterFromSchoolDirectoryRequest;
import com.sep.vox.interfaces.rest.dto.request.ResetPasswordRequest;
import com.sep.vox.interfaces.rest.dto.request.SendResetPasswordOtpRequest;
import com.sep.vox.interfaces.rest.dto.request.SetUpPasswordRequest;

import com.sep.vox.interfaces.rest.dto.request.VerifyRegisterFormOtpRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor 
public class RestApiAuthController {
    
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

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request, HttpServletRequest req, HttpServletResponse res) {
        LoginCommand command = LoginRequest.toCommand(request, req, res);
        LoginResponse data = loginUseCase.execute(command);

        ApiResponse<LoginResponse> response = ApiResponse.success("Login successfully", data);
        return ResponseEntity.ok(response);
    }


    @PostMapping("/register/school-directory")
    public ResponseEntity<ApiResponse<RegisterFromSchoolDirectoryResponse>> registerFromSchoolDirectory(@Valid @RequestBody RegisterFromSchoolDirectoryRequest request) {
        RegisterFromSchoolDirectoryCommand command = RegisterFromSchoolDirectoryRequest.toCommand(request);
        RegisterFromSchoolDirectoryResponse data = registerFromSchoolDirectoryUseCase.execute(command);
        ApiResponse<RegisterFromSchoolDirectoryResponse> response = ApiResponse.success("Registration form has been sent successfully", data);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/register/self-declared")
    public ResponseEntity<ApiResponse<Void>> registerBySelfDeclared(@Valid @RequestBody RegisterBySelfDeclaredRequest request) {
        RegisterBySelfDeclaredCommand command = RegisterBySelfDeclaredRequest.toCommand(request);
        registerBySelfDeclaredUseCase.execute(command);
        ApiResponse<Void> response = ApiResponse.success("Registration form has been sent successfully. Please wait for approval from admin");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @PostMapping("/register/verify")
    public ResponseEntity<ApiResponse<Void>> verifyRegistrationOtp(@Valid @RequestBody VerifyRegisterFormOtpRequest request) {
        VerifyRegisterFormOtpCommand command = VerifyRegisterFormOtpRequest.toCommand(request);
        verifyRegisterFormOtpUseCase.execute(command);
        ApiResponse<Void> response = ApiResponse.success("Registration form has been verified");
        return ResponseEntity.ok(response);
    }


    @PostMapping("/setup-password")
    public ResponseEntity<ApiResponse<Void>> setUpPassword(@Valid @RequestBody SetUpPasswordRequest request) {
        SetUpPasswordCommand command = SetUpPasswordRequest.toCommand(request);
        setUpPasswordUseCase.execute(command);
        ApiResponse<Void> response = ApiResponse.success("Password set up successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshResponse>> refresh(@Valid @RequestBody RefreshRequest request, HttpServletRequest req, HttpServletResponse res) {
        RefreshCommand command = RefreshRequest.toCommand(request, req, res);
        RefreshResponse data = refreshUseCase.execute(command);
        ApiResponse<RefreshResponse> response = ApiResponse.success("Refresh successfully", data);
        return ResponseEntity.ok(response);
    }


    @PostMapping("/reset-password/otp")
    public ResponseEntity<ApiResponse<Void>> sendResetPasswordOtp(@Valid @RequestBody SendResetPasswordOtpRequest request) {
        SendResetPasswordOtpCommand command = SendResetPasswordOtpRequest.toCommand(request);
        sendResetPasswordOtpUseCase.execute(command);
        ApiResponse<Void> response = ApiResponse.success("Password reset OTP has been sent successfully");
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        ResetPasswordCommand command = ResetPasswordRequest.toCommand(request);
        resetPasswordUseCase.execute(command);
        ApiResponse<Void> response = ApiResponse.success("Password has been reseted successfully");
        return ResponseEntity.ok(response);
    }
    @PostMapping("/oauth2/google/token")
    public ResponseEntity<ApiResponse<LoginResponse>> googleTokenLogin(
        @Valid @RequestBody GoogleTokenLoginRequest request,
        HttpServletRequest req,
        HttpServletResponse res
    ) {
        GoogleTokenLoginCommand command = GoogleTokenLoginRequest.toCommand(request, req, res);
        LoginResponse data = googleTokenLoginUseCase.execute(command);

        ApiResponse<LoginResponse> response = ApiResponse.success("Login successfully", data);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/oauth2/google/start")
    public void startGoogleLogin(
        @RequestParam(name = "deviceId", required = true) String deviceId,
        @RequestParam(name = "deviceName", required = true) String deviceName,
        @RequestParam(name = "platform", required = true) String platform,
        HttpServletRequest request,
        HttpServletResponse response
    ) throws IOException {
        var session = request.getSession();
        session.setAttribute("oauth2_device_id", deviceId);
        session.setAttribute("oauth2_device_name", deviceName);
        session.setAttribute("oauth2_platform", platform);

        response.sendRedirect("/oauth2/authorization/google");
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody LogoutRequest request, HttpServletRequest req, HttpServletResponse res) {
        LogoutCommand command = LogoutRequest.toCommand(request, req, res);
        logoutUseCase.execute(command);
        ApiResponse<Void> response = ApiResponse.success("User logged out successfully");

        return ResponseEntity.ok(response);
    }
}
