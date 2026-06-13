package com.sep.vox.interfaces.rest.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sep.vox.application.port.input.usecase.supportedlanguage.CreateSupportedLanguageUseCase;
import com.sep.vox.application.response.input.supportedlanguage.CreateSupportedLanguageResponse;
import com.sep.vox.interfaces.rest.dto.request.CreateSupportedLanguageRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;
import com.sep.vox.interfaces.rest.mapper.CreateSupportedLanguageCommandMapper;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/supported-languages")
public class SupportedLanguageController {

    private final CreateSupportedLanguageUseCase createSupportedLanguageUseCase;

    public SupportedLanguageController(CreateSupportedLanguageUseCase createSupportedLanguageUseCase) {
        this.createSupportedLanguageUseCase = createSupportedLanguageUseCase;
    }

    @PostMapping
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<CreateSupportedLanguageResponse>> create(
            @Valid @RequestBody CreateSupportedLanguageRequest request) {
        var command = CreateSupportedLanguageCommandMapper.fromRequest(request);
        var data = createSupportedLanguageUseCase.execute(command);
        var response = ApiResponse.success("Tạo Ngôn Ngữ Thành Công", data);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
