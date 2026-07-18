package com.sep.vox.interfaces.rest.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sep.vox.application.port.input.usecase.devicesession.RegisterPushTokenUseCase;
import com.sep.vox.interfaces.rest.dto.request.RegisterPushTokenRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;
import com.sep.vox.interfaces.rest.mapper.RegisterPushTokenCommandMapper;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/devices")
public class DeviceController {

    private final RegisterPushTokenUseCase registerPushTokenUseCase;

    public DeviceController(RegisterPushTokenUseCase registerPushTokenUseCase) {
        this.registerPushTokenUseCase = registerPushTokenUseCase;
    }

    @PutMapping("/push-token")
    public ResponseEntity<ApiResponse<Object>> registerPushToken(@Valid @RequestBody RegisterPushTokenRequest request) {
        var command = RegisterPushTokenCommandMapper.fromRequest(request);
        registerPushTokenUseCase.execute(command);
        var response = ApiResponse.success("Đã lưu push token thành công");
        return ResponseEntity.ok(response);
    }
}
