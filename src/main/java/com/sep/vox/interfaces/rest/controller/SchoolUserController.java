package com.sep.vox.interfaces.rest.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sep.vox.application.port.input.usecase.schooluser.ChangeSchoolUserRoleUseCase;
import com.sep.vox.application.port.input.usecase.schooluser.CreateSchoolUserUseCase;
import com.sep.vox.application.port.input.usecase.schooluser.DeleteSchoolUserUseCase;
import com.sep.vox.application.port.input.usecase.schooluser.ViewSchoolUserUseCase;
import com.sep.vox.application.response.input.schooluser.SchoolUserResponse;
import com.sep.vox.interfaces.rest.dto.request.ChangeSchoolUserRoleRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateSchoolUserRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;
import com.sep.vox.interfaces.rest.mapper.ChangeSchoolUserRoleCommandMapper;
import com.sep.vox.interfaces.rest.mapper.CreateSchoolUserCommandMapper;
import com.sep.vox.interfaces.rest.mapper.ViewSchoolUserCommandMapper;
import com.sep.vox.interfaces.rest.mapper.DeleteSchoolUserCommandMapper;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/schools")
public class SchoolUserController {

    private final CreateSchoolUserUseCase createSchoolUserUseCase;
    private final ViewSchoolUserUseCase viewSchoolUserUseCase;
    private final DeleteSchoolUserUseCase deleteSchoolUserUseCase;
    private final ChangeSchoolUserRoleUseCase changeSchoolUserRoleUseCase;

    public SchoolUserController(
            CreateSchoolUserUseCase createSchoolUserUseCase,
            ViewSchoolUserUseCase viewSchoolUserUseCase,
            DeleteSchoolUserUseCase deleteSchoolUserUseCase,
            ChangeSchoolUserRoleUseCase changeSchoolUserRoleUseCase) {
        this.createSchoolUserUseCase = createSchoolUserUseCase;
        this.viewSchoolUserUseCase = viewSchoolUserUseCase;
        this.deleteSchoolUserUseCase = deleteSchoolUserUseCase;
        this.changeSchoolUserRoleUseCase = changeSchoolUserRoleUseCase;
    }

    @PostMapping("/{schoolId}/users")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<SchoolUserResponse>> createUser(
            @PathVariable UUID schoolId,
            @Valid @RequestBody CreateSchoolUserRequest request) {
        var command = CreateSchoolUserCommandMapper.fromRequest(schoolId, request);
        var data = createSchoolUserUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Tạo người dùng thành công", data));
    }

    @GetMapping("/{schoolId}/users/{userId}")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<SchoolUserResponse>> getUser(
            @PathVariable UUID schoolId,
            @PathVariable UUID userId) {
        var command = ViewSchoolUserCommandMapper.fromRequest(schoolId, userId);
        SchoolUserResponse data = viewSchoolUserUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin người dùng thành công", data));
    }

    @DeleteMapping("/{schoolId}/users/{userId}")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<Object>> deleteUser(
            @PathVariable UUID schoolId,
            @PathVariable UUID userId) {
        var command = DeleteSchoolUserCommandMapper.fromRequest(schoolId, userId);
        deleteSchoolUserUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Xóa người dùng thành công"));
    }

    @PatchMapping("/{schoolId}/users/{userId}/role")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<Object>> changeUserRole(
            @PathVariable UUID schoolId,
            @PathVariable UUID userId,
            @Valid @RequestBody ChangeSchoolUserRoleRequest request) {
        var command = ChangeSchoolUserRoleCommandMapper.fromRequest(schoolId, userId, request);
        changeSchoolUserRoleUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật vai trò người dùng thành công"));
    }
}
