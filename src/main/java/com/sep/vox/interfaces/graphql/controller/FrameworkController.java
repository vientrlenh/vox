package com.sep.vox.interfaces.graphql.controller;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.dataloader.DataLoader;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import graphql.schema.DataFetchingEnvironment;

import com.sep.vox.application.port.input.query.ViewFrameworkCriteriaQuery;
import com.sep.vox.application.port.input.query.ViewFrameworkCriterionDetailsQuery;
import com.sep.vox.application.port.input.query.ViewFrameworkDetailsQuery;
import com.sep.vox.application.port.input.query.ViewFrameworkVersionDetailsQuery;
import com.sep.vox.application.port.input.query.ViewFrameworkVersionsQuery;
import com.sep.vox.application.port.input.query.ViewFrameworksQuery;
import com.sep.vox.application.port.input.usecase.framework.UpdateFrameworkCriterionBandUseCase;
import com.sep.vox.application.port.input.usecase.framework.UpdateFrameworkCriterionUseCase;
import com.sep.vox.application.port.input.usecase.framework.UpdateFrameworkResultBandUseCase;
import com.sep.vox.application.port.input.usecase.framework.UpdateFrameworkVersionUseCase;
import com.sep.vox.application.port.input.usecase.framework.ViewActiveFrameworksUseCase;
import com.sep.vox.application.port.input.usecase.framework.ViewFrameworkCriteriaUseCase;
import com.sep.vox.application.port.input.usecase.framework.ViewFrameworkCriterionDetailsUseCase;
import com.sep.vox.application.port.input.usecase.framework.ViewFrameworkDetailsUseCase;
import com.sep.vox.application.port.input.usecase.framework.ViewFrameworkVersionDetailsUseCase;
import com.sep.vox.application.port.input.usecase.framework.ViewFrameworkVersionsUseCase;
import com.sep.vox.application.port.input.usecase.framework.ViewFrameworksUseCase;
import com.sep.vox.application.port.input.usecase.framework.ViewPublishedFrameworkVersionDetailsUseCase;
import com.sep.vox.application.port.input.usecase.framework.ViewPublishedFrameworkVersionsUseCase;
import com.sep.vox.application.port.input.usecase.framework.ViewSchoolFrameworkCriteriaUseCase;
import com.sep.vox.application.port.input.usecase.framework.ViewSchoolFrameworkCriterionDetailsUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.FrameworkCriterionDto;
import com.sep.vox.domain.dto.FrameworkDto;
import com.sep.vox.domain.dto.FrameworkResultBandDto;
import com.sep.vox.domain.dto.FrameworkVersionDto;
import com.sep.vox.interfaces.graphql.dto.request.UpdateFrameworkCriterionBandInput;
import com.sep.vox.interfaces.graphql.dto.request.UpdateFrameworkCriterionInput;
import com.sep.vox.interfaces.graphql.dto.request.UpdateFrameworkResultBandInput;
import com.sep.vox.interfaces.graphql.dto.request.UpdateFrameworkVersionInput;
import com.sep.vox.interfaces.graphql.mapper.UpdateFrameworkCriterionBandCommandMapper;
import com.sep.vox.interfaces.graphql.mapper.UpdateFrameworkCriterionCommandMapper;
import com.sep.vox.interfaces.graphql.mapper.UpdateFrameworkResultBandCommandMapper;
import com.sep.vox.interfaces.graphql.mapper.UpdateFrameworkVersionCommandMapper;

@Controller("graphqlFrameworkController")
public class FrameworkController {

    private final ViewFrameworksUseCase viewFrameworksUseCase;
    private final ViewFrameworkDetailsUseCase viewFrameworkDetailsUseCase;
    private final ViewFrameworkVersionsUseCase viewFrameworkVersionsUseCase;
    private final ViewFrameworkVersionDetailsUseCase viewFrameworkVersionDetailsUseCase;
    private final UpdateFrameworkVersionUseCase updateFrameworkVersionUseCase;
    private final UpdateFrameworkCriterionUseCase updateFrameworkCriterionUseCase;
    private final UpdateFrameworkCriterionBandUseCase updateFrameworkCriterionBandUseCase;
    private final UpdateFrameworkResultBandUseCase updateFrameworkResultBandUseCase;
    private final ViewActiveFrameworksUseCase viewActiveFrameworksUseCase;
    private final ViewPublishedFrameworkVersionsUseCase viewPublishedFrameworkVersionsUseCase;
    private final ViewPublishedFrameworkVersionDetailsUseCase viewPublishedFrameworkVersionDetailsUseCase;
    private final ViewFrameworkCriteriaUseCase viewFrameworkCriteriaUseCase;
    private final ViewFrameworkCriterionDetailsUseCase viewFrameworkCriterionDetailsUseCase;
    private final ViewSchoolFrameworkCriteriaUseCase viewSchoolFrameworkCriteriaUseCase;
    private final ViewSchoolFrameworkCriterionDetailsUseCase viewSchoolFrameworkCriterionDetailsUseCase;

