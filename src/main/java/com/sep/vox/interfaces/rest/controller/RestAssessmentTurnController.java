package com.sep.vox.interfaces.rest.controller;

import com.sep.vox.application.response.input.assessmentturn.TurnUploadUrlResponse;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sep.vox.application.port.input.query.assessmentturn.GetTurnUploadUrlQuery;
import com.sep.vox.application.port.input.usecase.assessmentturn.GetTurnUploadUrlUseCase;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;

@RestController
@RequestMapping("/api/v1/assessment-turns")
@RequiredArgsConstructor
public class RestAssessmentTurnController {

    private final GetTurnUploadUrlUseCase getTurnUploadUrlUseCase;

    @GetMapping("/upload-url")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<?>> getUploadUrl(
            @RequestParam(name = "attemptAnswerId") UUID attemptAnswerId,
            @RequestParam(name = "turnOrder") int turnOrder) {
        TurnUploadUrlResponse data = getTurnUploadUrlUseCase.execute(new GetTurnUploadUrlQuery(attemptAnswerId, turnOrder));
        return ResponseEntity.ok(ApiResponse.success("Upload URL retrieved successfully", data));
    }
}
