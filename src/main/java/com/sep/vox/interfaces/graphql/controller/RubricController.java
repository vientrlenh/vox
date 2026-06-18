package com.sep.vox.interfaces.graphql.controller;


import com.sep.vox.application.port.input.query.*;
import com.sep.vox.application.port.input.usecase.rubricschool.*;
import com.sep.vox.application.port.input.usecase.rubricsystem.*;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.*;
import com.sep.vox.interfaces.graphql.dto.request.*;
import com.sep.vox.interfaces.graphql.mapper.*;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.UUID;

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
    private final UpdateSystemRubricCriterionUseCase updateSystemRubricCriterionUseCase;
    private final UpdateSchoolRubricCriterionUseCase updateSchoolRubricCriterionUseCase;
    private final ViewSystemRubricCriterionDetailsUseCase viewSystemRubricCriterionDetailsUseCase;
    private final ViewSchoolRubricCriterionDetailsUseCase viewSchoolRubricCriterionDetailsUseCase;
    private final ViewSystemRubricCriteriaUseCase viewSystemRubricCriteriaUseCase;
    private final ViewSchoolRubricCriteriaUseCase viewSchoolRubricCriteriaUseCase;
    private final UpdateSchoolRubricCriterionBandUseCase updateSchoolRubricCriterionBandUseCase;
    private final UpdateSystemRubricCriterionBandUseCase updateSystemRubricCriterionBandUseCase;
    private final ViewSystemRubricCriterionBandDetailsUseCase viewSystemRubricCriterionBandDetailsUseCase;
    private final ViewSchoolRubricCriterionBandDetailsUseCase viewSchoolRubricCriterionBandDetailsUseCase;
    private final ViewSchoolRubricCriterionBandsUseCase viewSchoolRubricCriterionBandsUseCase;
    private final ViewSystemRubricCriterionBandsUseCase viewSystemRubricCriterionBandsUseCase;
    private final UpdateSystemRubricResultBandUseCase updateSystemRubricResultBandUseCase;
    private final UpdateSchoolRubricResultBandUseCase updateSchoolRubricResultBandUseCase;
    private final ViewSystemRubricResultBandDetailsUseCase viewSystemRubricResultBandDetailsUseCase;
    private final ViewSchoolRubricResultBandDetailsUseCase viewSchoolRubricResultBandDetailsUseCase;
    private final ViewSystemRubricResultBandsUseCase viewSystemRubricResultBandsUseCase;
    private final ViewSchoolRubricResultBandsUseCase viewSchoolRubricResultBandsUseCase;

    public RubricController(UpdateSystemRubricUseCase updateSystemRubricUseCase, UpdateSchoolRubricUseCase updateSchoolRubricUseCase, ViewSystemRubricDetailsUseCase viewSystemRubricDetailsUseCase, ViewSchoolRubricDetailsUseCase viewSchoolRubricDetailsUseCase, ViewSystemRubricsUseCase viewSystemRubricsUseCase, ViewSchoolRubricsUseCase viewSchoolRubricsUseCase, UpdateSystemRubricVersionUseCase updateSystemRubricVersionUseCase, UpdateSchoolRubricVersionUseCase updateSchoolRubricVersionUseCase, ViewSchoolRubricVersionDetailsUseCase viewSchoolRubricVersionDetailsUseCase, ViewSystemRubricVersionDetailsUseCase viewSystemRubricVersionDetailsUseCase, ViewSchoolRubricVersionsUseCase viewSchoolRubricVersionsUseCase, ViewSystemRubricVersionsUseCase viewSystemRubricVersionsUseCase, UpdateSystemRubricCriterionUseCase updateSystemRubricCriterionUseCase, UpdateSchoolRubricCriterionUseCase updateSchoolRubricCriterionUseCase, ViewSystemRubricCriterionDetailsUseCase viewSystemRubricCriterionDetailsUseCase, ViewSchoolRubricCriterionDetailsUseCase viewSchoolRubricCriterionDetailsUseCase, ViewSystemRubricCriteriaUseCase viewSystemRubricCriteriaUseCase, ViewSchoolRubricCriteriaUseCase viewSchoolRubricCriteriaUseCase, UpdateSchoolRubricCriterionBandUseCase updateSchoolRubricCriterionBandUseCase, UpdateSystemRubricCriterionBandUseCase updateSystemRubricCriterionBandUseCase, ViewSystemRubricCriterionBandDetailsUseCase viewSystemRubricCriterionBandDetailsUseCase, ViewSchoolRubricCriterionBandDetailsUseCase viewSchoolRubricCriterionBandDetailsUseCase, ViewSchoolRubricCriterionBandsUseCase viewSchoolRubricCriterionBandsUseCase, ViewSystemRubricCriterionBandsUseCase viewSystemRubricCriterionBandsUseCase, UpdateSystemRubricResultBandUseCase updateSystemRubricResultBandUseCase, UpdateSchoolRubricResultBandUseCase updateSchoolRubricResultBandUseCase, ViewSystemRubricResultBandDetailsUseCase viewSystemRubricResultBandDetailsUseCase, ViewSchoolRubricResultBandDetailsUseCase viewSchoolRubricResultBandDetailsUseCase, ViewSystemRubricResultBandsUseCase viewSystemRubricResultBandsUseCase, ViewSchoolRubricResultBandsUseCase viewSchoolRubricResultBandsUseCase) {
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
        this.updateSystemRubricCriterionUseCase = updateSystemRubricCriterionUseCase;
        this.updateSchoolRubricCriterionUseCase = updateSchoolRubricCriterionUseCase;
        this.viewSystemRubricCriterionDetailsUseCase = viewSystemRubricCriterionDetailsUseCase;
        this.viewSchoolRubricCriterionDetailsUseCase = viewSchoolRubricCriterionDetailsUseCase;
        this.viewSystemRubricCriteriaUseCase = viewSystemRubricCriteriaUseCase;
        this.viewSchoolRubricCriteriaUseCase = viewSchoolRubricCriteriaUseCase;
        this.updateSchoolRubricCriterionBandUseCase = updateSchoolRubricCriterionBandUseCase;
        this.updateSystemRubricCriterionBandUseCase = updateSystemRubricCriterionBandUseCase;
        this.viewSystemRubricCriterionBandDetailsUseCase = viewSystemRubricCriterionBandDetailsUseCase;
        this.viewSchoolRubricCriterionBandDetailsUseCase = viewSchoolRubricCriterionBandDetailsUseCase;
        this.viewSchoolRubricCriterionBandsUseCase = viewSchoolRubricCriterionBandsUseCase;
        this.viewSystemRubricCriterionBandsUseCase = viewSystemRubricCriterionBandsUseCase;
        this.updateSystemRubricResultBandUseCase = updateSystemRubricResultBandUseCase;
        this.updateSchoolRubricResultBandUseCase = updateSchoolRubricResultBandUseCase;
        this.viewSystemRubricResultBandDetailsUseCase = viewSystemRubricResultBandDetailsUseCase;
        this.viewSchoolRubricResultBandDetailsUseCase = viewSchoolRubricResultBandDetailsUseCase;
        this.viewSystemRubricResultBandsUseCase = viewSystemRubricResultBandsUseCase;
        this.viewSchoolRubricResultBandsUseCase = viewSchoolRubricResultBandsUseCase;
    }


    //============================== RUBRIC ========================================
    //Update Rubric của hệ thống
    @MutationMapping(name = "updateSystemRubric")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public UUID updateSystemRubric(
            @Argument UUID id,
            @Argument UpdateRubricInput input
    ) {
        var command = UpdateRubricGraphQLMapper.fromSystemInput(id, input);

        return updateSystemRubricUseCase.execute(command);
    }


    //Update Rubric của trường học
    @MutationMapping(name = "updateSchoolRubric")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public UUID updateSchoolRubric(
            @Argument UUID schoolId,
            @Argument UUID id,
            @Argument UpdateRubricInput input
    ) {
        // Map và Execute
        var command = UpdateRubricGraphQLMapper.fromSchoolInput(schoolId, id, input);
        return updateSchoolRubricUseCase.execute(command);
    }


    //View Rubric của hệ thống
    @QueryMapping(name = "viewSystemRubric")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public RubricDto viewSystemRubricById(@Argument("id") UUID id) {
        var query = new ViewSystemRubricDetailsQuery(id);
        return viewSystemRubricDetailsUseCase.execute(query);
    }


    //View Rubric của trường học
    @QueryMapping(name = "viewSchoolRubric")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public RubricDto viewSchoolRubricById(
            @Argument("schoolId") UUID schoolId,
            @Argument("id") UUID id
    ) {
        var query = new ViewSchoolRubricDetailsQuery(schoolId, id);
        return viewSchoolRubricDetailsUseCase.execute(query);
    }


    //View Rubrics của hệ thống
    @QueryMapping(name = "viewSystemRubrics")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public PageResult<RubricDto> viewSystemRubrics(
            @Argument("page") Integer page,
            @Argument("size") Integer size
    ) {
        // 1. Chặn ngay từ cửa nếu Frontend gửi data sai (page <= 0 hoặc size <= 0)
        if (page != null && page <= 0) {
            throw new IllegalArgumentException("Tham số 'page' không hợp lệ. Trang phải bắt đầu từ 1.");
        }
        if (size != null && size <= 0) {
            throw new IllegalArgumentException("Tham số 'size' không hợp lệ. Số lượng phần tử phải lớn hơn 0.");
        }

        // 2. Gán giá trị mặc định nếu Frontend không truyền (Quy ước mặc định là trang 1)
        int pageNumber = (page != null) ? page : 1;
        int pageSize = (size != null) ? size : 10;

        // 3. Đẩy xuống UseCase
        // LƯU Ý CỰC QUAN TRỌNG: Vì Spring Data JPA đếm page từ 0,
        // nên ta phải lấy (pageNumber - 1) trước khi gọi DB.

        var query = new ViewSystemRubricsQuery(pageNumber, pageSize);
        return viewSystemRubricsUseCase.execute(query);
    }


    //View Rubrics của trường học
    @QueryMapping(name = "viewSchoolRubrics")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public PageResult<RubricDto> viewSchoolRubrics(
            @Argument("schoolId") UUID schoolId,
            @Argument("page") Integer page,
            @Argument("size") Integer size
    ) {
        if (page != null && page <= 0) {
            throw new IllegalArgumentException("Tham số 'page' không hợp lệ. Trang phải bắt đầu từ 1.");
        }
        if (size != null && size <= 0) {
            throw new IllegalArgumentException("Tham số 'size' không hợp lệ. Số lượng phần tử phải lớn hơn 0.");
        }

        int pageNumber = (page != null) ? page : 1;
        int pageSize = (size != null) ? size : 10;

        var query = new ViewSchoolRubricsQuery(schoolId, pageNumber, pageSize);
        return viewSchoolRubricsUseCase.execute(query);
    }

    //========================== RUBRIC VERSION ===============================================
    // Update Rubric Version của hệ thống
    @MutationMapping(name = "updateSystemRubricVersion")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public UUID updateSystemRubricVersion(
            @Argument("versionId") UUID versionId,
            @Argument("input") UpdateRubricVersionInput input
    ) {
        // Sử dụng Mapper chuẩn men
        var command = UpdateRubricVersionGraphQLMapper.fromSystemInput(versionId, input);

        return updateSystemRubricVersionUseCase.execute(command);
    }


    // Update Rubric Version của school Admin
    @MutationMapping(name = "updateSchoolRubricVersion")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public UUID updateSchoolRubricVersion(
            @Argument("schoolId") UUID schoolId,
            @Argument("versionId") UUID versionId,
            @Argument("input") UpdateRubricVersionInput input
    ) {
        var command = UpdateRubricVersionGraphQLMapper.fromSchoolInput(schoolId, versionId, input);

        return updateSchoolRubricVersionUseCase.execute(command);
    }

    //View School Rubric Version Details của trường
    @QueryMapping(name = "viewSchoolRubricVersion")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public RubricVersionDto viewSchoolRubricVersion(
            @Argument("schoolId") UUID schoolId,
            @Argument("versionId") UUID versionId
    ) {
        var query = new ViewSchoolRubricVersionDetailsQuery(schoolId, versionId);
        return viewSchoolRubricVersionDetailsUseCase.execute(query);
    }

    //View School Rubric Version Details của hệ thống
    @QueryMapping(name = "viewSystemRubricVersion")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public RubricVersionDto viewSystemRubricVersion(
            @Argument("versionId") UUID versionId
    ) {
        var query = new ViewSystemRubricVersionDetailsQuery(versionId);
        return viewSystemRubricVersionDetailsUseCase.execute(query);
    }


    //View School Rubric Versions của trường
    @QueryMapping(name = "viewSchoolRubricVersions")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public PageResult<RubricVersionDto> viewSchoolRubricVersions(
            @Argument("schoolId") UUID schoolId,
            @Argument("rubricId") UUID rubricId,
            @Argument("status") String status,
            @Argument("page") Integer page,
            @Argument("size") Integer size
    ) {
        if (page != null && page <= 0) {
            throw new IllegalArgumentException("Tham số 'page' không hợp lệ. Trang phải bắt đầu từ 1.");
        }
        if (size != null && size <= 0) {
            throw new IllegalArgumentException("Tham số 'size' không hợp lệ. Số lượng phần tử phải lớn hơn 0.");
        }

        int pageNumber = (page != null) ? page : 1;
        int pageSize = (size != null) ? size : 10;

        var query = new ViewSchoolRubricVersionsQuery(schoolId, rubricId, status, pageNumber, pageSize);
        return viewSchoolRubricVersionsUseCase.execute(query);
    }


    //View System Rubric Versions của hệ thống
    @QueryMapping(name = "viewSystemRubricVersions")
    @PreAuthorize("isAuthenticated()")
    public PageResult<RubricVersionDto> viewSystemRubricVersions(
            @Argument("rubricId") UUID rubricId,
            @Argument("status") String status,
            @Argument("page") Integer page,
            @Argument("size") Integer size
    ) {
        if (page != null && page <= 0) {
            throw new IllegalArgumentException("Tham số 'page' không hợp lệ. Trang phải bắt đầu từ 1.");
        }
        if (size != null && size <= 0) {
            throw new IllegalArgumentException("Tham số 'size' không hợp lệ. Số lượng phần tử phải lớn hơn 0.");
        }

        int pageNumber = (page != null) ? page : 1;
        int pageSize = (size != null) ? size : 10;

        var query = new ViewSystemRubricVersionsQuery(rubricId, status, pageNumber, pageSize);
        return viewSystemRubricVersionsUseCase.execute(query);
    }

    //========================== RUBRIC CRITERION =======================

    //Update Rubric Criterion của hệ thống
    @MutationMapping(name = "updateSystemRubricCriterion")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public UUID updateSystemRubricCriterion(
            @Argument("criterionId") UUID criterionId,
            @Argument("input") UpdateRubricCriterionInput input
    ) {
        var command = RubricCriterionGraphQLMapper.fromSystemInput(criterionId, input);
        return updateSystemRubricCriterionUseCase.execute(command);
    }

    //Update Rubric Criterion của trường học
    @MutationMapping(name = "updateSchoolRubricCriterion")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public UUID updateSchoolRubricCriterion(
            @Argument("schoolId") UUID schoolId,
            @Argument("criterionId") UUID criterionId,
            @Argument("input") UpdateRubricCriterionInput input
    ) {
        var command = RubricCriterionGraphQLMapper.fromSchoolInput(schoolId, criterionId, input);
        return updateSchoolRubricCriterionUseCase.execute(command);
    }

    //View Rubric Criterion của hệ thống
    @QueryMapping(name = "viewSystemRubricCriterion")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public RubricCriterionDto viewSystemRubricCriterion(
            @Argument("criterionId") UUID criterionId
    ) {
        var query = new ViewSystemRubricCriterionDetailsQuery(criterionId);
        return viewSystemRubricCriterionDetailsUseCase.execute(query);
    }

    // View Rubric Criterion của trường học
    @QueryMapping(name = "viewSchoolRubricCriterion")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public RubricCriterionDto viewSchoolRubricCriterion(
            @Argument("schoolId") UUID schoolId,
            @Argument("criterionId") UUID criterionId
    ) {
        var query = new ViewSchoolRubricCriterionDetailsQuery(schoolId, criterionId);
        return viewSchoolRubricCriterionDetailsUseCase.execute(query);
    }


    //View Danh sách Rubric Criterion của hệ thống
    @QueryMapping(name = "viewSystemRubricCriteria")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public PageResult<RubricCriterionDto> viewSystemRubricCriteria(
            @Argument("versionId") UUID versionId,
            @Argument("page") Integer page,
            @Argument("size") Integer size
    ) {
        if (page != null && page <= 0) {
            throw new IllegalArgumentException("Tham số 'page' không hợp lệ. Trang phải bắt đầu từ 1.");
        }
        if (size != null && size <= 0) {
            throw new IllegalArgumentException("Tham số 'size' không hợp lệ. Số lượng phần tử phải lớn hơn 0.");
        }

        int pageNumber = (page != null) ? page : 1;
        int pageSize = (size != null) ? size : 10;

        var query = new ViewSystemRubricCriteriaQuery(versionId, pageNumber, pageSize);
        return viewSystemRubricCriteriaUseCase.execute(query);
    }

    //View Danh sách Rubric Criterion của trường học
    @QueryMapping(name = "viewSchoolRubricCriteria")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public PageResult<RubricCriterionDto> viewSchoolRubricCriteria(
            @Argument("schoolId") java.util.UUID schoolId,
            @Argument("versionId") java.util.UUID versionId,
            @Argument("page") Integer page,
            @Argument("size") Integer size
    ) {
        if (page != null && page <= 0) {
            throw new IllegalArgumentException("Tham số 'page' không hợp lệ. Trang phải bắt đầu từ 1.");
        }
        if (size != null && size <= 0) {
            throw new IllegalArgumentException("Tham số 'size' không hợp lệ. Số lượng phần tử phải lớn hơn 0.");
        }

        int pageNumber = (page != null) ? page : 1;
        int pageSize = (size != null) ? size : 10;

        var query = new ViewSchoolRubricCriteriaQuery(schoolId, versionId, pageNumber, pageSize);
        return viewSchoolRubricCriteriaUseCase.execute(query);
    }


    // ====================RUBRIC CRITERION BAND =====================
    //Update Rubric Criterion band cửa trường
    @MutationMapping(name = "updateSchoolRubricCriterionBand")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public UUID updateSchoolRubricCriterionBand(
            @Argument("schoolId") UUID schoolId,
            @Argument("bandId") UUID bandId,
            @Argument("input") UpdateRubricCriterionBandInput input
    ) {
        var command = RubricCriterionBandGraphQLMapper.fromSchoolInput(schoolId, bandId, input);
        return updateSchoolRubricCriterionBandUseCase.execute(command);
    }

    // Update Rubric Criterion band của hệ thống
    @MutationMapping(name = "updateSystemRubricCriterionBand")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public UUID updateSystemRubricCriterionBand(
            @Argument("bandId") UUID bandId,
            @Argument("input") UpdateRubricCriterionBandInput input
    ) {
        var command = RubricCriterionBandGraphQLMapper.fromSystemInput(bandId, input);
        return updateSystemRubricCriterionBandUseCase.execute(command);
    }

    //View Rubric Cretiron Band của hệ thống
    @QueryMapping(name = "viewSystemRubricCriterionBand")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public RubricCriterionBandDto viewSystemRubricCriterionBand(
            @Argument("bandId") UUID bandId
    ) {
        var query = new ViewSystemRubricCriterionBandDetailsQuery(bandId);
        return viewSystemRubricCriterionBandDetailsUseCase.execute(query);
    }

    //View RUBRIC CRITERION BAND của trường học
    @QueryMapping(name = "viewSchoolRubricCriterionBand")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public RubricCriterionBandDto viewSchoolRubricCriterionBand(
            @Argument("schoolId") UUID schoolId,
            @Argument("bandId") UUID bandId
    ) {
        var query = new ViewSchoolRubricCriterionBandDetailsQuery(schoolId, bandId);
        return viewSchoolRubricCriterionBandDetailsUseCase.execute(query);
    }

    //View  Rubric Criterion Bands của trường học
    @QueryMapping(name = "viewSchoolRubricCriterionBands")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public PageResult<RubricCriterionBandDto> viewSchoolRubricCriterionBands(
            @Argument("schoolId") java.util.UUID schoolId,
            @Argument("criterionId") java.util.UUID criterionId,
            @Argument("page") Integer page,
            @Argument("size") Integer size
    ) {
        if (page != null && page <= 0) {
            throw new IllegalArgumentException("Tham số 'page' không hợp lệ. Trang phải bắt đầu từ 1.");
        }
        if (size != null && size <= 0) {
            throw new IllegalArgumentException("Tham số 'size' không hợp lệ. Số lượng phần tử phải lớn hơn 0.");
        }

        int pageNumber = (page != null) ? page : 1;
        int pageSize = (size != null) ? size : 10;

        var query = new ViewSchoolRubricCriterionBandsQuery(schoolId, criterionId, pageNumber, pageSize);
        return viewSchoolRubricCriterionBandsUseCase.execute(query);
    }

    //View  Rubric Criterion Bands của hệ thống
    @QueryMapping(name = "viewSystemRubricCriterionBands")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public PageResult<RubricCriterionBandDto> viewSystemRubricCriterionBands(
            @Argument("criterionId") java.util.UUID criterionId,
            @Argument("page") Integer page,
            @Argument("size") Integer size
    ) {
        if (page != null && page <= 0) {
            throw new IllegalArgumentException("Tham số 'page' không hợp lệ. Trang phải bắt đầu từ 1.");
        }
        if (size != null && size <= 0) {
            throw new IllegalArgumentException("Tham số 'size' không hợp lệ. Số lượng phần tử phải lớn hơn 0.");
        }

        int pageNumber = (page != null) ? page : 1;
        int pageSize = (size != null) ? size : 10;

        var query = new ViewSystemRubricCriterionBandsQuery(criterionId, pageNumber, pageSize);
        return viewSystemRubricCriterionBandsUseCase.execute(query);
    }

    // ====================RUBRIC RESULT BAND =====================
    //Update Rubric Result Band của hệ thống
    @MutationMapping(name = "updateSystemRubricResultBand")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public UUID updateSystemRubricResultBand(
            @Argument("resultBandId") UUID resultBandId,
            @Argument("input") UpdateRubricResultBandInput input
    ) {
        var command = RubricResultBandGraphQLMapper.fromSystemInput(resultBandId, input);
        return updateSystemRubricResultBandUseCase.execute(command);
    }

    //Update school rubric result band của trường
    @MutationMapping(name = "updateSchoolRubricResultBand")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public UUID updateSchoolRubricResultBand(
            @Argument("schoolId") UUID schoolId,
            @Argument("resultBandId") UUID resultBandId,
            @Argument("input") UpdateRubricResultBandInput input
    ) {
        var command = RubricResultBandGraphQLMapper.fromSchoolInput(schoolId, resultBandId, input);
        return updateSchoolRubricResultBandUseCase.execute(command);
    }

    //View Rubric Result Band của hệ thống
    @QueryMapping(name = "viewSystemRubricResultBand")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public RubricResultBandDto viewSystemRubricResultBand(
            @Argument("resultBandId") UUID resultBandId
    ) {
        var query = new ViewSystemRubricResultBandDetailsQuery(resultBandId);
        return viewSystemRubricResultBandDetailsUseCase.execute(query);
    }

    //View Rubric Result Band của trường
    @QueryMapping(name = "viewSchoolRubricResultBand")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public RubricResultBandDto viewSchoolRubricResultBand(
            @Argument("schoolId") UUID schoolId,
            @Argument("resultBandId") UUID resultBandId
    ) {
        var query = new ViewSchoolRubricResultBandDetailsQuery(schoolId, resultBandId);
        return viewSchoolRubricResultBandDetailsUseCase.execute(query);
    }

    //View Rubric Result Bands của hệ thống
    @QueryMapping(name = "viewSystemRubricResultBands")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public PageResult<RubricResultBandDto> viewSystemRubricResultBands(
            @Argument("versionId") UUID versionId,
            @Argument("page") Integer page,
            @Argument("size") Integer size
    ) {
        if (page != null && page <= 0) {
            throw new IllegalArgumentException("Tham số 'page' không hợp lệ. Trang phải bắt đầu từ 1.");
        }
        if (size != null && size <= 0) {
            throw new IllegalArgumentException("Tham số 'size' không hợp lệ. Số lượng phần tử phải lớn hơn 0.");
        }

        int pageNumber = (page != null) ? page : 1;
        int pageSize = (size != null) ? size : 10;

        var query = new ViewSystemRubricResultBandsQuery(versionId, pageNumber, pageSize);
        return viewSystemRubricResultBandsUseCase.execute(query);
    }

    //View Rubric Result Bands của trường học
    @QueryMapping(name = "viewSchoolRubricResultBands")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public PageResult<RubricResultBandDto> viewSchoolRubricResultBands(
            @Argument("schoolId") UUID schoolId,
            @Argument("versionId") UUID versionId,
            @Argument("page") Integer page,
            @Argument("size") Integer size
    ) {
        if (page != null && page <= 0) {
            throw new IllegalArgumentException("Tham số 'page' không hợp lệ. Trang phải bắt đầu từ 1.");
        }
        if (size != null && size <= 0) {
            throw new IllegalArgumentException("Tham số 'size' không hợp lệ. Số lượng phần tử phải lớn hơn 0.");
        }

        int pageNumber = (page != null) ? page : 1;
        int pageSize = (size != null) ? size : 10;

        var query = new ViewSchoolRubricResultBandsQuery(schoolId, versionId, pageNumber, pageSize);
        return viewSchoolRubricResultBandsUseCase.execute(query);
    }
}
