package com.sep.vox.interfaces.rest.controller;

import com.sep.vox.application.port.input.command.registration.ApproveRegisterFormCommand;
import com.sep.vox.application.port.input.command.registration.RejectRegisterFormCommand;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sep.vox.application.port.input.usecase.registration.ApproveRegisterFormUseCase;
import com.sep.vox.application.port.input.usecase.registration.RejectRegisterFormUseCase;
import com.sep.vox.interfaces.rest.dto.request.registration.ApproveRegisterFormRequest;
import com.sep.vox.interfaces.rest.dto.request.registration.RejectRegisterFormRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/register-forms")
@RequiredArgsConstructor
public class RestRegisterFormController {

    private final ApproveRegisterFormUseCase approveRegisterFormUseCase;
    private final RejectRegisterFormUseCase rejectRegisterFormUseCase;

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Object>> approve(@PathVariable("id") UUID id, @Valid @RequestBody ApproveRegisterFormRequest request) {
        ApproveRegisterFormCommand command = ApproveRegisterFormRequest.toCommand(id, request);
        approveRegisterFormUseCase.execute(command);
        ApiResponse<Object> response = ApiResponse.success("Registration form approved");
        return ResponseEntity.ok(response);
    }


    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Object>> reject(@PathVariable("id") UUID id, @Valid @RequestBody RejectRegisterFormRequest request) {
        RejectRegisterFormCommand command = RejectRegisterFormRequest.toCommand(id, request);
        rejectRegisterFormUseCase.execute(command);
        ApiResponse<Object> response = ApiResponse.success("Registration form rejected successfully");
        return ResponseEntity.ok(response);
    }
}
