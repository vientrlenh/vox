package com.sep.vox.interfaces.rest.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sep.vox.application.port.input.command.DeleteSupportedLanguageCommand;
import com.sep.vox.application.port.input.usecase.supportedlanguage.CreateSupportedLanguageUseCase;
import com.sep.vox.application.port.input.usecase.supportedlanguage.DeleteSupportedLanguageUseCase;
import com.sep.vox.application.response.input.supportedlanguage.CreateSupportedLanguageResponse;
import com.sep.vox.interfaces.rest.dto.request.CreateSupportedLanguageRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;
import com.sep.vox.interfaces.rest.mapper.CreateSupportedLanguageCommandMapper;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/supported-languages")
public class SupportedLanguageController {

    private final CreateSupportedLanguageUseCase createSupportedLanguageUseCase;
    private final DeleteSupportedLanguageUseCase deleteSupportedLanguageUseCase;

    public SupportedLanguageController(
            CreateSupportedLanguageUseCase createSupportedLanguageUseCase,
            DeleteSupportedLanguageUseCase deleteSupportedLanguageUseCase) {
        this.createSupportedLanguageUseCase = createSupportedLanguageUseCase;
        this.deleteSupportedLanguageUseCase = deleteSupportedLanguageUseCase;
    }

    @PostMapping
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<CreateSupportedLanguageResponse>> create(
            @Valid @RequestBody CreateSupportedLanguageRequest request) {
        var command = CreateSupportedLanguageCommandMapper.fromRequest(request);
        var data = createSupportedLanguageUseCase.execute(command);
        var response = ApiResponse.success("Tạo ngôn ngữ thành công", data);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        deleteSupportedLanguageUseCase.execute(new DeleteSupportedLanguageCommand(id));
        var response = ApiResponse.<Void>success("Xóa ngôn ngữ thành công");
        return ResponseEntity.ok(response);
    }
}
