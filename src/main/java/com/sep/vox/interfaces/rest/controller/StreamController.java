package com.sep.vox.interfaces.rest.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sep.vox.application.port.input.usecase.stream.GetStreamTokenUseCase;
import com.sep.vox.interfaces.rest.dto.request.GetStreamTokenRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;
import com.sep.vox.interfaces.rest.mapper.GetStreamTokenCommandMapper;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/streams")
public class StreamController {
    
    private final GetStreamTokenUseCase getStreamTokenUseCase;

    public StreamController(GetStreamTokenUseCase getStreamTokenUseCase) {
        this.getStreamTokenUseCase = getStreamTokenUseCase;
    }

    @PostMapping("/token")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<String>> getStreamToken(@Valid @RequestBody GetStreamTokenRequest request) {
        var command = GetStreamTokenCommandMapper.fromRequest(request);
        var token = getStreamTokenUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success(token));
    }
}
