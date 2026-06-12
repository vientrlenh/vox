package com.sep.vox.interfaces.rest.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sep.vox.application.port.input.command.dummy.PublishReportGeneratedCommand;
import com.sep.vox.application.port.input.command.dummy.PublishUserRegisteredCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;

@RestController
@RequestMapping("/api/v1/dummy/external-events")
public class DummyExternalEventController {

    private final IUseCase<PublishUserRegisteredCommand, Void> publishUserRegisteredUseCase;
    private final IUseCase<PublishReportGeneratedCommand, Void> publishReportGeneratedUseCase;

    public DummyExternalEventController(
        IUseCase<PublishUserRegisteredCommand, Void> publishUserRegisteredUseCase,
        IUseCase<PublishReportGeneratedCommand, Void> publishReportGeneratedUseCase
    ) {
        this.publishUserRegisteredUseCase = publishUserRegisteredUseCase;
        this.publishReportGeneratedUseCase = publishReportGeneratedUseCase;
    }

    @PostMapping("/user-registered")
    public ResponseEntity<ApiResponse<Object>> publishUserRegistered(@RequestBody PublishUserRegisteredCommand input) {
        publishUserRegisteredUseCase.execute(input);
        return ResponseEntity.ok(ApiResponse.success("Da publish DummyUserRegisteredExternalEvent"));
    }

    @PostMapping("/report-generated")
    public ResponseEntity<ApiResponse<Object>> publishReportGenerated(@RequestBody PublishReportGeneratedCommand input) {
        publishReportGeneratedUseCase.execute(input);
        return ResponseEntity.ok(ApiResponse.success("Da publish DummyReportGeneratedExternalEvent"));
    }
}
