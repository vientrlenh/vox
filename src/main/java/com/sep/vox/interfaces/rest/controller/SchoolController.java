package com.sep.vox.interfaces.rest.controller;

import com.sep.vox.application.port.input.usecase.school.DeleteSchoolUseCase;
import com.sep.vox.application.port.input.usecase.school.UpdateSchoolStatusUseCase;
import com.sep.vox.application.port.input.usecase.schoolgrade.CreateSchoolGradeUseCase;
import com.sep.vox.application.port.input.usecase.schoolgrade.DeleteSchoolGradeUseCase;
import com.sep.vox.application.port.input.usecase.schoolroom.AddSchoolRoomUseCase;
import com.sep.vox.application.port.input.usecase.schoolroom.DeleteSchoolRoomUseCase;
import com.sep.vox.application.response.SchoolGradeResponse.SchoolGradeResponse;
import com.sep.vox.application.response.SchoolResponse.SchoolResponse;
import com.sep.vox.application.response.SchoolRoomResponse.SchoolRoomResponse;
import com.sep.vox.interfaces.rest.dto.request.AddSchoolRoomRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateSchoolGradeRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;


import com.sep.vox.interfaces.rest.mapper.*;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/schools")
public class SchoolController {

    private final DeleteSchoolUseCase deleteSchoolUseCase;
    private final UpdateSchoolStatusUseCase updateSchoolStatusUseCase;
    private final AddSchoolRoomUseCase addSchoolRoomUseCase;
    private final DeleteSchoolRoomUseCase deleteSchoolRoomUseCase;
    private final CreateSchoolGradeUseCase createSchoolGradeUseCase;
    private final DeleteSchoolGradeUseCase deleteSchoolGradeUseCase;


    public SchoolController(DeleteSchoolUseCase deleteSchoolUseCase, UpdateSchoolStatusUseCase updateSchoolStatusUseCase, AddSchoolRoomUseCase addSchoolRoomUseCase, DeleteSchoolRoomUseCase deleteSchoolRoomUseCase, CreateSchoolGradeUseCase createSchoolGradeUseCase, DeleteSchoolGradeUseCase deleteSchoolGradeUseCase) {
        this.deleteSchoolUseCase = deleteSchoolUseCase;
        this.updateSchoolStatusUseCase = updateSchoolStatusUseCase;
        this.addSchoolRoomUseCase = addSchoolRoomUseCase;
        this.deleteSchoolRoomUseCase = deleteSchoolRoomUseCase;
        this.createSchoolGradeUseCase = createSchoolGradeUseCase;
        this.deleteSchoolGradeUseCase = deleteSchoolGradeUseCase;
    }


    //Delete School
    @Operation(summary = "Xóa trường học")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> deleteSchool(@PathVariable UUID id) {

        var command = DeleteSchoolCommandMapper.fromRequest(id);

        // Hứng response từ UseCase
        var response = deleteSchoolUseCase.execute(command);

        return ResponseEntity.ok(ApiResponse.success("Xóa trường học thành công", response));
    }


    @Operation(summary = "Thay đổi trạng thái hoạt động của trường học")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<SchoolResponse>> updateSchoolStatus(
            @PathVariable UUID id,
            @RequestParam boolean isActive
    ) {

        var command = UpdateSchoolStatusCommandMapper.fromRequest(id, isActive);

        var response = updateSchoolStatusUseCase.execute(command);

        String message = isActive
                ? "Đã kích hoạt lại trường học thành công"
                : "Đã vô hiệu hóa trường học thành công";

        return ResponseEntity.ok(
                ApiResponse.success(message, response)
        );
    }


    //=======================SCHOOL ROOM=========================================
    @Operation(summary = "Thêm phòng học")
    @PostMapping("/{schoolId}/rooms")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<SchoolRoomResponse>> addSchoolRoom(
            @PathVariable UUID schoolId,
            @Valid @RequestBody AddSchoolRoomRequest request
    ) {

        var command = AddSchoolRoomCommandMapper.fromRequest(schoolId,request);

        var result = addSchoolRoomUseCase.execute(command);

        return ResponseEntity.ok(
                ApiResponse.success("Thêm phòng học thành công", result)
        );
    }

    @Operation(summary = "Xóa phòng học theo id ")
    @DeleteMapping("/{schoolId}/rooms/{roomId}")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<SchoolRoomResponse>> deleteSchoolRoom(
            @PathVariable("schoolId") UUID schoolId,
            @PathVariable UUID roomId
    ) {

        // Map ID từ URL vào Command
        var command = DeleteSchoolRoomCommandMapper.fromRequest(roomId,schoolId);

        // Thực thi UseCase
        var result = deleteSchoolRoomUseCase.execute(command);

        // Trả về ApiResponse chuẩn của team bạn
        return ResponseEntity.ok(ApiResponse.success("Xóa thành công school room", result));
    }

    //====================================SCHOOL GRADE ==============================================

    @Operation(summary = "Thêm khối học sinh vd: khối 10,11,12")
    @PostMapping("/{schoolId}/grades")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> createSchoolGrade(
            @PathVariable UUID schoolId,
            @Valid @RequestBody CreateSchoolGradeRequest request) {

        var command = CreateSchoolGradeCommandMapper.fromRequest(schoolId, request);

        UUID newGradeId = createSchoolGradeUseCase.execute(command);

        return ResponseEntity.ok(ApiResponse.success("Thêm thành công khối của trường", newGradeId));
    }



    @Operation(summary = "Xóa khối theo id của trường ")
    @DeleteMapping("/{schoolId}/grades/{gradeId}")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<SchoolGradeResponse>> deleteSchoolGrade(
            @PathVariable UUID schoolId,
            @PathVariable UUID gradeId
    ) {

        // Map ID từ URL vào Command
        var command = DeleteSchoolGradeCommandMapper.fromRequest(schoolId,gradeId);

        // Thực thi UseCase
        var result = deleteSchoolGradeUseCase.execute(command);

        // Trả về ApiResponse chuẩn của team bạn
        return ResponseEntity.ok(ApiResponse.success("Xóa thành công school grade", result));
    }


}
