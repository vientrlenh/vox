package com.sep.vox.interfaces.rest.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sep.vox.application.port.input.command.CreateNotificationDeviceCommand;
import com.sep.vox.application.port.input.command.MarkNotificationAsReadCommand;
import com.sep.vox.application.port.input.usecase.notification.CreateNotificationDeviceUseCase;
import com.sep.vox.application.port.input.usecase.notification.DeleteNotificationDeviceUseCase;
import com.sep.vox.application.port.input.usecase.notification.MarkAllNotificationsAsReadUseCase;
import com.sep.vox.application.port.input.usecase.notification.MarkNotificationAsReadUseCase;
import com.sep.vox.interfaces.rest.dto.request.CreateNotificationDeviceRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;
import com.sep.vox.interfaces.rest.mapper.CreateNotificationDeviceCommandMapper;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor 
public class RestApiNotificationController {
    
    private final CreateNotificationDeviceUseCase createNotificationDeviceUseCase;
    private final DeleteNotificationDeviceUseCase deleteNotificationDeviceUseCase;
    private final MarkNotificationAsReadUseCase markNotificationAsReadUseCase;
    private final MarkAllNotificationsAsReadUseCase markAllNotificationsAsReadUseCase;
    

    @PostMapping("/devices")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> createDevice(@RequestBody @Valid CreateNotificationDeviceRequest request) {
        CreateNotificationDeviceCommand command = CreateNotificationDeviceCommandMapper.fromRequest(request);
        createNotificationDeviceUseCase.execute(command);
        ApiResponse<Void> response = ApiResponse.success("Device notification initialized");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/devices/{installationId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteDevice(@PathVariable(name = "installationId") String installationId) {
        deleteNotificationDeviceUseCase.execute(installationId);
        ApiResponse<Void> response = ApiResponse.success("Device notification removed");
        return ResponseEntity.ok(response);
    }

    @PatchMapping("{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UUID>> markNotificationAsRead(@PathVariable(name = "id") UUID id) {
        MarkNotificationAsReadCommand command = new MarkNotificationAsReadCommand(id);
        UUID data = markNotificationAsReadUseCase.execute(command);
        ApiResponse<UUID> response = ApiResponse.success("Notification read", data);
        return ResponseEntity.ok(response);
    }


    @PatchMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> markAllNotificationsAsRead() {
        markAllNotificationsAsReadUseCase.execute(null);
        ApiResponse<Void> response = ApiResponse.success("All notifications read");
        return ResponseEntity.ok(response);
    }
}
