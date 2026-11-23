package com.sep.vox.interfaces.rest.controller;

import com.sep.vox.application.port.input.command.RejectImportSessionCommand;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sep.vox.application.port.input.usecase.importfile.RejectImportSessionUseCase;
import com.sep.vox.application.response.input.importfile.RejectImportSessionResponse;
import com.sep.vox.interfaces.rest.dto.request.RejectImportSessionRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;
import com.sep.vox.interfaces.rest.mapper.RejectImportSessionCommandMapper;

@RestController
@RequestMapping("/api/v1/imports")
@RequiredArgsConstructor
public class ImportController {

    private final RejectImportSessionUseCase rejectImportSessionUseCase;

    @PostMapping("/{sessionId}/reject")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<RejectImportSessionResponse>> rejectImportSession(
            @PathVariable("sessionId") UUID sessionId,
            @RequestBody(required = false) RejectImportSessionRequest request) {
        RejectImportSessionCommand command = RejectImportSessionCommandMapper.fromRequest(sessionId, request);
        RejectImportSessionResponse data = rejectImportSessionUseCase.execute(command);
        ApiResponse<RejectImportSessionResponse> response = ApiResponse.success("Hủy import thành công", data);
        return ResponseEntity.ok(response);
    }
}
