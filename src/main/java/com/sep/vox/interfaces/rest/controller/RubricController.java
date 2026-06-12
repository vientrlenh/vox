package com.sep.vox.interfaces.rest.controller;


import com.sep.vox.application.port.input.usecase.rubricschool.*;

import com.sep.vox.application.port.input.usecase.rubricsystem.*;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.interfaces.rest.dto.request.*;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;
import com.sep.vox.interfaces.rest.mapper.*;


import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rubrics")
public class RubricController {

    private final CreateSchoolRubricCriterionUseCase createSchoolRubricCriterionUseCase;
    private final CreateSchoolRubricUseCase createSchoolRubricUseCase;
    private final CreateSystemRubricUseCase createSystemRubricUseCase;
    private final CreateSystemRubricCriteriaUseCase createSystemRubricCriteriaUseCase;
    private final CreateSchoolRubricResultBandsUseCase createSchoolRubricResultBandsUseCase;
    private final CreateSystemRubricResultBandsUseCase createSystemRubricResultBandsUseCase;
    private final DeleteSchoolRubricVersionUseCase deleteSchoolRubricVersionUseCase;
    private final DeleteSchoolRubricCriterionUseCase deleteSchoolRubricCriterionUseCase;
    private final DeleteSchoolRubricResultBandUseCase deleteSchoolRubricResultBandUseCase;
    private final DeleteSystemRubricUseCase deleteSystemRubricUseCase;
    private final DeleteSystemRubricVersionUseCase deleteSystemRubricVersionUseCase;
    private final DeleteSystemRubricCriterionUseCase deleteSystemRubricCriterionUseCase;
    private final DeleteSystemRubricResultBandUseCase deleteSystemRubricResultBandUseCase;
    private final ChangeSystemRubricVersionStatusUseCase changeSystemRubricVersionStatusUseCase;
    private final ChangeSchoolRubricVersionStatusUseCase changeSchoolRubricVersionStatusUseCase;

    public RubricController(CreateSchoolRubricCriterionUseCase createSchoolRubricCriterionUseCase,
                            CreateSchoolRubricUseCase createSchoolRubricUseCase,
                            CreateSystemRubricUseCase createSystemRubricUseCase,
                            CreateSystemRubricCriteriaUseCase createSystemRubricCriteriaUseCase,
                            CreateSchoolRubricResultBandsUseCase createSchoolRubricResultBandsUseCase,
                            CreateSystemRubricResultBandsUseCase createSystemRubricResultBandsUseCase,
                            DeleteSchoolRubricVersionUseCase deleteSchoolRubricVersionUseCase,
                            DeleteSchoolRubricCriterionUseCase deleteSchoolRubricCriterionUseCase,
                            DeleteSchoolRubricResultBandUseCase deleteSchoolRubricResultBandUseCase,
                            DeleteSystemRubricUseCase deleteSystemRubricUseCase,
                            DeleteSystemRubricVersionUseCase deleteSystemRubricVersionUseCase,
                            DeleteSystemRubricCriterionUseCase deleteSystemRubricCriterionUseCase, DeleteSystemRubricResultBandUseCase deleteSystemRubricResultBandUseCase, ChangeSystemRubricVersionStatusUseCase changeSystemRubricVersionStatusUseCase,
                            ChangeSchoolRubricVersionStatusUseCase changeSchoolRubricVersionStatusUseCase) {
        this.createSchoolRubricCriterionUseCase = createSchoolRubricCriterionUseCase;
        this.createSchoolRubricUseCase = createSchoolRubricUseCase;
        this.createSystemRubricUseCase = createSystemRubricUseCase;
        this.createSystemRubricCriteriaUseCase = createSystemRubricCriteriaUseCase;
        this.createSchoolRubricResultBandsUseCase = createSchoolRubricResultBandsUseCase;
        this.createSystemRubricResultBandsUseCase = createSystemRubricResultBandsUseCase;
        this.deleteSchoolRubricVersionUseCase = deleteSchoolRubricVersionUseCase;
        this.deleteSchoolRubricCriterionUseCase = deleteSchoolRubricCriterionUseCase;
        this.deleteSchoolRubricResultBandUseCase = deleteSchoolRubricResultBandUseCase;
        this.deleteSystemRubricUseCase = deleteSystemRubricUseCase;
        this.deleteSystemRubricVersionUseCase = deleteSystemRubricVersionUseCase;
        this.deleteSystemRubricCriterionUseCase = deleteSystemRubricCriterionUseCase;
        this.deleteSystemRubricResultBandUseCase = deleteSystemRubricResultBandUseCase;
        this.changeSystemRubricVersionStatusUseCase = changeSystemRubricVersionStatusUseCase;
        this.changeSchoolRubricVersionStatusUseCase = changeSchoolRubricVersionStatusUseCase;
    }

