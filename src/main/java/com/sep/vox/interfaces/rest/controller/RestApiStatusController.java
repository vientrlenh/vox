package com.sep.vox.interfaces.rest.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sep.vox.interfaces.rest.dto.response.ApiResponse;

@RestController
@RequestMapping("/api/v1/status")
public class RestApiStatusController {

    @GetMapping
    public ResponseEntity<ApiResponse<Void>> ping() {
        ApiResponse<Void> response = ApiResponse.success("Server is alive");
        return ResponseEntity.ok(response);
    }
}
