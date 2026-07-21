package com.sep.vox.interfaces.rest.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sep.vox.application.port.input.usecase.stream.IssueMonitorTokenUseCase;
import com.sep.vox.application.port.input.usecase.stream.IssueStudentStreamTokenUseCase;
import com.sep.vox.interfaces.rest.dto.request.IssueMonitorTokenRequest;
import com.sep.vox.interfaces.rest.dto.request.IssueStudentStreamTokenRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;
import com.sep.vox.interfaces.rest.mapper.IssueMonitorTokenCommandMapper;
import com.sep.vox.interfaces.rest.mapper.IssueStudentStreamTokenCommandMapper;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/streams")
public class StreamController {
    
    private final IssueStudentStreamTokenUseCase issueStudentStreamTokenUseCase;
    private final IssueMonitorTokenUseCase issueMonitorTokenUseCase;

    public StreamController(IssueStudentStreamTokenUseCase issueStudentStreamTokenUseCase, IssueMonitorTokenUseCase issueMonitorTokenUseCase) {
        this.issueStudentStreamTokenUseCase = issueStudentStreamTokenUseCase; 
        this.issueMonitorTokenUseCase = issueMonitorTokenUseCase;
    }

    @PostMapping("/student/token")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<String>> getStreamToken(@Valid @RequestBody IssueStudentStreamTokenRequest request) {
        var command = IssueStudentStreamTokenCommandMapper.fromRequest(request);
        var token = issueStudentStreamTokenUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Token stream được lấy thành công", token));
    }


    @PostMapping("/monitor/token")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<String>> getMonitorToken(@Valid @RequestBody IssueMonitorTokenRequest request) {
        var command = IssueMonitorTokenCommandMapper.fromRequest(request);
        var token = issueMonitorTokenUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Token giám sát được lấy thành công", token));
    }
}
