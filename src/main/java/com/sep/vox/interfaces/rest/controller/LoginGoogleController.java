package com.sep.vox.interfaces.rest.controller;


import com.sep.vox.application.port.input.usecase.auth.LoginGoogleUseCase;
import com.sep.vox.application.response.input.auth.LoginResponse;
import com.sep.vox.interfaces.rest.dto.request.LoginGoogleRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;
import com.sep.vox.interfaces.rest.mapper.LoginGoogleCommandMapper;
import com.sep.vox.interfaces.shared.IpAddressReceiver;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class LoginGoogleController {

    private final LoginGoogleUseCase loginGoogleUseCase;

    public LoginGoogleController(LoginGoogleUseCase loginGoogleUseCase) {
        this.loginGoogleUseCase = loginGoogleUseCase;
    }


    // ====LOGIN Google==============
    @Operation(
            summary = "Đăng nhập bằng Google",
            description = "Xác thực idToken từ Google và cấp Access Token & Session Token cho hệ thống VOX"
    )
    @PostMapping("/google")
    public ResponseEntity<ApiResponse<LoginResponse>> loginWithGoogle(
            @Valid @RequestBody LoginGoogleRequest request,
            HttpServletRequest httpRequest) {

        // 1. Trích xuất thông tin mạng
        String ipAddress = IpAddressReceiver.getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader(org.springframework.http.HttpHeaders.USER_AGENT);

        // 2. Đóng gói Command thông qua Mapper (Controller giờ rất gọn gàng)
        var command = LoginGoogleCommandMapper.fromRequest(request, ipAddress, userAgent);

        // 3. Thực thi Use Case
        LoginResponse response = loginGoogleUseCase.execute(command);

        // 4. Trả về Response
        return ResponseEntity.ok(ApiResponse.success("Đăng nhập Google thành công", response));
    }
}
