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

import com.sep.vox.application.port.input.query.ViewFrameworkDetailsQuery;
import com.sep.vox.application.port.input.query.ViewFrameworkVersionDetailsQuery;
import com.sep.vox.application.port.input.query.ViewFrameworkVersionsQuery;
import com.sep.vox.application.port.input.query.ViewFrameworksQuery;
import com.sep.vox.application.port.input.usecase.framework.UpdateFrameworkCriterionBandUseCase;
import com.sep.vox.application.port.input.usecase.framework.UpdateFrameworkCriterionUseCase;
import com.sep.vox.application.port.input.usecase.framework.UpdateFrameworkResultBandUseCase;
import com.sep.vox.application.port.input.usecase.framework.UpdateFrameworkVersionUseCase;
import com.sep.vox.application.port.input.usecase.framework.ViewFrameworkDetailsUseCase;
import com.sep.vox.application.port.input.usecase.framework.ViewFrameworkVersionDetailsUseCase;
import com.sep.vox.application.port.input.usecase.framework.ViewFrameworkVersionsUseCase;
import com.sep.vox.application.port.input.usecase.framework.ViewFrameworksUseCase;
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

    public FrameworkController(
            ViewFrameworksUseCase viewFrameworksUseCase,
            ViewFrameworkDetailsUseCase viewFrameworkDetailsUseCase,
            ViewFrameworkVersionsUseCase viewFrameworkVersionsUseCase,
            ViewFrameworkVersionDetailsUseCase viewFrameworkVersionDetailsUseCase,
            UpdateFrameworkVersionUseCase updateFrameworkVersionUseCase,
            UpdateFrameworkCriterionUseCase updateFrameworkCriterionUseCase,
            UpdateFrameworkCriterionBandUseCase updateFrameworkCriterionBandUseCase,
            UpdateFrameworkResultBandUseCase updateFrameworkResultBandUseCase) {
        this.viewFrameworksUseCase = viewFrameworksUseCase;
        this.viewFrameworkDetailsUseCase = viewFrameworkDetailsUseCase;
        this.viewFrameworkVersionsUseCase = viewFrameworkVersionsUseCase;
        this.viewFrameworkVersionDetailsUseCase = viewFrameworkVersionDetailsUseCase;
        this.updateFrameworkVersionUseCase = updateFrameworkVersionUseCase;
        this.updateFrameworkCriterionUseCase = updateFrameworkCriterionUseCase;
        this.updateFrameworkCriterionBandUseCase = updateFrameworkCriterionBandUseCase;
        this.updateFrameworkResultBandUseCase = updateFrameworkResultBandUseCase;
    }

    @QueryMapping(name = "frameworks")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public PageResult<FrameworkDto> frameworks(
            @Argument(name = "page") int page,
            @Argument(name = "size") int size,
            @Argument(name = "search") String search,
            @Argument(name = "isActive") Boolean isActive) {
        if (page <= 0 || size <= 0 || size > 200) {
            throw new IllegalArgumentException("Số trang hoặc kích thước trang không hợp lệ");
        }
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
            @Argument(name = "page") int page,
            @Argument(name = "size") int size) {
        if (page <= 0 || size <= 0 || size > 200) {
            throw new IllegalArgumentException("Số trang hoặc kích thước trang không hợp lệ");
        }
        return viewFrameworkVersionsUseCase.execute(new ViewFrameworkVersionsQuery(frameworkId, page, size));
    }

    @QueryMapping(name = "frameworkVersion")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public FrameworkVersionDto frameworkVersion(@Argument(name = "id") UUID id) {
        return viewFrameworkVersionDetailsUseCase.execute(new ViewFrameworkVersionDetailsQuery(id));
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
}
