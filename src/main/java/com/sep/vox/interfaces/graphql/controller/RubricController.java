package com.sep.vox.interfaces.graphql.controller;


import com.sep.vox.application.port.input.query.*;
import com.sep.vox.application.port.input.query.key.RubricCriteriaKey;
import com.sep.vox.application.port.input.query.key.RubricResultBandsKey;
import com.sep.vox.application.port.input.query.key.RubricVersionsKey;
import com.sep.vox.application.port.input.usecase.rubricschool.*;
import com.sep.vox.application.port.input.usecase.rubricsystem.*;
import com.sep.vox.application.port.input.usecase.rubricteacher.*;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.*;
import com.sep.vox.interfaces.graphql.dto.request.*;
import com.sep.vox.interfaces.graphql.mapper.*;
import graphql.schema.DataFetchingEnvironment;
import jakarta.validation.Valid;
import org.dataloader.DataLoader;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Controller("graphqlRubricController")
public class RubricController {

    private final UpdateSystemRubricUseCase updateSystemRubricUseCase;
    private final UpdateSchoolRubricUseCase updateSchoolRubricUseCase;
    private final ViewSystemRubricDetailsUseCase viewSystemRubricDetailsUseCase;
    private final ViewSchoolRubricDetailsUseCase viewSchoolRubricDetailsUseCase;
    private final ViewSystemRubricsUseCase viewSystemRubricsUseCase;
    private final ViewSchoolRubricsUseCase viewSchoolRubricsUseCase;
    private final UpdateSystemRubricVersionUseCase updateSystemRubricVersionUseCase;
    private final UpdateSchoolRubricVersionUseCase updateSchoolRubricVersionUseCase;
    private final ViewSchoolRubricVersionDetailsUseCase viewSchoolRubricVersionDetailsUseCase;
    private final ViewSystemRubricVersionDetailsUseCase viewSystemRubricVersionDetailsUseCase;
    private final ViewSchoolRubricVersionsUseCase viewSchoolRubricVersionsUseCase;
    private final ViewSystemRubricVersionsUseCase viewSystemRubricVersionsUseCase;
    private final ViewTeacherRubricVersionsUseCase viewTeacherRubricVersionsUseCase;
    private final UpdateSystemRubricCriterionUseCase updateSystemRubricCriterionUseCase;
    private final UpdateSchoolRubricCriterionUseCase updateSchoolRubricCriterionUseCase;
    private final ViewSystemRubricCriterionDetailsUseCase viewSystemRubricCriterionDetailsUseCase;
    private final ViewSchoolRubricCriterionDetailsUseCase viewSchoolRubricCriterionDetailsUseCase;
    private final ViewSystemRubricCriteriaUseCase viewSystemRubricCriteriaUseCase;
    private final ViewSchoolRubricCriteriaUseCase viewSchoolRubricCriteriaUseCase;
    private final UpdateSystemRubricResultBandUseCase updateSystemRubricResultBandUseCase;
    private final UpdateSchoolRubricResultBandUseCase updateSchoolRubricResultBandUseCase;
    private final ViewSystemRubricResultBandDetailsUseCase viewSystemRubricResultBandDetailsUseCase;
    private final ViewSchoolRubricResultBandDetailsUseCase viewSchoolRubricResultBandDetailsUseCase;
    private final ViewSystemRubricResultBandsUseCase viewSystemRubricResultBandsUseCase;
    private final ViewSchoolRubricResultBandsUseCase viewSchoolRubricResultBandsUseCase;
    private final SearchSystemRubricsUseCase searchSystemRubricsUseCase;
    private final SearchSchoolRubricsUseCase searchSchoolRubricsUseCase;
    private final SearchTeacherRubricsUseCase searchTeacherRubricsUseCase;
    private final SearchSystemRubricVersionsUseCase searchSystemRubricVersionsUseCase;
    private final SearchSchoolRubricVersionsUseCase searchSchoolRubricVersionsUseCase;
    private final SearchSchoolRubricCriteriaUseCase searchSchoolRubricCriteriaUseCase;
    private final SearchSystemRubricCriteriaUseCase searchSystemRubricCriteriaUseCase;
    private final SearchSystemRubricResultBandsUseCase searchSystemRubricResultBandsUseCase;
    private final SearchSchoolRubricResultBandsUseCase searchSchoolRubricResultBandsUseCase;