    public FrameworkController(
            ViewFrameworksUseCase viewFrameworksUseCase,
            ViewFrameworkDetailsUseCase viewFrameworkDetailsUseCase,
            ViewFrameworkVersionsUseCase viewFrameworkVersionsUseCase,
            ViewFrameworkVersionDetailsUseCase viewFrameworkVersionDetailsUseCase,
            UpdateFrameworkVersionUseCase updateFrameworkVersionUseCase,
            UpdateFrameworkCriterionUseCase updateFrameworkCriterionUseCase,
            UpdateFrameworkCriterionBandUseCase updateFrameworkCriterionBandUseCase,
            UpdateFrameworkResultBandUseCase updateFrameworkResultBandUseCase,
            ViewActiveFrameworksUseCase viewActiveFrameworksUseCase,
            ViewPublishedFrameworkVersionsUseCase viewPublishedFrameworkVersionsUseCase,
            ViewPublishedFrameworkVersionDetailsUseCase viewPublishedFrameworkVersionDetailsUseCase,
            ViewFrameworkCriteriaUseCase viewFrameworkCriteriaUseCase,
            ViewFrameworkCriterionDetailsUseCase viewFrameworkCriterionDetailsUseCase,
            ViewSchoolFrameworkCriteriaUseCase viewSchoolFrameworkCriteriaUseCase,
            ViewSchoolFrameworkCriterionDetailsUseCase viewSchoolFrameworkCriterionDetailsUseCase) {
        this.viewFrameworksUseCase = viewFrameworksUseCase;
        this.viewFrameworkDetailsUseCase = viewFrameworkDetailsUseCase;
        this.viewFrameworkVersionsUseCase = viewFrameworkVersionsUseCase;
        this.viewFrameworkVersionDetailsUseCase = viewFrameworkVersionDetailsUseCase;
        this.updateFrameworkVersionUseCase = updateFrameworkVersionUseCase;
        this.updateFrameworkCriterionUseCase = updateFrameworkCriterionUseCase;
        this.updateFrameworkCriterionBandUseCase = updateFrameworkCriterionBandUseCase;
        this.updateFrameworkResultBandUseCase = updateFrameworkResultBandUseCase;
        this.viewActiveFrameworksUseCase = viewActiveFrameworksUseCase;
        this.viewPublishedFrameworkVersionsUseCase = viewPublishedFrameworkVersionsUseCase;
        this.viewPublishedFrameworkVersionDetailsUseCase = viewPublishedFrameworkVersionDetailsUseCase;
        this.viewFrameworkCriteriaUseCase = viewFrameworkCriteriaUseCase;
        this.viewFrameworkCriterionDetailsUseCase = viewFrameworkCriterionDetailsUseCase;
        this.viewSchoolFrameworkCriteriaUseCase = viewSchoolFrameworkCriteriaUseCase;
        this.viewSchoolFrameworkCriterionDetailsUseCase = viewSchoolFrameworkCriterionDetailsUseCase;
    }

    @QueryMapping(name = "frameworks")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public PageResult<FrameworkDto> frameworks(
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size,
            @Argument(name = "search") String search,
            @Argument(name = "isActive") Boolean isActive) {
        validatePage(page, size);
        return viewFrameworksUseCase.execute(new ViewFrameworksQuery(page, size, search, isActive));
    }

    @QueryMapping(name = "framework")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public FrameworkDto framework(@Argument(name = "id") UUID id) {
        return viewFrameworkDetailsUseCase.execute(new ViewFrameworkDetailsQuery(id));
    }

