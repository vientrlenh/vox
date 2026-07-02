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
import com.sep.vox.application.port.input.command.UpdateFrameworkActiveStatusCommand;
import com.sep.vox.application.port.input.usecase.framework.UpdateFrameworkStatusUseCase;
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
import com.sep.vox.interfaces.graphql.mapper.UpdateFrameworkVersionCommandMapper;
import com.sep.vox.interfaces.graphql.mapper.UpdateFrameworkVersionInput;

@Controller("graphqlFrameworkController")
public class FrameworkController {

    private final ViewFrameworksUseCase viewFrameworksUseCase;
    private final ViewFrameworkDetailsUseCase viewFrameworkDetailsUseCase;
    private final ViewFrameworkVersionsUseCase viewFrameworkVersionsUseCase;
    private final ViewFrameworkVersionDetailsUseCase viewFrameworkVersionDetailsUseCase;
    private final UpdateFrameworkVersionUseCase updateFrameworkVersionUseCase;
    private final UpdateFrameworkStatusUseCase updateFrameworkActiveStatusUseCase;

    public FrameworkController(
            ViewFrameworksUseCase viewFrameworksUseCase,
            ViewFrameworkDetailsUseCase viewFrameworkDetailsUseCase,
            ViewFrameworkVersionsUseCase viewFrameworkVersionsUseCase,
            ViewFrameworkVersionDetailsUseCase viewFrameworkVersionDetailsUseCase,
            UpdateFrameworkVersionUseCase updateFrameworkVersionUseCase,
            UpdateFrameworkStatusUseCase updateFrameworkActiveStatusUseCase) {
        this.viewFrameworksUseCase = viewFrameworksUseCase;
        this.viewFrameworkDetailsUseCase = viewFrameworkDetailsUseCase;
        this.viewFrameworkVersionsUseCase = viewFrameworkVersionsUseCase;
        this.viewFrameworkVersionDetailsUseCase = viewFrameworkVersionDetailsUseCase;
        this.updateFrameworkVersionUseCase = updateFrameworkVersionUseCase;
        this.updateFrameworkActiveStatusUseCase = updateFrameworkActiveStatusUseCase;
    }

    @QueryMapping(name = "frameworks")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public PageResult<FrameworkDto> frameworks(
            @Argument(name = "page") int page,
            @Argument(name = "size") int size) {
        if (page <= 0 || size <= 0) {
            throw new IllegalArgumentException("Số trang hoặc kích thước trang không hợp lệ");
        }
        return viewFrameworksUseCase.execute(new ViewFrameworksQuery(page, size));
    }

    @QueryMapping(name = "framework")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public FrameworkDto framework(@Argument(name = "id") UUID id) {
        return viewFrameworkDetailsUseCase.execute(new ViewFrameworkDetailsQuery(id));
    }

    @QueryMapping(name = "frameworkVersions")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public PageResult<FrameworkVersionDto> frameworkVersions(
            @Argument(name = "frameworkId") UUID frameworkId,
            @Argument(name = "page") int page,
            @Argument(name = "size") int size) {
        if (page <= 0 || size <= 0) {
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

    @MutationMapping(name = "updateFrameworkStatus")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public UUID updateFrameworkStatus(
            @Argument(name = "id") UUID id,
            @Argument(name = "isActive") boolean isActive) {
        return updateFrameworkActiveStatusUseCase.execute(new UpdateFrameworkActiveStatusCommand(id, isActive));
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
