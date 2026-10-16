package com.sep.vox.interfaces.rest.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sep.vox.application.port.input.query.GetTurnUploadUrlQuery;
import com.sep.vox.application.port.input.usecase.assessmentturn.GetTurnUploadUrlUseCase;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;

@RestController
@RequestMapping("/api/v1/assessment-turns")
public class AssessmentTurnController {

    private final GetTurnUploadUrlUseCase getTurnUploadUrlUseCase;

    public AssessmentTurnController(GetTurnUploadUrlUseCase getTurnUploadUrlUseCase) {
        this.getTurnUploadUrlUseCase = getTurnUploadUrlUseCase;
    }

    @GetMapping("/upload-url")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<?>> getUploadUrl(
            @RequestParam(name = "attemptAnswerId") UUID attemptAnswerId,
            @RequestParam(name = "turnOrder") int turnOrder) {
        var data = getTurnUploadUrlUseCase.execute(new GetTurnUploadUrlQuery(attemptAnswerId, turnOrder));
        return ResponseEntity.ok(ApiResponse.success("Lấy upload URL thành công", data));
    }
}
