package com.sep.vox.interfaces.rest.controller;

import com.sep.vox.application.port.input.usecase.school.DeleteSchoolUseCase;
import com.sep.vox.application.port.input.usecase.school.UpdateSchoolStatusUseCase;
import com.sep.vox.application.port.input.usecase.school.UpdateSchoolUseCase;
import com.sep.vox.application.response.SchoolResponse.SchoolResponse;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;
import com.sep.vox.interfaces.rest.mapper.DeleteSchoolCommandMapper;
import com.sep.vox.interfaces.rest.mapper.UpdateSchoolStatusCommandMapper;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/school")
public class SchoolController {

    private final UpdateSchoolUseCase updateSchoolUseCase;
    private final DeleteSchoolUseCase deleteSchoolUseCase;
    private final UpdateSchoolStatusUseCase updateSchoolStatusUseCase;


    public SchoolController(UpdateSchoolUseCase updateSchoolUseCase, DeleteSchoolUseCase deleteSchoolUseCase, UpdateSchoolStatusUseCase updateSchoolStatusUseCase) {
        this.updateSchoolUseCase = updateSchoolUseCase;
        this.deleteSchoolUseCase = deleteSchoolUseCase;
        this.updateSchoolStatusUseCase = updateSchoolStatusUseCase;
    }


//    //Update School
//    @Operation(summary = "Cập nhật thông tin trường học")
//    @PutMapping("/{id}")
//    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
//    public ResponseEntity<ApiResponse<SchoolResponse>> updateSchool(
//            @PathVariable UUID id,
//            @Valid @RequestBody UpdateSchoolRequest request) {
//
//        // Map sang Command
//        var command = UpdateSchoolCommandMapper.fromRequest(id, request);
//        // Gọi UseCase
//        var response = updateSchoolUseCase.execute(command);
//        return ResponseEntity.ok(ApiResponse.success("Cập nhật trường học thành công" , response));
//    }


    //Delete School
    @Operation(summary = "Xóa trường học")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<SchoolResponse>> deleteSchool(@PathVariable UUID id) {

        var command = DeleteSchoolCommandMapper.fromRequest(id);

        // Hứng response từ UseCase
        SchoolResponse response = deleteSchoolUseCase.execute(command);

        return ResponseEntity.ok(ApiResponse.success("Xóa trường học thành công", response));
    }


    // Đổi status cho school
    @Operation(summary = "Thay đổi trạng thái hoạt động của trường học")
    @PatchMapping("/{id}/status/{isActive}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<SchoolResponse>> updateSchoolStatus(
            @PathVariable UUID id,
            @PathVariable boolean isActive) {
        var command = UpdateSchoolStatusCommandMapper.fromRequest(id, isActive);
        var response = updateSchoolStatusUseCase.execute(command);
        String message = isActive ? "Đã kích hoạt lại trường học thành công" : "Đã vô hiệu hóa trường học thành công";
        return ResponseEntity.ok(ApiResponse.success(message, response));
    }
}