    public RubricController(UpdateSystemRubricUseCase updateSystemRubricUseCase, UpdateSchoolRubricUseCase updateSchoolRubricUseCase, ViewSystemRubricDetailsUseCase viewSystemRubricDetailsUseCase, ViewSchoolRubricDetailsUseCase viewSchoolRubricDetailsUseCase, ViewSystemRubricsUseCase viewSystemRubricsUseCase, ViewSchoolRubricsUseCase viewSchoolRubricsUseCase, UpdateSystemRubricVersionUseCase updateSystemRubricVersionUseCase, UpdateSchoolRubricVersionUseCase updateSchoolRubricVersionUseCase, ViewSchoolRubricVersionDetailsUseCase viewSchoolRubricVersionDetailsUseCase, ViewSystemRubricVersionDetailsUseCase viewSystemRubricVersionDetailsUseCase, ViewSchoolRubricVersionsUseCase viewSchoolRubricVersionsUseCase, ViewSystemRubricVersionsUseCase viewSystemRubricVersionsUseCase, ViewTeacherRubricVersionsUseCase viewTeacherRubricVersionsUseCase, UpdateSystemRubricCriterionUseCase updateSystemRubricCriterionUseCase, UpdateSchoolRubricCriterionUseCase updateSchoolRubricCriterionUseCase, ViewSystemRubricCriterionDetailsUseCase viewSystemRubricCriterionDetailsUseCase, ViewSchoolRubricCriterionDetailsUseCase viewSchoolRubricCriterionDetailsUseCase, ViewSystemRubricCriteriaUseCase viewSystemRubricCriteriaUseCase, ViewSchoolRubricCriteriaUseCase viewSchoolRubricCriteriaUseCase, UpdateSystemRubricResultBandUseCase updateSystemRubricResultBandUseCase, UpdateSchoolRubricResultBandUseCase updateSchoolRubricResultBandUseCase, ViewSystemRubricResultBandDetailsUseCase viewSystemRubricResultBandDetailsUseCase, ViewSchoolRubricResultBandDetailsUseCase viewSchoolRubricResultBandDetailsUseCase, ViewSystemRubricResultBandsUseCase viewSystemRubricResultBandsUseCase, ViewSchoolRubricResultBandsUseCase viewSchoolRubricResultBandsUseCase, SearchSystemRubricsUseCase searchSystemRubricsUseCase, SearchSchoolRubricsUseCase searchSchoolRubricsUseCase, SearchTeacherRubricsUseCase searchTeacherRubricsUseCase, SearchSystemRubricVersionsUseCase searchSystemRubricVersionsUseCase, SearchSchoolRubricVersionsUseCase searchSchoolRubricVersionsUseCase, SearchSchoolRubricCriteriaUseCase searchSchoolRubricCriteriaUseCase, SearchSystemRubricCriteriaUseCase searchSystemRubricCriteriaUseCase, SearchSystemRubricResultBandsUseCase searchSystemRubricResultBandsUseCase, SearchSchoolRubricResultBandsUseCase searchSchoolRubricResultBandsUseCase) {
        this.updateSystemRubricUseCase = updateSystemRubricUseCase;
        this.updateSchoolRubricUseCase = updateSchoolRubricUseCase;
        this.viewSystemRubricDetailsUseCase = viewSystemRubricDetailsUseCase;
        this.viewSchoolRubricDetailsUseCase = viewSchoolRubricDetailsUseCase;
        this.viewSystemRubricsUseCase = viewSystemRubricsUseCase;
        this.viewSchoolRubricsUseCase = viewSchoolRubricsUseCase;
        this.updateSystemRubricVersionUseCase = updateSystemRubricVersionUseCase;
        this.updateSchoolRubricVersionUseCase = updateSchoolRubricVersionUseCase;
        this.viewSchoolRubricVersionDetailsUseCase = viewSchoolRubricVersionDetailsUseCase;
        this.viewSystemRubricVersionDetailsUseCase = viewSystemRubricVersionDetailsUseCase;
        this.viewSchoolRubricVersionsUseCase = viewSchoolRubricVersionsUseCase;
        this.viewSystemRubricVersionsUseCase = viewSystemRubricVersionsUseCase;
        this.viewTeacherRubricVersionsUseCase = viewTeacherRubricVersionsUseCase;
        this.updateSystemRubricCriterionUseCase = updateSystemRubricCriterionUseCase;
        this.updateSchoolRubricCriterionUseCase = updateSchoolRubricCriterionUseCase;
        this.viewSystemRubricCriterionDetailsUseCase = viewSystemRubricCriterionDetailsUseCase;
        this.viewSchoolRubricCriterionDetailsUseCase = viewSchoolRubricCriterionDetailsUseCase;
        this.viewSystemRubricCriteriaUseCase = viewSystemRubricCriteriaUseCase;
        this.viewSchoolRubricCriteriaUseCase = viewSchoolRubricCriteriaUseCase;
        this.updateSystemRubricResultBandUseCase = updateSystemRubricResultBandUseCase;
        this.updateSchoolRubricResultBandUseCase = updateSchoolRubricResultBandUseCase;
        this.viewSystemRubricResultBandDetailsUseCase = viewSystemRubricResultBandDetailsUseCase;
        this.viewSchoolRubricResultBandDetailsUseCase = viewSchoolRubricResultBandDetailsUseCase;
        this.viewSystemRubricResultBandsUseCase = viewSystemRubricResultBandsUseCase;
        this.viewSchoolRubricResultBandsUseCase = viewSchoolRubricResultBandsUseCase;
        this.searchSystemRubricsUseCase = searchSystemRubricsUseCase;
        this.searchSchoolRubricsUseCase = searchSchoolRubricsUseCase;
        this.searchTeacherRubricsUseCase = searchTeacherRubricsUseCase;
        this.searchSystemRubricVersionsUseCase = searchSystemRubricVersionsUseCase;
        this.searchSchoolRubricVersionsUseCase = searchSchoolRubricVersionsUseCase;
        this.searchSchoolRubricCriteriaUseCase = searchSchoolRubricCriteriaUseCase;
        this.searchSystemRubricCriteriaUseCase = searchSystemRubricCriteriaUseCase;
        this.searchSystemRubricResultBandsUseCase = searchSystemRubricResultBandsUseCase;
        this.searchSchoolRubricResultBandsUseCase = searchSchoolRubricResultBandsUseCase;
    }