    //==========================RUBRIC  & RUBRIC VERSION===================================
    // API 1: TẠO RUBRIC & VERSION (DRAFT) = tối ưu/chưa chạy lại => Check Đợt 2
    @Operation(summary = "Tạo mới một bộ tiêu chí (Rubric) cho trường học")
    @PostMapping("/schools/{schoolId}/rubrics")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> createSchoolRubric(
            @PathVariable UUID schoolId,
            @Valid @RequestBody CreateSchoolRubricRequest request
    ) {
        var command = CreateSchoolRubricCommandMapper.fromRequest(schoolId, request);
        var response = createSchoolRubricUseCase.execute(command);

        return ResponseEntity.ok(ApiResponse.success("Khởi tạo bộ tiêu chí nháp thành công", response));
    }


    //Tạo mới 1 rubric cho 1 hệ thống đã tối ưu / => đã check đợt 2
    @Operation(summary = "Tạo mới một (Rubric) cho hệ thống")
    @PostMapping("/system/rubrics")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')") // Phân quyền cao hơn
    public ResponseEntity<ApiResponse<UUID>> createSystemRubric(
            @Valid @RequestBody CreateSystemRubricRequest request
    ) {
        var command = CreateSystemRubricCommandMapper.fromRequest(request);
        var response = createSystemRubricUseCase.execute(command);

        return ResponseEntity.ok(ApiResponse.success("Khởi tạo Rubric hệ thống thành công", response));
    }


    //Xóa rubric version của trường -> ko cho xoa rubric gốc (đã tối ưu/chưa chạy lại) => check đợt 2
    @Operation(summary = "Xử lý phiên bản Rubric (Xóa nếu DRAFT, Lưu trữ nếu PUBLISHED)")
    @DeleteMapping("/schools/{schoolId}/rubric-versions/{versionId}")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteSchoolRubricVersion(
            @PathVariable UUID schoolId,
            @PathVariable UUID versionId
    ) {
        var command = DeleteSchoolRubricCommandMapper.versionFromRequest(schoolId, versionId);
        deleteSchoolRubricVersionUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Xóa phiên bản Rubric thành công"));
    }


    // Đang làm ở đây - dc xoa rubric gốc => Check đợt 2
    @Operation(summary = "Xóa toàn bộ Rubric của hệ thống (Chỉ cho phép khi chưa có bản nào được PUBLISHED/ARCHIVED)")
    @DeleteMapping("/system/rubrics/{rubricId}") // SỬA PATH Ở ĐÂY CHO CHUẨN
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteSystemRubric(
            @PathVariable UUID rubricId
    ) {
        var command = DeleteSystemRubricCommandMapper.fromRequest(rubricId);
        deleteSystemRubricUseCase.execute(command); // UseCase trả về Void nên ko cần gán biến
        return ResponseEntity.ok(ApiResponse.success("Xóa bộ Rubric hệ thống thành công"));
    }


