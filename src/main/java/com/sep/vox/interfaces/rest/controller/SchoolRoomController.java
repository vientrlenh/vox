package com.sep.vox.interfaces.rest.controller;

import com.sep.vox.application.port.input.usecase.schoolroom.AddSchoolRoomUseCase;
import com.sep.vox.application.port.input.usecase.schoolroom.UpdateSchoolRoomUseCase;
import com.sep.vox.application.response.SchoolRoomResponse.SchoolRoomResponse;
import com.sep.vox.interfaces.rest.dto.request.AddSchoolRoomRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateSchoolRoomRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;
import com.sep.vox.interfaces.rest.mapper.AddSchoolRoomCommandMapper;
import com.sep.vox.interfaces.rest.mapper.UpdateSchoolRoomCommandMapper;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/school-rooms")
public class SchoolRoomController {

    private final AddSchoolRoomUseCase addSchoolRoomUseCase;
    private final UpdateSchoolRoomUseCase updateSchoolRoomUseCase;

    public SchoolRoomController(AddSchoolRoomUseCase addSchoolRoomUseCase, UpdateSchoolRoomUseCase updateSchoolRoomUseCase) {
        this.addSchoolRoomUseCase = addSchoolRoomUseCase;
        this.updateSchoolRoomUseCase = updateSchoolRoomUseCase;
    }

    @Operation(summary = "Thêm phòng thi")
    @PostMapping
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<SchoolRoomResponse>> addSchoolRoom(@Valid @RequestBody AddSchoolRoomRequest request) {
        // 1. Map Request sang Command
        var command = AddSchoolRoomCommandMapper.fromRequest(request);

        // 2. Chạy UseCase và hứng dữ liệu trả về (result chính là SchoolRoomResponse)
        var result = addSchoolRoomUseCase.execute(command);

        // 3. Trả về cho Frontend với 2 tham số: Câu thông báo và Data
        return ResponseEntity.ok(ApiResponse.success("Thêm phòng học thành công", result));
    }

    @Operation(summary = "Cập nhật thông tin phòng học")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<SchoolRoomResponse>> updateSchoolRoom(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSchoolRoomRequest request) {

        var command = UpdateSchoolRoomCommandMapper.fromRequest(id, request);
        var result = updateSchoolRoomUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật phòng học thành công", result));
    }


}