    //============================== RUBRIC ========================================
    //Update Rubric của hệ thống
    @MutationMapping(name = "updateSystemRubric")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public UUID updateSystemRubric(
            @Argument(name = "id") UUID id,
            @Argument(name = "input") UpdateRubricInput input
    ) {
        var command = UpdateRubricGraphQLMapper.fromSystemInput(id, input);

        return updateSystemRubricUseCase.execute(command);
    }


    //Update Rubric của trường học
    @MutationMapping(name = "updateSchoolRubric")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public UUID updateSchoolRubric(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "id") UUID id,
            @Valid @Argument(name = "input") UpdateRubricInput input
    ) {
        var command = UpdateRubricGraphQLMapper.fromSchoolInput(schoolId, id, input);
        return updateSchoolRubricUseCase.execute(command);
    }


    //View Rubric của hệ thống
    @QueryMapping(name = "viewSystemRubric")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public RubricDto viewSystemRubricById(@Argument(name = "id") UUID id) {
        var query = new ViewSystemRubricDetailsQuery(id);
        return viewSystemRubricDetailsUseCase.execute(query);
    }


    //View Rubric của trường học
    @QueryMapping(name = "viewSchoolRubric")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public RubricDto viewSchoolRubricById(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "id") UUID id
    ) {
        var query = new ViewSchoolRubricDetailsQuery(schoolId, id);
        return viewSchoolRubricDetailsUseCase.execute(query);
    }


    //View Rubrics của hệ thống
    @QueryMapping(name = "viewSystemRubrics")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public PageResult<RubricDto> viewSystemRubrics(
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size
    ) {
        // 1. Chặn ngay từ cửa nếu Frontend gửi data sai (page <= 0 hoặc size <= 0)
        if (page != null && page <= 0) {
            throw new IllegalArgumentException("Tham số 'page' không hợp lệ. Trang phải bắt đầu từ 1.");
        }
        if (size != null && size <= 0) {
            throw new IllegalArgumentException("Tham số 'size' không hợp lệ. Số lượng phần tử phải lớn hơn 0.");
        }

        // 2. Gán giá trị mặc định nếu Frontend không truyền (Quy ước mặc định là trang 1)
        int validPage = page != null ? page - 1 : 0;
        int pageSize = (size != null) ? size : 10;

        // 3. Đẩy xuống UseCase
        // LƯU Ý CỰC QUAN TRỌNG: Vì Spring Data JPA đếm page từ 0,
        // nên ta phải lấy (pageNumber - 1) trước khi gọi DB.

        var query = new ViewSystemRubricsQuery(validPage, pageSize);
        return viewSystemRubricsUseCase.execute(query);
    }


    //View Rubrics của trường học
    @QueryMapping(name = "viewSchoolRubrics")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public PageResult<RubricDto> viewSchoolRubrics(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size
    ) {
        if (page != null && page <= 0) {
            throw new IllegalArgumentException("Tham số 'page' không hợp lệ. Trang phải bắt đầu từ 1.");
        }
        if (size != null && size <= 0) {
            throw new IllegalArgumentException("Tham số 'size' không hợp lệ. Số lượng phần tử phải lớn hơn 0.");
        }

        int validPage = page != null ? page - 1 : 0;
        int pageSize = (size != null) ? size : 10;

        var query = new ViewSchoolRubricsQuery(schoolId, validPage, pageSize);
        return viewSchoolRubricsUseCase.execute(query);
    }

    @SchemaMapping(typeName = "Rubric", field = "language")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public CompletableFuture<SupportedLanguageDto> getLanguage(
            RubricDto rubric,
            DataFetchingEnvironment env) {

        DataLoader<UUID, SupportedLanguageDto> loader = env.getDataLoader("supportedLanguageDataLoader");
        if (loader == null) {
            throw new IllegalStateException("Không tìm thấy DataLoader: supportedLanguageDataLoader");
        }
        return loader.load(rubric.languageId());
    }

    @SchemaMapping(typeName = "Rubric", field = "framework")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public CompletableFuture<FrameworkDto> getFramework(
            RubricDto rubric,
            DataFetchingEnvironment env) {

        DataLoader<UUID, FrameworkDto> loader = env.getDataLoader("frameworkDataLoader");
        if (loader == null) {
            throw new IllegalStateException("Không tìm thấy DataLoader: frameworkDataLoader");
        }
        return loader.load(rubric.frameworkId());
    }


    //Lấy tất cẩ Rubric Version của trường
    // Ko thể tách ra được do dùng chung 1 type bên scheme
    // Xử lý Role
    @SchemaMapping(typeName = "Rubric", field = "versions")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public CompletableFuture<PageResult<RubricVersionDto>> getRubricVersions(
            RubricDto rubric,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size,
            @Argument(name = "status") String status,
            DataFetchingEnvironment env) {

        int validPage = page != null ? page - 1 : 0;
        int validSize = (size != null && size > 0) ? size : 10;

        DataLoader<RubricVersionsKey, PageResult<RubricVersionDto>> loader = env.getDataLoader("rubricVersionsDataLoader");
        return loader.load(new RubricVersionsKey(rubric.id(), status, validPage, validSize));
    }


    //Search Rubric theo code, name của hệ thống
    @QueryMapping(name = "searchSystemRubrics")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public PageResult<RubricDto> searchSystemRubrics(
            @Argument(name = "filter") SearchRubricFilterRequest filter,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {

        // Nắn tham số 1-based (Frontend) về 0-based (Backend)
        int validPage = page != null ? page - 1 : 0;
        int validSize = (size != null && size > 0) ? size : 10;

        // Tránh NullPointerException nếu Frontend không truyền filter
        var safeFilter = (filter != null) ? filter : new SearchRubricFilterRequest(null, null, null);

        // Tạo Query của Clean Architecture (Lưu ý tham số thứ 4 là PageRequest)
        var query = new SearchSystemRubricsQuery(
                safeFilter.keyword(),
                safeFilter.frameworkId(),
                safeFilter.languageId(),
                validPage,
                validSize
        );

        // SỬA COMMENT: Thực thi UseCase của hệ thống (chứ không phải của trường)
        return searchSystemRubricsUseCase.execute(query);
    }


    //Search Rubric theo code, name của trường
    @QueryMapping(name = "searchSchoolRubrics")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public PageResult<RubricDto> searchSchoolRubrics(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "filter") SearchRubricFilterRequest filter,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {

        // 1. Nắn tham số 1-based về 0-based
        int validPage = page != null ? page - 1 : 0;
        int validSize = (size != null && size > 0) ? size : 10;

        // 2. Tránh NullPointer
        var safeFilter = (filter != null) ? filter : new SearchRubricFilterRequest(null, null, null);

        // 3. Khởi tạo Query gọn gàng (Gộp inline PageRequest)
        var query = new SearchSchoolRubricsQuery(
                schoolId,
                safeFilter.keyword(),
                safeFilter.frameworkId(),
                safeFilter.languageId(),
                validPage,
                validSize
        );
        return searchSchoolRubricsUseCase.execute(query);
    }

    //Search Rubric theo code, name của Teacher (chỉ rubric thuộc trường của Teacher)
    @QueryMapping(name = "searchTeacherRubrics")
    @PreAuthorize("hasRole('TEACHER')")
    public PageResult<RubricDto> searchTeacherRubrics(
            @Argument(name = "filter") SearchRubricFilterRequest filter,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {

        // 1. Nắn tham số 1-based về 0-based
        int validPage = page != null ? page - 1 : 0;
        int validSize = (size != null && size > 0) ? size : 10;

        // 2. Tránh NullPointer
        var safeFilter = (filter != null) ? filter : new SearchRubricFilterRequest(null, null, null);

        // 3. Khởi tạo Query (schoolId được UseCase tự suy ra từ Teacher đang đăng nhập)
        var query = new SearchTeacherRubricsQuery(
                safeFilter.keyword(),
                safeFilter.frameworkId(),
                safeFilter.languageId(),
                validPage,
                validSize
        );
        return searchTeacherRubricsUseCase.execute(query);
    }

    //========================== RUBRIC VERSION ===============================================
    // Update Rubric Version của hệ thống
    @MutationMapping(name = "updateSystemRubricVersion")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public UUID updateSystemRubricVersion(
            @Argument(name = "versionId") UUID versionId,
            @Argument(name = "input") UpdateRubricVersionInput input
    ) {
        // Sử dụng Mapper chuẩn men
        var command = UpdateRubricVersionGraphQLMapper.fromSystemInput(versionId, input);

        return updateSystemRubricVersionUseCase.execute(command);
    }


    // Update Rubric Version của school Admin
    @MutationMapping(name = "updateSchoolRubricVersion")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public UUID updateSchoolRubricVersion(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "versionId") UUID versionId,
            @Argument(name = "input") UpdateRubricVersionInput input
    ) {
        var command = UpdateRubricVersionGraphQLMapper.fromSchoolInput(schoolId, versionId, input);

        return updateSchoolRubricVersionUseCase.execute(command);
    }

    //View School Rubric Version Details của trường
    @QueryMapping(name = "viewSchoolRubricVersion")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public RubricVersionDto viewSchoolRubricVersion(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "versionId") UUID versionId
    ) {
        var query = new ViewSchoolRubricVersionDetailsQuery(schoolId, versionId);
        return viewSchoolRubricVersionDetailsUseCase.execute(query);
    }

    //View School Rubric Version Details của hệ thống
    @QueryMapping(name = "viewSystemRubricVersion")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public RubricVersionDto viewSystemRubricVersion(
            @Argument(name = "versionId") UUID versionId
    ) {
        var query = new ViewSystemRubricVersionDetailsQuery(versionId);
        return viewSystemRubricVersionDetailsUseCase.execute(query);
    }


    //View School Rubric Versions của trường
    @QueryMapping(name = "viewSchoolRubricVersions")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public PageResult<RubricVersionDto> viewSchoolRubricVersions(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "rubricId") UUID rubricId,
            @Argument(name = "status") String status,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size
    ) {
        if (page != null && page <= 0) {
            throw new IllegalArgumentException("Tham số 'page' không hợp lệ. Trang phải bắt đầu từ 1.");
        }
        if (size != null && size <= 0) {
            throw new IllegalArgumentException("Tham số 'size' không hợp lệ. Số lượng phần tử phải lớn hơn 0.");
        }

        int validPage = page != null ? page - 1 : 0;
        int pageSize = (size != null) ? size : 10;

        var query = new ViewSchoolRubricVersionsQuery(schoolId, rubricId, status, validPage, pageSize);
        return viewSchoolRubricVersionsUseCase.execute(query);
    }


    //View System Rubric Versions của hệ thống
    @QueryMapping(name = "viewSystemRubricVersions")
    @PreAuthorize("isAuthenticated()")
    public PageResult<RubricVersionDto> viewSystemRubricVersions(
            @Argument(name = "rubricId") UUID rubricId,
            @Argument(name = "status") String status,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size
    ) {
        if (page != null && page <= 0) {
            throw new IllegalArgumentException("Tham số 'page' không hợp lệ. Trang phải bắt đầu từ 1.");
        }
        if (size != null && size <= 0) {
            throw new IllegalArgumentException("Tham số 'size' không hợp lệ. Số lượng phần tử phải lớn hơn 0.");
        }

        int validPage = page != null ? page - 1 : 0;
        int pageSize = (size != null) ? size : 10;

        var query = new ViewSystemRubricVersionsQuery(rubricId, status, validPage, pageSize);
        return viewSystemRubricVersionsUseCase.execute(query);
    }

    //View Rubric Versions cho Teacher (chỉ PUBLISHED, chỉ rubric thuộc trường của Teacher)
    @QueryMapping(name = "viewTeacherRubricVersions")
    @PreAuthorize("hasRole('TEACHER')")
    public PageResult<RubricVersionDto> viewTeacherRubricVersions(
            @Argument(name = "rubricId") UUID rubricId,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size
    ) {
        if (page != null && page <= 0) {
            throw new IllegalArgumentException("Tham số 'page' không hợp lệ. Trang phải bắt đầu từ 1.");
        }
        if (size != null && size <= 0) {
            throw new IllegalArgumentException("Tham số 'size' không hợp lệ. Số lượng phần tử phải lớn hơn 0.");
        }

        int validPage = page != null ? page - 1 : 0;
        int pageSize = (size != null) ? size : 10;

        var query = new ViewTeacherRubricVersionsQuery(rubricId, validPage, pageSize);
        return viewTeacherRubricVersionsUseCase.execute(query);
    }

    // Lấy các tiêu chí(rubric criterions của rubric version
    @SchemaMapping(typeName = "RubricVersion", field = "criteria")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public CompletableFuture<PageResult<RubricCriterionDto>> getRubricCriteria(
            RubricVersionDto version,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size,
            DataFetchingEnvironment env) {

        // Validate & nắn từ 1-based về 0-based
        int validPage = page != null ? page - 1 : 0;
        int validSize = (size != null && size > 0) ? size : 10;

        DataLoader<RubricCriteriaKey, PageResult<RubricCriterionDto>> loader = env.getDataLoader("rubricCriteriaDataLoader");
        if (loader == null) {
            throw new IllegalStateException("Không tìm thấy DataLoader rubricCriteriaDataLoader.");
        }

        return loader.load(new RubricCriteriaKey(version.id(), validPage, validSize));
    }


    // Lấy Rubric ResultBand từ rubric version
    @SchemaMapping(typeName = "RubricVersion", field = "resultBands")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public CompletableFuture<PageResult<RubricResultBandDto>> getRubricResultBands(
            RubricVersionDto version,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size,
            DataFetchingEnvironment env) {

        int validPage = (page != null && page > 0) ? page - 1 : 0;
        int validSize = (size != null && size > 0) ? size : 10;

        DataLoader<RubricResultBandsKey, PageResult<RubricResultBandDto>> loader = env.getDataLoader("rubricResultBandsDataLoader");
        if (loader == null) {
            throw new IllegalStateException("Không tìm thấy DataLoader rubricResultBandsDataLoader.");
        }

        return loader.load(new RubricResultBandsKey(version.id(), validPage, validSize));
    }


    //Search Rubric version của hệ thống
    @QueryMapping(name = "searchSystemRubricVersions")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public PageResult<RubricVersionDto> searchSystemRubricVersions(
            @Argument(name = "rubricId") UUID rubricId,
            @Argument(name = "filter") SearchRubricVersionFilterRequest filter,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {

        // 1. Chuẩn hóa phân trang: Chuyển 1-based từ client về 0-based của Spring Data
        int validPage = (page != null && page > 0) ? page - 1 : 0;
        int validSize = (size != null && size > 0) ? size : 10;

        // 2. Phòng hộ lỗi dữ liệu null của Filter block
        var safeFilter = (filter != null) ? filter : new SearchRubricVersionFilterRequest(null, null);

        var query = new SearchSystemRubricVersionsQuery(
                rubricId,
                safeFilter.keyword(),
                safeFilter.status(),
                validPage,
                validSize
        );

        return searchSystemRubricVersionsUseCase.execute(query);
    }

    // Search Rubric Version của school
    @QueryMapping(name = "searchSchoolRubricVersions")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public PageResult<RubricVersionDto> searchSchoolRubricVersions(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "rubricId") UUID rubricId,
            @Argument(name = "filter") SearchRubricVersionFilterRequest filter,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {

        // Chuẩn hóa phân trang về 0-based
        int validPage = (page != null && page > 0) ? page - 1 : 0;
        int validSize = (size != null && size > 0) ? size : 10;

        // Tránh NullPointer
        var safeFilter = (filter != null) ? filter : new SearchRubricVersionFilterRequest(null, null);

        // Khởi tạo Query phẳng
        var query = new SearchSchoolRubricVersionsQuery(
                schoolId,
                rubricId,
                safeFilter.keyword(),
                safeFilter.status(),
                validPage,
                validSize
        );

        return searchSchoolRubricVersionsUseCase.execute(query);
    }

    //========================== RUBRIC CRITERION =======================

    //Update Rubric Criterion của hệ thống
    @MutationMapping(name = "updateSystemRubricCriterion")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public UUID updateSystemRubricCriterion(
            @Argument(name = "criterionId") UUID criterionId,
            @Argument(name = "input") UpdateRubricCriterionInput input
    ) {
        var command = RubricCriterionGraphQLMapper.fromSystemInput(criterionId, input);
        return updateSystemRubricCriterionUseCase.execute(command);
    }

    //Update Rubric Criterion của trường học
    @MutationMapping(name = "updateSchoolRubricCriterion")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public UUID updateSchoolRubricCriterion(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "criterionId") UUID criterionId,
            @Argument(name = "input") UpdateRubricCriterionInput input
    ) {
        var command = RubricCriterionGraphQLMapper.fromSchoolInput(schoolId, criterionId, input);
        return updateSchoolRubricCriterionUseCase.execute(command);
    }

    //View Rubric Criterion của hệ thống
    @QueryMapping(name = "viewSystemRubricCriterion")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public RubricCriterionDto viewSystemRubricCriterion(
            @Argument(name = "criterionId") UUID criterionId
    ) {
        var query = new ViewSystemRubricCriterionDetailsQuery(criterionId);
        return viewSystemRubricCriterionDetailsUseCase.execute(query);
    }

    // View Rubric Criterion của trường học
    @QueryMapping(name = "viewSchoolRubricCriterion")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public RubricCriterionDto viewSchoolRubricCriterion(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "criterionId") UUID criterionId
    ) {
        var query = new ViewSchoolRubricCriterionDetailsQuery(schoolId, criterionId);
        return viewSchoolRubricCriterionDetailsUseCase.execute(query);
    }


    //View Danh sách Rubric Criterion của hệ thống
    @QueryMapping(name = "viewSystemRubricCriteria")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public PageResult<RubricCriterionDto> viewSystemRubricCriteria(
            @Argument(name = "versionId") UUID versionId,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size
    ) {
        if (page != null && page <= 0) {
            throw new IllegalArgumentException("Tham số 'page' không hợp lệ. Trang phải bắt đầu từ 1.");
        }
        if (size != null && size <= 0) {
            throw new IllegalArgumentException("Tham số 'size' không hợp lệ. Số lượng phần tử phải lớn hơn 0.");
        }

        int validPage = page != null ? page - 1 : 0;
        int pageSize = (size != null) ? size : 10;

        var query = new ViewSystemRubricCriteriaQuery(versionId, validPage, pageSize);
        return viewSystemRubricCriteriaUseCase.execute(query);
    }

    //View Danh sách Rubric Criterion của trường học
    @QueryMapping(name = "viewSchoolRubricCriteria")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public PageResult<RubricCriterionDto> viewSchoolRubricCriteria(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "versionId") UUID versionId,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size
    ) {

        int validPage = (page != null && page > 0) ? page - 1 : 0;
        int pageSize = (size != null) ? size : 10;

        var query = new ViewSchoolRubricCriteriaQuery(schoolId, versionId, validPage, pageSize);
        return viewSchoolRubricCriteriaUseCase.execute(query);
    }


    // GraphQL: TÌM KIẾM TIÊU CHÍ (CRITERIA) CHO SYSTEM ADMIN
    @QueryMapping(name = "searchSystemRubricCriteria")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public PageResult<RubricCriterionDto> searchSystemRubricCriteria(
            @Argument(name = "versionId") UUID versionId,
            @Argument(name = "filter") SearchRubricCriterionFilterRequest filter,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {

        int validPage = (page != null && page > 0) ? page - 1 : 0;
        int validSize = (size != null && size > 0) ? size : 10;
        var safeFilter = (filter != null) ? filter : new SearchRubricCriterionFilterRequest(null, null);

        var query = new SearchSystemRubricCriteriaQuery(
                versionId, safeFilter.keyword(), safeFilter.isRequired(), validPage, validSize
        );

        return searchSystemRubricCriteriaUseCase.execute(query);
    }

    // GraphQL: TÌM KIẾM TIÊU CHÍ (CRITERIA) CHO SCHOOL ADMIN
    @QueryMapping(name = "searchSchoolRubricCriteria")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public PageResult<RubricCriterionDto> searchSchoolRubricCriteria(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "versionId") UUID versionId,
            @Argument(name = "filter") SearchRubricCriterionFilterRequest filter,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {

        int validPage = (page != null && page > 0) ? page - 1 : 0;
        int validSize = (size != null && size > 0) ? size : 10;
        var safeFilter = (filter != null) ? filter : new SearchRubricCriterionFilterRequest(null, null);

        var query = new SearchSchoolRubricCriteriaQuery(
                schoolId, versionId, safeFilter.keyword(), safeFilter.isRequired(), validPage, validSize
        );

        return searchSchoolRubricCriteriaUseCase.execute(query);
    }

    // ====================RUBRIC RESULT BAND =====================
    //Update Rubric Result Band của hệ thống
    @MutationMapping(name = "updateSystemRubricResultBand")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public UUID updateSystemRubricResultBand(
            @Argument(name = "resultBandId") UUID resultBandId,
            @Argument(name = "input") UpdateRubricResultBandInput input
    ) {
        var command = RubricResultBandGraphQLMapper.fromSystemInput(resultBandId, input);
        return updateSystemRubricResultBandUseCase.execute(command);
    }

    //Update school rubric result band của trường
    @MutationMapping(name = "updateSchoolRubricResultBand")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public UUID updateSchoolRubricResultBand(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "resultBandId") UUID resultBandId,
            @Argument(name = "input") UpdateRubricResultBandInput input
    ) {
        var command = RubricResultBandGraphQLMapper.fromSchoolInput(schoolId, resultBandId, input);
        return updateSchoolRubricResultBandUseCase.execute(command);
    }

    //View Rubric Result Band của hệ thống
    @QueryMapping(name = "viewSystemRubricResultBand")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public RubricResultBandDto viewSystemRubricResultBand(
            @Argument(name = "resultBandId") UUID resultBandId
    ) {
        var query = new ViewSystemRubricResultBandDetailsQuery(resultBandId);
        return viewSystemRubricResultBandDetailsUseCase.execute(query);
    }

    //View Rubric Result Band của trường
    @QueryMapping(name = "viewSchoolRubricResultBand")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public RubricResultBandDto viewSchoolRubricResultBand(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "resultBandId") UUID resultBandId
    ) {
        var query = new ViewSchoolRubricResultBandDetailsQuery(schoolId, resultBandId);
        return viewSchoolRubricResultBandDetailsUseCase.execute(query);
    }

    //View Rubric Result Bands của hệ thống
    @QueryMapping(name = "viewSystemRubricResultBands")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public PageResult<RubricResultBandDto> viewSystemRubricResultBands(
            @Argument(name = "versionId") UUID versionId,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size
    ) {

        int validPage = (page != null && page > 0) ? page - 1 : 0;
        int pageSize = (size != null && size > 0) ? size : 10;

        var query = new ViewSystemRubricResultBandsQuery(versionId, validPage, pageSize);
        return viewSystemRubricResultBandsUseCase.execute(query);
    }

    //View Rubric Result Bands của trường học
    @QueryMapping(name = "viewSchoolRubricResultBands")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public PageResult<RubricResultBandDto> viewSchoolRubricResultBands(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "versionId") UUID versionId,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size
    ) {

        int validPage = (page != null && page > 0) ? page - 1 : 0;
        int pageSize = (size != null && size > 0) ? size : 10;

        var query = new ViewSchoolRubricResultBandsQuery(schoolId, versionId, validPage, pageSize);
        return viewSchoolRubricResultBandsUseCase.execute(query);
    }

    // GraphQL: TÌM KIẾM THANG ĐIỂM KẾT QUẢ CHO SYSTEM ADMIN
    @QueryMapping(name = "searchSystemRubricResultBands")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public PageResult<RubricResultBandDto> searchSystemRubricResultBands(
            @Argument(name = "versionId") UUID versionId,
            @Argument(name = "filter") SearchRubricResultBandFilterRequest filter,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {

        int validPage = (page != null && page > 0) ? page - 1 : 0;
        int validSize = (size != null && size > 0) ? size : 10;
        var safeFilter = (filter != null) ? filter : new SearchRubricResultBandFilterRequest(null);

        var query = new SearchSystemRubricResultBandsQuery(versionId, safeFilter.keyword(), validPage, validSize);
        return searchSystemRubricResultBandsUseCase.execute(query);
    }

    // GraphQL: TÌM KIẾM THANG ĐIỂM KẾT QUẢ CHO SCHOOL ADMIN
    @QueryMapping(name = "searchSchoolRubricResultBands")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public PageResult<RubricResultBandDto> searchSchoolRubricResultBands(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "versionId") UUID versionId,
            @Argument(name = "filter") SearchRubricResultBandFilterRequest filter,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {

        int validPage = (page != null && page > 0) ? page - 1 : 0;
        int validSize = (size != null && size > 0) ? size : 10;
        var safeFilter = (filter != null) ? filter : new SearchRubricResultBandFilterRequest(null);

        var query = new SearchSchoolRubricResultBandsQuery(schoolId, versionId, safeFilter.keyword(), validPage, validSize);
        return searchSchoolRubricResultBandsUseCase.execute(query);
    }
}