    // Xóa rubric version hệ thồng => Đã check 2
    @Operation(summary = "Xóa rubric version của hệ thống")
    @DeleteMapping("/system/rubric-versions/{versionId}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> manageSystemRubricVersion(
            @PathVariable UUID versionId
    ) {
        var command = DeleteSystemRubricCommandMapper.versionFromRequest(versionId);
        deleteSystemRubricVersionUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Xử lý phiên bản hệ thống thành công"));
    }

    // Update Status RubricVersion của system admin => đã check 2
    @Operation(summary = "Đổi trạng thái Phiên bản Rubric Hệ thống (Ví dụ: DRAFT -> PUBLISHED, ARCHIVED)")
    @PatchMapping("/system/rubric-versions/{versionId}/status")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> changeSystemRubricVersionStatus(
            @PathVariable UUID versionId,
            @RequestParam(name = "status") RubricStatus status
    ) {
        var command = ChangeRubricVersionStatusCommandMapper.fromSystemRequest(versionId, status);
        var responseId = changeSystemRubricVersionStatusUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Chuyển trạng thái phiên bản thành công!", responseId));
    }

    // API: CẬP NHẬT TRẠNG THÁI PHIÊN BẢN RUBRIC TRƯỜNG HỌC => đã check 2
    @Operation(summary = "Đổi trạng thái Phiên bản Rubric Trường học (Ví dụ: DRAFT -> PUBLISHED, ARCHIVED)")
    @PatchMapping("/schools/{schoolId}/rubric-versions/{versionId}/status")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> changeSchoolRubricVersionStatus(
            @PathVariable UUID schoolId,
            @PathVariable UUID versionId,
            @RequestParam(name = "status") RubricStatus status
    ) {
        var command = ChangeRubricVersionStatusCommandMapper.fromSchoolRequest(schoolId, versionId, status);
        var responseId = changeSchoolRubricVersionStatusUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Chuyển trạng thái phiên bản thành công!", responseId));
    }


    //=====================RUBRIC CRITERION===============================
    //Thêm Rubric Creitrion vào Rubric Version đã tối ưu/ chưa chạy lại =>check 2
    @Operation(summary = "Thêm một Tiêu chí (Criterion) vào Phiên bản Rubric (RubricVersion) cho trường học")
    @PostMapping("/schools/{schoolId}/rubric-versions/{versionId}/criteria")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<List<UUID>>> createSchoolRubricCriterion(
            @PathVariable UUID schoolId,
            @PathVariable UUID versionId,
            @Valid @RequestBody CreateSchoolRubricCriteriaRequest request
    ) {
        var command = CreateSchoolRubricCriteriaCommandMapper.fromRequest(schoolId, versionId, request);
        var newCriterionId = createSchoolRubricCriterionUseCase.execute(command);

        return ResponseEntity.ok(ApiResponse.success("Thêm tiêu chí thành công", newCriterionId));
    }


    // Phục vụ cho việc tạo bài kiểm tra đầu vào (cá nhân hóa đánh giá năng lực)
    //Đã tối ưu/chưa chạy lại
    // đã check 2
    @Operation(summary = "Thêm Rubric version hệ thống")
    @PostMapping("/system/rubric-versions/{versionId}/criteria")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<List<UUID>>> createSystemRubricCriterion(
            @PathVariable UUID versionId,
            @Valid @RequestBody CreateSystemRubricCriteriaRequest request
    ) {
        // Truyền versionId vào mapper
        var command = CreateSystemRubricCriteriaCommandMapper.fromRequest(versionId, request);
        var result = createSystemRubricCriteriaUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Thêm tiêu chí hệ thống thành công", result));
    }


    // Xóa 1 version Rubric khỏi rubric của trường
    // đã tối ưu / chưa chạy lại
    //Check 2
    @Operation(summary = "Xóa một Tiêu chí khỏi phiên bản Rubric")
    @DeleteMapping("/schools/{schoolId}/rubric-versions/{versionId}/criteria/{criterionId}")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteSchoolRubricCriterion(
            @PathVariable UUID schoolId,
            @PathVariable UUID versionId,
            @PathVariable UUID criterionId
    ) {
        var command = DeleteSchoolRubricCommandMapper.criterionFromRequest(schoolId, versionId, criterionId);
        var response = deleteSchoolRubricCriterionUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Xóa tiêu chí thành công"));
    }


