package com.sep.vox.interfaces.rest.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sep.vox.interfaces.rest.dto.response.ApiResponse;

@RestController
@RequestMapping("/api")
public class RestStatusController {

    @GetMapping("/v1/status")
    public ResponseEntity<ApiResponse<Object>> ping() {
        ApiResponse<Object> response = ApiResponse.success("Server is running");
        return ResponseEntity.ok(response);
    }
}
