package com.sep.vox.interfaces.rest.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sep.vox.application.port.input.usecase.registration.ApproveRegisterFormUseCase;
import com.sep.vox.application.port.input.usecase.registration.RejectRegisterFormUseCase;
import com.sep.vox.interfaces.rest.dto.request.ApproveRegisterFormRequest;
import com.sep.vox.interfaces.rest.dto.request.RejectRegisterFormRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;
import com.sep.vox.interfaces.rest.mapper.ApproveRegisterFormCommandMapper;
import com.sep.vox.interfaces.rest.mapper.RejectRegisterFormCommandMapper;

import jakarta.validation.Valid;

@RestController("restRegisterFormController")
@RequestMapping("/api/v1/register-forms")
public class RegisterFormController {

    private final ApproveRegisterFormUseCase approveRegisterFormUseCase;
    private final RejectRegisterFormUseCase rejectRegisterFormUseCase;

    public RegisterFormController(ApproveRegisterFormUseCase approveRegisterFormUseCase, RejectRegisterFormUseCase rejectRegisterFormUseCase) {
        this.approveRegisterFormUseCase = approveRegisterFormUseCase;
        this.rejectRegisterFormUseCase = rejectRegisterFormUseCase;
    }
    
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Object>> approve(@PathVariable("id") UUID id, @Valid @RequestBody ApproveRegisterFormRequest request) {
        var command = ApproveRegisterFormCommandMapper.fromRequest(id, request);
        approveRegisterFormUseCase.execute(command);
        var response = ApiResponse.success("Đơn đăng ký đã được phê duyệt");
        return ResponseEntity.ok(response);
    }


    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Object>> reject(@PathVariable("id") UUID id, @Valid @RequestBody RejectRegisterFormRequest request) {
        var command = RejectRegisterFormCommandMapper.fromRequest(id, request);
        rejectRegisterFormUseCase.execute(command);
        var response = ApiResponse.success("Đơn đăng ký đã từ chối thành công");
        return ResponseEntity.ok(response);
    }
}