    @QueryMapping(name = "frameworkVersions")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public PageResult<FrameworkVersionDto> frameworkVersions(
            @Argument(name = "frameworkId") UUID frameworkId,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {
        validatePage(page, size);
        return viewFrameworkVersionsUseCase.execute(new ViewFrameworkVersionsQuery(frameworkId, page, size));
    }

    @QueryMapping(name = "frameworkVersion")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public FrameworkVersionDto frameworkVersion(@Argument(name = "id") UUID id) {
        return viewFrameworkVersionDetailsUseCase.execute(new ViewFrameworkVersionDetailsQuery(id));
    }

    @QueryMapping(name = "schoolFrameworks")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public PageResult<FrameworkDto> schoolFrameworks(
            @Argument(name = "page") int page,
            @Argument(name = "size") int size,
            @Argument(name = "search") String search) {
        if (page <= 0 || size <= 0) {
            throw new IllegalArgumentException("Số trang hoặc kích thước trang không hợp lệ");
        }
        return viewActiveFrameworksUseCase.execute(new ViewFrameworksQuery(page, size, search, true));
    }

    @QueryMapping(name = "schoolFrameworkVersions")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public PageResult<FrameworkVersionDto> schoolFrameworkVersions(
            @Argument(name = "frameworkId") UUID frameworkId,
            @Argument(name = "page") int page,
            @Argument(name = "size") int size) {
        if (page <= 0 || size <= 0) {
            throw new IllegalArgumentException("Số trang hoặc kích thước trang không hợp lệ");
        }
        return viewPublishedFrameworkVersionsUseCase.execute(new ViewFrameworkVersionsQuery(frameworkId, page, size));
    }

    @QueryMapping(name = "schoolFrameworkVersion")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public FrameworkVersionDto schoolFrameworkVersion(@Argument(name = "id") UUID id) {
        return viewPublishedFrameworkVersionDetailsUseCase.execute(new ViewFrameworkVersionDetailsQuery(id));
    }

    // Query gốc lấy danh sách FrameworkCriterion cho System Admin (không giới hạn trạng thái Version)
    @QueryMapping(name = "frameworkCriteria")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public List<FrameworkCriterionDto> frameworkCriteria(@Argument(name = "frameworkVersionId") UUID frameworkVersionId) {
        return viewFrameworkCriteriaUseCase.execute(new ViewFrameworkCriteriaQuery(frameworkVersionId));
    }

    // Query gốc lấy chi tiết 1 FrameworkCriterion cho System Admin
    @QueryMapping(name = "frameworkCriterion")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public FrameworkCriterionDto frameworkCriterion(@Argument(name = "id") UUID id) {
        return viewFrameworkCriterionDetailsUseCase.execute(new ViewFrameworkCriterionDetailsQuery(id));
    }

    // Query gốc lấy danh sách FrameworkCriterion cho School Admin (chỉ Version đã PUBLISHED)
    @QueryMapping(name = "schoolFrameworkCriteria")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public List<FrameworkCriterionDto> schoolFrameworkCriteria(@Argument(name = "frameworkVersionId") UUID frameworkVersionId) {
        return viewSchoolFrameworkCriteriaUseCase.execute(new ViewFrameworkCriteriaQuery(frameworkVersionId));
    }

    // Query gốc lấy chi tiết 1 FrameworkCriterion cho School Admin (chỉ Version đã PUBLISHED)
    @QueryMapping(name = "schoolFrameworkCriterion")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public FrameworkCriterionDto schoolFrameworkCriterion(@Argument(name = "id") UUID id) {
        return viewSchoolFrameworkCriterionDetailsUseCase.execute(new ViewFrameworkCriterionDetailsQuery(id));
    }

    @MutationMapping(name = "updateFrameworkVersion")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public UUID updateFrameworkVersion(
            @Argument(name = "frameworkId") UUID frameworkId,
            @Argument(name = "versionId") UUID versionId,
            @Argument(name = "input") UpdateFrameworkVersionInput input) {
        var command = UpdateFrameworkVersionCommandMapper.fromInput(frameworkId, versionId, input);
        return updateFrameworkVersionUseCase.execute(command);
    }

    @MutationMapping(name = "updateFrameworkCriterion")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public UUID updateFrameworkCriterion(
            @Argument(name = "frameworkId") UUID frameworkId,
            @Argument(name = "versionId") UUID versionId,
            @Argument(name = "criterionId") UUID criterionId,
            @Argument(name = "input") UpdateFrameworkCriterionInput input) {
        var command = UpdateFrameworkCriterionCommandMapper.fromInput(frameworkId, versionId, criterionId, input);
        return updateFrameworkCriterionUseCase.execute(command);
    }

    @MutationMapping(name = "updateFrameworkCriterionBand")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public UUID updateFrameworkCriterionBand(
            @Argument(name = "frameworkId") UUID frameworkId,
            @Argument(name = "versionId") UUID versionId,
            @Argument(name = "criterionId") UUID criterionId,
            @Argument(name = "bandId") UUID bandId,
            @Argument(name = "input") UpdateFrameworkCriterionBandInput input) {
        var command = UpdateFrameworkCriterionBandCommandMapper.fromInput(
                frameworkId, versionId, criterionId, bandId, input);
        return updateFrameworkCriterionBandUseCase.execute(command);
    }

    @MutationMapping(name = "updateFrameworkResultBand")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public UUID updateFrameworkResultBand(
            @Argument(name = "frameworkId") UUID frameworkId,
            @Argument(name = "versionId") UUID versionId,
            @Argument(name = "bandId") UUID bandId,
            @Argument(name = "input") UpdateFrameworkResultBandInput input) {
        var command = UpdateFrameworkResultBandCommandMapper.fromInput(frameworkId, versionId, bandId, input);
        return updateFrameworkResultBandUseCase.execute(command);
    }

    @SchemaMapping(typeName = "FrameworkVersion", field = "criteria")
    public CompletableFuture<List<FrameworkCriterionDto>> criteria(
            FrameworkVersionDto version, DataFetchingEnvironment env) {
        DataLoader<UUID, List<FrameworkCriterionDto>> loader = env.getDataLoader("criteriaByFrameworkVersion");
        return loader.load(version.id());
    }

    @SchemaMapping(typeName = "FrameworkVersion", field = "resultBands")
    public CompletableFuture<List<FrameworkResultBandDto>> resultBands(
            FrameworkVersionDto version, DataFetchingEnvironment env) {
        DataLoader<UUID, List<FrameworkResultBandDto>> loader = env.getDataLoader("resultBandsByFrameworkVersion");
        return loader.load(version.id());
    }

    private void validatePage(Integer page, Integer size) {
        if (page == null || size == null || page <= 0 || size <= 0)
            throw new IllegalStateException("Số trang hoặc kích thước trang yêu cầu không hợp lệ");
        
    }
}
