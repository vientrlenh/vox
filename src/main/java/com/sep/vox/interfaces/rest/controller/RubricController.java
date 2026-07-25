package com.sep.vox.interfaces.rest.controller;


import java.util.List;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.sep.vox.application.port.input.usecase.rubricschool.AcceptSchoolRubricCriterionImportUseCase;
import com.sep.vox.application.port.input.usecase.rubricschool.AcceptSchoolRubricResultBandImportUseCase;
import com.sep.vox.application.port.input.usecase.rubricschool.AcceptSchoolRubricVersionImportUseCase;
import com.sep.vox.application.port.input.usecase.rubricschool.AddSchoolRubricVersionsUseCase;
import com.sep.vox.application.port.input.usecase.rubricschool.ArchiveSchoolRubricVersionUseCase;
import com.sep.vox.application.port.input.usecase.rubricschool.ChangeSchoolRubricVersionStatusUseCase;
import com.sep.vox.application.port.input.usecase.rubricschool.CreateSchoolRubricCriterionUseCase;
import com.sep.vox.application.port.input.usecase.rubricschool.CreateSchoolRubricResultBandsUseCase;
import com.sep.vox.application.port.input.usecase.rubricschool.CreateSchoolRubricUseCase;
import com.sep.vox.application.port.input.usecase.rubricschool.DeleteSchoolRubricCriterionUseCase;
import com.sep.vox.application.port.input.usecase.rubricschool.DeleteSchoolRubricResultBandUseCase;
import com.sep.vox.application.port.input.usecase.rubricschool.DeleteSchoolRubricVersionUseCase;
import com.sep.vox.application.port.input.usecase.rubricschool.PreviewSchoolRubricCriterionImportFromFileUseCase;
import com.sep.vox.application.port.input.usecase.rubricschool.PreviewSchoolRubricResultBandImportFromFileUseCase;
import com.sep.vox.application.port.input.usecase.rubricschool.PreviewSchoolRubricVersionImportFromFileUseCase;
import com.sep.vox.application.port.input.usecase.rubricsystem.AcceptSystemRubricCriterionImportUseCase;
import com.sep.vox.application.port.input.usecase.rubricsystem.AcceptSystemRubricResultBandImportUseCase;
import com.sep.vox.application.port.input.usecase.rubricsystem.AcceptSystemRubricVersionImportUseCase;
import com.sep.vox.application.port.input.usecase.rubricsystem.AddSystemRubricVersionsUseCase;
import com.sep.vox.application.port.input.usecase.rubricsystem.ArchiveSystemRubricVersionUseCase;
import com.sep.vox.application.port.input.usecase.rubricsystem.ChangeSystemRubricVersionStatusUseCase;
import com.sep.vox.application.port.input.usecase.rubricsystem.CreateSystemRubricCriteriaUseCase;
import com.sep.vox.application.port.input.usecase.rubricsystem.CreateSystemRubricResultBandsUseCase;
import com.sep.vox.application.port.input.usecase.rubricsystem.CreateSystemRubricUseCase;
import com.sep.vox.application.port.input.usecase.rubricsystem.DeleteSystemRubricCriterionUseCase;
import com.sep.vox.application.port.input.usecase.rubricsystem.DeleteSystemRubricResultBandUseCase;
import com.sep.vox.application.port.input.usecase.rubricsystem.DeleteSystemRubricUseCase;
import com.sep.vox.application.port.input.usecase.rubricsystem.DeleteSystemRubricVersionUseCase;
import com.sep.vox.application.port.input.usecase.rubricsystem.PreviewSystemRubricCriterionImportFromFileUseCase;
import com.sep.vox.application.port.input.usecase.rubricsystem.PreviewSystemRubricResultBandImportFromFileUseCase;
import com.sep.vox.application.port.input.usecase.rubricsystem.PreviewSystemRubricVersionImportFromFileUseCase;
import com.sep.vox.application.port.input.command.ArchiveSchoolRubricVersionCommand;
import com.sep.vox.application.port.input.command.ArchiveSystemRubricVersionCommand;
import com.sep.vox.application.response.input.importfile.AcceptRubricCriterionImportResponse;
import com.sep.vox.application.response.input.importfile.AcceptRubricResultBandImportResponse;
import com.sep.vox.application.response.input.importfile.AcceptRubricVersionImportResponse;
import com.sep.vox.application.response.input.importfile.PreviewRubricCriterionImportResponse;
import com.sep.vox.application.response.input.importfile.PreviewRubricResultBandImportResponse;
import com.sep.vox.application.response.input.importfile.PreviewRubricVersionImportResponse;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.interfaces.rest.dto.request.AcceptImportRequest;
import com.sep.vox.interfaces.rest.dto.request.AddRubricVersionsRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateSchoolRubricCriteriaRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateSchoolRubricRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateSchoolRubricResultBandsRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateSystemRubricCriteriaRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateSystemRubricRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateSystemRubricResultBandsRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;
import com.sep.vox.interfaces.rest.mapper.AcceptRubricCriterionImportCommandMapper;
import com.sep.vox.interfaces.rest.mapper.AcceptRubricResultBandImportCommandMapper;
import com.sep.vox.interfaces.rest.mapper.AcceptRubricVersionImportCommandMapper;
import com.sep.vox.interfaces.rest.mapper.AddRubricVersionsCommandMapper;
import com.sep.vox.interfaces.rest.mapper.ChangeRubricVersionStatusCommandMapper;
import com.sep.vox.interfaces.rest.mapper.CreateSchoolRubricCommandMapper;
import com.sep.vox.interfaces.rest.mapper.CreateSchoolRubricCriteriaCommandMapper;
import com.sep.vox.interfaces.rest.mapper.CreateSchoolRubricResultBandsCommandMapper;
import com.sep.vox.interfaces.rest.mapper.CreateSystemRubricCommandMapper;
import com.sep.vox.interfaces.rest.mapper.CreateSystemRubricCriteriaCommandMapper;
import com.sep.vox.interfaces.rest.mapper.CreateSystemRubricResultBandsCommandMapper;
import com.sep.vox.interfaces.rest.mapper.DeleteSchoolRubricCommandMapper;
import com.sep.vox.interfaces.rest.mapper.DeleteSystemRubricCommandMapper;
import com.sep.vox.interfaces.rest.mapper.PreviewRubricCriterionImportCommandMapper;
import com.sep.vox.interfaces.rest.mapper.PreviewRubricResultBandImportCommandMapper;
import com.sep.vox.interfaces.rest.mapper.PreviewRubricVersionImportFromFileCommandMapper;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;

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
    private final ArchiveSystemRubricVersionUseCase archiveSystemRubricVersionUseCase;
    private final ArchiveSchoolRubricVersionUseCase archiveSchoolRubricVersionUseCase;
    private final PreviewSystemRubricVersionImportFromFileUseCase previewSystemRubricVersionImportFromFileUseCase;
    private final AcceptSystemRubricVersionImportUseCase acceptSystemRubricVersionImportUseCase;
    private final AddSystemRubricVersionsUseCase addSystemRubricVersionsUseCase;
    private final AcceptSchoolRubricVersionImportUseCase acceptSchoolRubricVersionImportUseCase;
    private final PreviewSchoolRubricVersionImportFromFileUseCase previewSchoolRubricVersionImportFromFileUseCase;
    private final AddSchoolRubricVersionsUseCase addSchoolRubricVersionsUseCase;
    private final PreviewSystemRubricCriterionImportFromFileUseCase previewSystemRubricCriterionImportFromFileUseCase;
    private final AcceptSystemRubricCriterionImportUseCase acceptSystemRubricCriterionImportUseCase;
    private final PreviewSchoolRubricCriterionImportFromFileUseCase previewSchoolRubricCriterionImportFromFileUseCase;;
    private final AcceptSchoolRubricCriterionImportUseCase acceptSchoolRubricCriterionImportUseCase;
    private final PreviewSystemRubricResultBandImportFromFileUseCase previewSystemRubricResultBandImportFromFileUseCase;
    private final AcceptSystemRubricResultBandImportUseCase acceptSystemRubricResultBandImportUseCase;
    private final PreviewSchoolRubricResultBandImportFromFileUseCase previewSchoolRubricResultBandImportFromFileUseCase;
    private final AcceptSchoolRubricResultBandImportUseCase acceptSchoolRubricResultBandImportUseCase;

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
                            ChangeSchoolRubricVersionStatusUseCase changeSchoolRubricVersionStatusUseCase, PreviewSystemRubricVersionImportFromFileUseCase previewSystemRubricVersionImportFromFileUseCase, AcceptSystemRubricVersionImportUseCase acceptSystemRubricVersionImportUseCase, AddSystemRubricVersionsUseCase addSystemRubricVersionsUseCase, AcceptSchoolRubricVersionImportUseCase acceptSchoolRubricVersionImportUseCase, PreviewSchoolRubricVersionImportFromFileUseCase previewSchoolRubricVersionImportFromFileUseCase, AddSchoolRubricVersionsUseCase addSchoolRubricVersionsUseCase, PreviewSystemRubricCriterionImportFromFileUseCase previewSystemRubricCriterionImportFromFileUseCase, AcceptSystemRubricCriterionImportUseCase acceptSystemRubricCriterionImportUseCase, PreviewSystemRubricCriterionImportFromFileUseCase previewSchoolRubricCriterionImportFromFileUseCase, PreviewSchoolRubricCriterionImportFromFileUseCase previewSchoolRubricCriterionImportFromFileUseCase1, AcceptSchoolRubricCriterionImportUseCase acceptSchoolRubricCriterionImportUseCase, PreviewSystemRubricResultBandImportFromFileUseCase previewSystemRubricResultBandImportFromFileUseCase, AcceptSystemRubricResultBandImportUseCase acceptSystemRubricResultBandImportUseCase, PreviewSchoolRubricResultBandImportFromFileUseCase previewSchoolRubricResultBandImportFromFileUseCase, AcceptSchoolRubricResultBandImportUseCase acceptSchoolRubricResultBandImportUseCase, ArchiveSystemRubricVersionUseCase archiveSystemRubricVersionUseCase, ArchiveSchoolRubricVersionUseCase archiveSchoolRubricVersionUseCase) {
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
        this.previewSystemRubricVersionImportFromFileUseCase = previewSystemRubricVersionImportFromFileUseCase;
        this.acceptSystemRubricVersionImportUseCase = acceptSystemRubricVersionImportUseCase;
        this.addSystemRubricVersionsUseCase = addSystemRubricVersionsUseCase;
        this.acceptSchoolRubricVersionImportUseCase = acceptSchoolRubricVersionImportUseCase;
        this.previewSchoolRubricVersionImportFromFileUseCase = previewSchoolRubricVersionImportFromFileUseCase;
        this.addSchoolRubricVersionsUseCase = addSchoolRubricVersionsUseCase;
        this.previewSystemRubricCriterionImportFromFileUseCase = previewSystemRubricCriterionImportFromFileUseCase;
        this.acceptSystemRubricCriterionImportUseCase = acceptSystemRubricCriterionImportUseCase;
        this.previewSchoolRubricCriterionImportFromFileUseCase = previewSchoolRubricCriterionImportFromFileUseCase1;
        this.acceptSchoolRubricCriterionImportUseCase = acceptSchoolRubricCriterionImportUseCase;
        this.previewSystemRubricResultBandImportFromFileUseCase = previewSystemRubricResultBandImportFromFileUseCase;
        this.acceptSystemRubricResultBandImportUseCase = acceptSystemRubricResultBandImportUseCase;
        this.previewSchoolRubricResultBandImportFromFileUseCase = previewSchoolRubricResultBandImportFromFileUseCase;
        this.acceptSchoolRubricResultBandImportUseCase = acceptSchoolRubricResultBandImportUseCase;
        this.archiveSystemRubricVersionUseCase = archiveSystemRubricVersionUseCase;
        this.archiveSchoolRubricVersionUseCase = archiveSchoolRubricVersionUseCase;
    }

    //==========================RUBRIC  & RUBRIC VERSION===================================

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


    // Review File Import Rubric Version của hệ thống
    @Operation(summary = "Upload file Excel/CSV danh sách Rubric Version cho hệ thống (Bước 1: Preview)")
    @PostMapping(value = "/system/rubrics/{rubricId}/versions/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<PreviewRubricVersionImportResponse>> previewSystemRubricVersionImport(
            @PathVariable("rubricId") UUID rubricId,
            @RequestParam("file") MultipartFile file) {

        var command = PreviewRubricVersionImportFromFileCommandMapper.fromSystemRequest(rubricId, file);
        var sessionId = previewSystemRubricVersionImportFromFileUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Nạp file danh sách phiên bản thành công. Tạo phiên làm việc (Session) hoàn tất.", sessionId));
    }

    //Xác nhận import Rubric Version của hệ thống
    @Operation(summary = "Xác nhận và nhập dữ liệu Rubric Version vào DB (Bước 2: Accept)")
    @PostMapping(value = "/system/rubrics/versions/import/{sessionId}/accept")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<AcceptRubricVersionImportResponse>> acceptSystemRubricVersionImport(
            @PathVariable("sessionId") UUID sessionId,
            @Valid @RequestBody AcceptImportRequest request) {

        // 1. Map dữ liệu
        var command = AcceptRubricVersionImportCommandMapper.fromSystemRequest(sessionId, request);
        var response = acceptSystemRubricVersionImportUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Đã đưa yêu cầu Import Phiên bản hệ thống vào hàng đợi ngầm xử lý.", response));
    }


    // KHi người dùng ko muốn dùng import mà chỉ muốn thêm version từ màn hình
    @Operation(summary = "Thêm mới danh sách Rubric Version bằng tay (Form UI)")
    @PostMapping("/system/rubrics/{rubricId}/versions")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<List<UUID>>> addSystemRubricVersions(
            @PathVariable("rubricId") UUID rubricId,
            @Valid @RequestBody AddRubricVersionsRequest request
    ) {
        var command = AddRubricVersionsCommandMapper.fromSystemRequest(rubricId, request);
        var newVersionIds = addSystemRubricVersionsUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Đã thêm thành công " + newVersionIds.size() + " phiên bản mới vào Rubric.", newVersionIds));
    }

    // Tạo Rubric của trường
    @Operation(summary = "Tạo mới một bộ tiêu chí (Rubric) cho trường học UI")
    @PostMapping("/schools/{schoolId}/rubrics")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> createSchoolRubric(
            @PathVariable("schoolId") UUID schoolId,
            @Valid @RequestBody CreateSchoolRubricRequest request
    ) {
        var command = CreateSchoolRubricCommandMapper.fromRequest(schoolId, request);
        var response = createSchoolRubricUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Khởi tạo bộ tiêu chí nháp thành công", response));
    }


    // Preview Rubric Version của trường học
    @Operation(summary = "Upload file Excel/CSV danh sách Rubric Version cho Trường học (Bước 1: Preview)")
    @PostMapping(value = "/schools/{schoolId}/rubrics/{rubricId}/versions/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<PreviewRubricVersionImportResponse>> previewSchoolRubricVersionImport(
            @PathVariable("schoolId") UUID schoolId,
            @PathVariable("rubricId") UUID rubricId,
            @RequestParam("file") MultipartFile file) {

        var command = PreviewRubricVersionImportFromFileCommandMapper.fromSchoolRequest(schoolId, rubricId, file);
        var sessionId = previewSchoolRubricVersionImportFromFileUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Nạp file danh sách phiên bản thành công. Tạo phiên làm việc (Session) hoàn tất.", sessionId));
    }

    // Chấp nhận import rubric version của trường
    @Operation(summary = "Xác nhận và nhập dữ liệu Rubric Version cho Trường học từ Session nháp (Bước 2: Accept)")
    @PostMapping(value = "/schools/{schoolId}/rubrics/versions/import/{sessionId}/accept")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<AcceptRubricVersionImportResponse>> acceptSchoolRubricVersionImport(
            @PathVariable("schoolId") UUID schoolId,
            @PathVariable("sessionId") UUID sessionId,
            @Valid @RequestBody AcceptImportRequest request) {
        var command = AcceptRubricVersionImportCommandMapper.fromSchoolRequestId(schoolId, sessionId, request);
        var response = acceptSchoolRubricVersionImportUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Đã đưa yêu cầu Import Phiên bản của trường vào hàng đợi ngầm xử lý.", response));
    }

    // Thêm 1 rubic version của trường học (FROM-UI)
    @Operation(summary = "Thêm mới danh sách Rubric Version bằng tay (Form UI) cho Trường học")
    @PostMapping("/schools/{schoolId}/rubrics/{rubricId}/versions")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<List<UUID>>> addSchoolRubricVersions(
            @PathVariable("schoolId") UUID schoolId,
            @PathVariable("rubricId") UUID rubricId,
            @Valid @RequestBody AddRubricVersionsRequest request
    ) {
        var command = AddRubricVersionsCommandMapper.fromSchoolRequest(schoolId, rubricId, request);
        var newVersionIds = addSchoolRubricVersionsUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Đã thêm thành công " + newVersionIds.size() + " phiên bản mới vào Rubric của trường học.", newVersionIds));
    }


    //Xóa rubric version của trường -> ko cho xoa rubric gốc (đã tối ưu/chưa chạy lại) => check đợt 2
    @Operation(summary = "Xử lý phiên bản Rubric (Xóa nếu DRAFT, Lưu trữ nếu PUBLISHED)")
    @DeleteMapping("/schools/{schoolId}/rubric-versions/{versionId}")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteSchoolRubricVersion(
            @PathVariable("schoolId") UUID schoolId,
            @PathVariable("versionId") UUID versionId
    ) {
        var command = DeleteSchoolRubricCommandMapper.versionFromRequest(schoolId, versionId);
        deleteSchoolRubricVersionUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Xóa phiên bản Rubric thành công"));
    }


    //  dc xoa rubric gốc => Check đợt 2
    @Operation(summary = "Xóa toàn bộ Rubric của hệ thống (Chỉ cho phép khi chưa có bản nào được PUBLISHED/ARCHIVED)")
    @DeleteMapping("/system/rubrics/{rubricId}") // SỬA PATH Ở ĐÂY CHO CHUẨN
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteSystemRubric(
            @PathVariable("rubricId") UUID rubricId
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
            @PathVariable("versionId") UUID versionId
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
            @PathVariable("versionId") UUID versionId,
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
            @PathVariable("schoolId") UUID schoolId,
            @PathVariable("versionId") UUID versionId,
            @RequestParam(name = "status") RubricStatus status
    ) {
        var command = ChangeRubricVersionStatusCommandMapper.fromSchoolRequest(schoolId, versionId, status);
        var responseId = changeSchoolRubricVersionStatusUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Chuyển trạng thái phiên bản thành công!", responseId));
    }

    // Lưu trữ (ARCHIVE) Rubric Version hệ thống đang PUBLISHED
    @Operation(summary = "Lưu trữ (ARCHIVE) Phiên bản Rubric Hệ thống đang PUBLISHED")
    @PatchMapping("/system/rubric-versions/{versionId}/archive")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> archiveSystemRubricVersion(
            @PathVariable("versionId") UUID versionId
    ) {
        var responseId = archiveSystemRubricVersionUseCase.execute(new ArchiveSystemRubricVersionCommand(versionId));
        return ResponseEntity.ok(ApiResponse.success("Lưu trữ phiên bản Rubric hệ thống thành công", responseId));
    }

    // Lưu trữ (ARCHIVE) Rubric Version của trường học đang PUBLISHED
    @Operation(summary = "Lưu trữ (ARCHIVE) Phiên bản Rubric Trường học đang PUBLISHED")
    @PatchMapping("/schools/{schoolId}/rubric-versions/{versionId}/archive")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> archiveSchoolRubricVersion(
            @PathVariable("schoolId") UUID schoolId,
            @PathVariable("versionId") UUID versionId
    ) {
        var responseId = archiveSchoolRubricVersionUseCase.execute(new ArchiveSchoolRubricVersionCommand(schoolId, versionId));
        return ResponseEntity.ok(ApiResponse.success("Lưu trữ phiên bản Rubric của trường học thành công", responseId));
    }


    //=====================RUBRIC CRITERION===============================
    //Thêm Rubric Creitrion vào Rubric Version đã tối ưu/ chưa chạy lại =>check 2
    @Operation(summary = "Thêm một Tiêu chí (Criterion) vào Phiên bản Rubric (RubricVersion) cho trường học")
    @PostMapping("/schools/{schoolId}/rubric-versions/{versionId}/criteria")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<List<UUID>>> createSchoolRubricCriterion(
            @PathVariable("schoolId") UUID schoolId,
            @PathVariable("versionId") UUID versionId,
            @Valid @RequestBody CreateSchoolRubricCriteriaRequest request
    ) {
        var command = CreateSchoolRubricCriteriaCommandMapper.fromRequest(schoolId, versionId, request);
        var newCriterionId = createSchoolRubricCriterionUseCase.execute(command);

        return ResponseEntity.ok(ApiResponse.success("Thêm tiêu chí thành công", newCriterionId));
    }


    // Phục vụ cho việc tạo bài kiểm tra đầu vào (cá nhân hóa đánh giá năng lực)
    // đã check 2
    @Operation(summary = "Thêm tiêu chí Rubric Criterion cho Rubric version hệ thống")
    @PostMapping("/system/rubric-versions/{versionId}/criteria")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<List<UUID>>> createSystemRubricCriterion(
            @PathVariable("versionId") UUID versionId,
            @Valid @RequestBody CreateSystemRubricCriteriaRequest request
    ) {
        // Truyền versionId vào mapper
        var command = CreateSystemRubricCriteriaCommandMapper.fromRequest(versionId, request);
        var result = createSystemRubricCriteriaUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Thêm tiêu chí hệ thống thành công", result));
    }


    // Preview Rubric Rubrics Creitia của rubric version
    @Operation(summary = "Upload/Preview file Excel/CSV danh sách Tiêu chí Rubric cho hệ thống (Bước 1: Preview)")
    @PostMapping(value = "/system/rubrics/versions/{versionId}/criterions/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<PreviewRubricCriterionImportResponse>> previewSystemRubricCriterionImport(
            @PathVariable("versionId") UUID versionId,
            @RequestParam("file") MultipartFile file) {

        var command = PreviewRubricCriterionImportCommandMapper.fromSystemRequest(versionId, file);
        var response = previewSystemRubricCriterionImportFromFileUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Nạp file danh sách Tiêu chí thành công. Vui lòng kiểm tra màn hình Preview.", response));
    }


    // Chập nhận và add dữ liệu rubric rubri criteria từ rubric version
    @Operation(summary = "Xác nhận và nhập dữ liệu Tiêu chí (Rubric Criterion) vào DB (Bước 2: Accept)")
    @PostMapping(value = "/system/rubrics/criterions/import/{sessionId}/accept")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<AcceptRubricCriterionImportResponse>> acceptSystemRubricCriterionImport(
            @PathVariable("sessionId") UUID sessionId,
            @Valid @RequestBody AcceptImportRequest request) {

        var command = AcceptRubricCriterionImportCommandMapper.fromSystemRequest(sessionId, request);

        var response = acceptSystemRubricCriterionImportUseCase.execute(command);

        return ResponseEntity.ok(ApiResponse.success("Đã đưa yêu cầu Import Tiêu chí hệ thống vào hàng đợi ngầm xử lý.", response));
    }

    // Review Rubric Criteria version của trường
    @Operation(summary = "Upload/Preview file Excel/CSV danh sách Tiêu chí Rubric cho trường (Bước 1: Preview)")
    @PostMapping(value = "/schools/{schoolId}/rubric-versions/{versionId}/criterions/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<PreviewRubricCriterionImportResponse> previewSchoolRubricCriterionImport(
            @PathVariable("schoolId") UUID schoolId,
            @PathVariable("versionId") UUID versionId,
            @RequestParam("file") MultipartFile file) {

        var command = PreviewRubricCriterionImportCommandMapper.fromSchoolRequest(schoolId, versionId, file);
        var response = previewSchoolRubricCriterionImportFromFileUseCase.execute(command);
        return ResponseEntity.ok(response);
    }

    // Xác nhận tiêu chí và add rubric criteria và rubric version
    @Operation(summary = "Xác nhận và nhập dữ liệu Tiêu chí (Rubric Criterion) vào DB (Bước 2: Accept)")
    @PostMapping("/schools/{schoolId}/rubric-versions/import-sessions/{sessionId}/accept")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<AcceptRubricCriterionImportResponse> acceptSchoolRubricCriterionImport(
            @PathVariable("schoolId") UUID schoolId,
            @PathVariable("sessionId") UUID sessionId,
            @Valid @RequestBody AcceptImportRequest request) {

        // Sử dụng chính xác Mapper từ School Request của bác để tạo Command dùng chung
        var command = AcceptRubricCriterionImportCommandMapper.fromSchoolRequest(schoolId, sessionId, request);

        var response = acceptSchoolRubricCriterionImportUseCase.execute(command);
        return ResponseEntity.ok(response);
    }


    // Xóa 1 version Rubric khỏi rubric của trường
    // đã tối ưu / chưa chạy lại
    //Check 2
    @Operation(summary = "Xóa một Tiêu chí khỏi phiên bản Rubric của trường")
    @DeleteMapping("/schools/{schoolId}/rubric-versions/{versionId}/criteria/{criterionId}")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteSchoolRubricCriterion(
            @PathVariable("schoolId") UUID schoolId,
            @PathVariable("versionId") UUID versionId,
            @PathVariable("criterionId") UUID criterionId
    ) {
        var command = DeleteSchoolRubricCommandMapper.criterionFromRequest(schoolId, versionId, criterionId);
        deleteSchoolRubricCriterionUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Xóa tiêu chí thành công"));
    }


    // Xóa tiêu chí rubric trong hệ thống
    // Đã tối ưu / chưa chạy lại
    // check 2
    @Operation(summary = "Xóa một Tiêu chí(Rubric Version) khỏi phiên bản Rubric Hệ thống")
    @DeleteMapping("/system/rubric-versions/{versionId}/criteria/{criterionId}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteSystemRubricCriterion(
            @PathVariable("versionId") UUID versionId,
            @PathVariable("criterionId") UUID criterionId
    ) {
        var command = DeleteSystemRubricCommandMapper.criterionFromRequest(versionId, criterionId);
        deleteSystemRubricCriterionUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Xóa tiêu chí hệ thống thành công"));
    }

    //=====================RUBRIC RESULT BAND===============================


    //Phục vụ: Đánh giá sinh viên của system (Giỏi, Khá, Trung Bình) : UI
    //Đã tối ưu/ chưa chạy lại
    // Check 2
    @Operation(summary = "Thêm một Thang điểm kết quả/đánh giá vào Rubric Hệ thống")
    @PostMapping("/system/rubric-versions/{versionId}/result-bands")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<List<UUID>>> createSystemRubricResultBand(
            @PathVariable("versionId") UUID versionId,
            @Valid @RequestBody CreateSystemRubricResultBandsRequest request // Nhớ tạo thêm Request này nhé
    ) {
        var command = CreateSystemRubricResultBandsCommandMapper.fromRequest(versionId, request);
        var resultId = createSystemRubricResultBandsUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Thêm thang điểm hệ thống thành công", resultId));
    }


    // Phục vụ : Đánh giá sinh viên của trường (Giỏi, Khá, Trung Bình) UI
    //Đã tối ưu/ chưa chạy lại
    // Check 2 UI
    @Operation(summary = "Thêm một Thang điểm kết quả/đánh giá (Result Band) vào Phiên bản Rubric của Trường")
    @PostMapping("/schools/{schoolId}/rubric-versions/{versionId}/result-bands")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<List<UUID>>> createSchoolRubricResultBand(
            @PathVariable("schoolId") UUID schoolId,
            @PathVariable("versionId") UUID versionId,
            @Valid @RequestBody CreateSchoolRubricResultBandsRequest request
    ) {
        var command = CreateSchoolRubricResultBandsCommandMapper.fromRequest(schoolId, versionId, request);
        var newResultBandId = createSchoolRubricResultBandsUseCase.execute(command);

        return ResponseEntity.ok(ApiResponse.success("Thêm thang điểm kết quả thành công", newResultBandId));
    }


    // API: XÓA LẺ 1 THANG ĐIỂM KẾT QUẢ (RESULT BAND)
    //Check 2
    @Operation(summary = "Xóa một Thang điểm kết quả khỏi phiên bản Rubric")
    @DeleteMapping("/schools/{schoolId}/rubric-versions/{versionId}/result-bands/{resultBandId}")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteSchoolRubricResultBand(
            @PathVariable("schoolId") UUID schoolId,
            @PathVariable("versionId") UUID versionId,
            @PathVariable("resultBandId") UUID resultBandId
    ) {
        var command = DeleteSchoolRubricCommandMapper.resultBandFromRequest(schoolId, versionId, resultBandId);
        deleteSchoolRubricResultBandUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Xóa thang điểm thành công"));
    }


    // Xóa 1 Rubric Result band khổi rubric version
    @Operation(summary = "Xóa một Thang điểm khỏi phiên bản Rubric Hệ thống")
    @DeleteMapping("/system/rubric-versions/{versionId}/result-bands/{resultBandId}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteSystemRubricResultBand(
            @PathVariable("versionId") UUID versionId,
            @PathVariable("resultBandId") UUID resultBandId
    ) {
        var command = DeleteSystemRubricCommandMapper.resultBandFromRequest(versionId, resultBandId);
        deleteSystemRubricResultBandUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Xóa thang điểm hệ thống thành công"));
    }

    // 1. Preview Result Band (Dành riêng cho System Admin)
    @Operation(summary = "Preview file import Mức độ/Xếp loại kết quả (Result Band) cho Hệ thống")
    @PostMapping(value = "/system/versions/{versionId}/result-bands/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<PreviewRubricResultBandImportResponse>> previewSystemRubricResultBandImport(
            @PathVariable("versionId") UUID versionId,
            @RequestParam("file") MultipartFile file) {

        // Gọi đúng hàm fromSystemRequest (truyền schoolId = null)
        var command = PreviewRubricResultBandImportCommandMapper.fromSystemRequest(versionId, file);
        var response = previewSystemRubricResultBandImportFromFileUseCase.execute(command);

        return ResponseEntity.ok(ApiResponse.success("Nạp cấu trúc file xếp loại hệ thống thành công.", response));
    }

    // 2. Accept Result Band (Dành riêng cho System Admin)
    @Operation(summary = "Xác nhận và đẩy yêu cầu Import Xếp loại của hệ thống ")
    @PostMapping(value = "/system/versions/result-bands/import/{sessionId}/accept")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<AcceptRubricResultBandImportResponse>> acceptSystemRubricResultBandImport(
            @PathVariable("sessionId") UUID sessionId,
            @Valid @RequestBody AcceptImportRequest request) {

        var command = AcceptRubricResultBandImportCommandMapper.fromSystemRequest(sessionId, request);
        var response = acceptSystemRubricResultBandImportUseCase.execute(command);

        return ResponseEntity.ok(ApiResponse.success("Đã đưa yêu cầu import xếp loại hệ thống vào hàng đợi ngầm xử lý.", response));
    }


    // 1. Preview Result Band (Dành cho School Admin)
    @Operation(
            summary = "Bước 1: Nạp file và Xem trước dữ liệu Import Xếp loại (Preview)",
            description = "Nhận file Excel/CSV, bóc tách cấu trúc cột và trả về dữ liệu mẫu để School Admin kiểm tra trước khi xác nhận nhập vào DB."
    )
    @PostMapping(value = "/school/{schoolId}/rubric-versions/{versionId}/result-bands/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<PreviewRubricResultBandImportResponse> previewSchoolRubricResultBandImport(
            @PathVariable("schoolId") UUID schoolId,
            @PathVariable("versionId") UUID versionId,
            @RequestParam("file") MultipartFile file) {

        // Gọi Mapper bóc tách dữ liệu từ file và đính kèm schoolId bảo mật
        var command = PreviewRubricResultBandImportCommandMapper.fromSchoolRequest(schoolId, versionId, file);
        var response = previewSchoolRubricResultBandImportFromFileUseCase.execute(command);

        return ResponseEntity.ok(response);
    }

    // 2. Accept Result Band (Dành cho School Admin)
    @Operation(
            summary = "Bước 2: Xác nhận Import Xếp loại và đưa vào hàng đợi ngầm (Accept)",
            description = "School Admin xác nhận dữ liệu hợp lệ. Hệ thống sẽ chuyển trạng thái Session sang QUEUED để tiến hành import ngầm (Async) vào Database."
    )
    @PostMapping(value = "/school/{schoolId}/rubric-versions/result-bands/import/{sessionId}/accept")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<AcceptRubricResultBandImportResponse> acceptSchoolRubricResultBandImport(
            @PathVariable("schoolId") UUID schoolId,
            @PathVariable("sessionId") UUID sessionId,
            @Valid @RequestBody AcceptImportRequest request) {

        // Gọi Mapper đóng gói lệnh đồng ý import ngầm từ School
        var command = AcceptRubricResultBandImportCommandMapper.fromSchoolRequest(schoolId, sessionId, request);
        var response = acceptSchoolRubricResultBandImportUseCase.execute(command);

        return ResponseEntity.ok(response);
    }

}
