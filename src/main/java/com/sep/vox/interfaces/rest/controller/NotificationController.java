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

import com.sep.vox.application.port.input.command.MarkNotificationAsReadCommand;
import com.sep.vox.application.port.input.usecase.notification.CreateNotificationDeviceUseCase;
import com.sep.vox.application.port.input.usecase.notification.DeleteNotificationDeviceUseCase;
import com.sep.vox.application.port.input.usecase.notification.MarkAllNotificationsAsReadUseCase;
import com.sep.vox.application.port.input.usecase.notification.MarkNotificationAsReadUseCase;
import com.sep.vox.interfaces.rest.dto.request.CreateNotificationDeviceRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;
import com.sep.vox.interfaces.rest.mapper.CreateNotificationDeviceCommandMapper;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {
    
    private final CreateNotificationDeviceUseCase createNotificationDeviceUseCase;
    private final DeleteNotificationDeviceUseCase deleteNotificationDeviceUseCase;
    private final MarkNotificationAsReadUseCase markNotificationAsReadUseCase;
    private final MarkAllNotificationsAsReadUseCase markAllNotificationsAsReadUseCase;

    public NotificationController(
            CreateNotificationDeviceUseCase createNotificationDeviceUseCase,
            DeleteNotificationDeviceUseCase deleteNotificationDeviceUseCase, MarkNotificationAsReadUseCase markNotificationAsReadUseCase, MarkAllNotificationsAsReadUseCase markAllNotificationsAsReadUseCase) {
        this.createNotificationDeviceUseCase = createNotificationDeviceUseCase;
        this.deleteNotificationDeviceUseCase = deleteNotificationDeviceUseCase;
        this.markNotificationAsReadUseCase = markNotificationAsReadUseCase;
        this.markAllNotificationsAsReadUseCase = markAllNotificationsAsReadUseCase;
    }

    @PostMapping("/devices")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Object>> createDevice(@RequestBody @Valid CreateNotificationDeviceRequest request) {
        var command = CreateNotificationDeviceCommandMapper.fromRequest(request);
        createNotificationDeviceUseCase.execute(command);
        var response = ApiResponse.success("Thông báo cho thiết bị đã được khởi tạo");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/devices/{installationId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Object>> deleteDevice(@PathVariable(name = "installationId") String installationId) {
        deleteNotificationDeviceUseCase.execute(installationId);
        var response = ApiResponse.success("Thiết bị nhận thông báo đã được gỡ");
        return ResponseEntity.ok(response);
    }

    @PatchMapping("{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UUID>> markNotificationAsRead(@PathVariable(name = "id") UUID id) {
        var command = new MarkNotificationAsReadCommand(id);
        var data = markNotificationAsReadUseCase.execute(command);
        var response = ApiResponse.success("Thông báo đã được đọc", data);
        return ResponseEntity.ok(response);
    }


    @PatchMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Object>> markAllNotificationsAsRead() {
        var _ = markAllNotificationsAsReadUseCase.execute(null);
        var response = ApiResponse.success("Tất cả thông báo đã được đọc");
        return ResponseEntity.ok(response);
    }
}