    // Xóa tiêu chí rubric trong hệ thống
    // Đã tối ưu / chưa chạy lại
    // check 2
    @Operation(summary = "Xóa một Tiêu chí(Rubric Version) khỏi phiên bản Rubric Hệ thống")
    @DeleteMapping("/system/rubric-versions/{versionId}/criteria/{criterionId}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteSystemRubricCriterion(
            @PathVariable UUID versionId,
            @PathVariable UUID criterionId
    ) {
        var command = DeleteSystemRubricCommandMapper.criterionFromRequest(versionId, criterionId);
        deleteSystemRubricCriterionUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Xóa tiêu chí hệ thống thành công"));
    }

    //=====================RUBRIC RESULT BAND===============================
    // Phục vụ : Đánh giá sinh viên của trường (Giỏi, Khá, Trung Bình)
    //Đã tối ưu/ chưa chạy lại
    // Check 2
    @Operation(summary = "Thêm một Thang điểm kết quả/đánh giá (Result Band) vào Phiên bản Rubric của Trường")
    @PostMapping("/schools/{schoolId}/rubric-versions/{versionId}/result-bands")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<List<UUID>>> createSchoolRubricResultBand(
            @PathVariable UUID schoolId,
            @PathVariable UUID versionId,
            @Valid @RequestBody CreateSchoolRubricResultBandsRequest request
    ) {
        var command = CreateSchoolRubricResultBandsCommandMapper.fromRequest(schoolId, versionId, request);
        var newResultBandId = createSchoolRubricResultBandsUseCase.execute(command);

        return ResponseEntity.ok(ApiResponse.success("Thêm thang điểm kết quả thành công", newResultBandId));
    }

    //Phục vụ: Đánh giá sinh viên của system (Giỏi, Khá, Trung Bình)
    //Đã tối ưu/ chưa chạy lại
    // Check 2
    @Operation(summary = "Thêm một Thang điểm kết quả/đánh giá vào Rubric Hệ thống")
    @PostMapping("/system/rubric-versions/{versionId}/result-bands")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<List<UUID>>> createSystemRubricResultBand(
            @PathVariable UUID versionId,
            @Valid @RequestBody CreateSystemRubricResultBandsRequest request // Nhớ tạo thêm Request này nhé
    ) {
        var command = CreateSystemRubricResultBandsCommandMapper.fromRequest(versionId, request);
        var resultId = createSystemRubricResultBandsUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Thêm thang điểm hệ thống thành công", resultId));
    }

    // API: XÓA LẺ 1 THANG ĐIỂM KẾT QUẢ (RESULT BAND)
    //Check 2
    @Operation(summary = "Xóa một Thang điểm kết quả khỏi phiên bản Rubric")
    @DeleteMapping("/schools/{schoolId}/rubric-versions/{versionId}/result-bands/{resultBandId}")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteSchoolRubricResultBand(
            @PathVariable UUID schoolId,
            @PathVariable UUID versionId,
            @PathVariable UUID resultBandId
    ) {
        var command = DeleteSchoolRubricCommandMapper.resultBandFromRequest(schoolId, versionId, resultBandId);
        deleteSchoolRubricResultBandUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Xóa thang điểm thành công"));
    }


    // Xóa 1 Rubric Resust band khổi rubric version
    @Operation(summary = "Xóa một Thang điểm khỏi phiên bản Rubric Hệ thống")
    @DeleteMapping("/system/rubric-versions/{versionId}/result-bands/{resultBandId}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteSystemRubricResultBand(
            @PathVariable UUID versionId,
            @PathVariable UUID resultBandId
    ) {
        var command = DeleteSystemRubricCommandMapper.resultBandFromRequest(versionId, resultBandId);
        deleteSystemRubricResultBandUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Xóa thang điểm hệ thống thành công"));
    }

}
