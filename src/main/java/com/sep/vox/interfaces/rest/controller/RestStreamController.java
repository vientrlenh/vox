package com.sep.vox.interfaces.rest.controller;

import com.sep.vox.application.port.input.command.stream.IssueMonitorTokenCommand;
import com.sep.vox.application.port.input.command.stream.IssueStudentStreamTokenCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sep.vox.application.port.input.usecase.stream.IssueMonitorTokenUseCase;
import com.sep.vox.application.port.input.usecase.stream.IssueStudentStreamTokenUseCase;
import com.sep.vox.application.response.input.stream.IssueStudentStreamTokenResponse;
import com.sep.vox.interfaces.rest.dto.request.stream.IssueMonitorTokenRequest;
import com.sep.vox.interfaces.rest.dto.request.stream.IssueStudentStreamTokenRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/streams")
@RequiredArgsConstructor
public class RestStreamController {
    
    private final IssueStudentStreamTokenUseCase issueStudentStreamTokenUseCase;
    private final IssueMonitorTokenUseCase issueMonitorTokenUseCase;

    @PostMapping("/student/token")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<IssueStudentStreamTokenResponse>> getStreamToken(@Valid @RequestBody IssueStudentStreamTokenRequest request) {
        IssueStudentStreamTokenCommand command = IssueStudentStreamTokenRequest.toCommand(request);
        IssueStudentStreamTokenResponse data = issueStudentStreamTokenUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Stream token retrieved successfully", data));
    }


    @PostMapping("/monitor/token")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<String>> getMonitorToken(@Valid @RequestBody IssueMonitorTokenRequest request) {
        IssueMonitorTokenCommand command = IssueMonitorTokenRequest.toCommand(request);
        String token = issueMonitorTokenUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Monitoring token retrieved successfully", token));
    }
}
