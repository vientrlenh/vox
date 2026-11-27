package com.sep.vox.interfaces.graphql.controller;


import com.sep.vox.interfaces.graphql.dataloader.GraphQLDataLoaders;
import com.sep.vox.interfaces.graphql.dataloader.FrameworkGraphQLDataLoaderConfig;
import com.sep.vox.interfaces.graphql.dataloader.RubricGraphQLDataLoaderConfig;
import com.sep.vox.interfaces.graphql.dataloader.SupportedLanguageGraphQLDataLoaderConfig;
import com.sep.vox.application.port.input.command.rubricschool.UpdateSchoolRubricCommand;
import com.sep.vox.application.port.input.command.rubricschool.UpdateSchoolRubricCriterionCommand;
import com.sep.vox.application.port.input.command.rubricschool.UpdateSchoolRubricResultBandCommand;
import com.sep.vox.application.port.input.command.rubricschool.UpdateSchoolRubricVersionCommand;
import com.sep.vox.application.port.input.command.rubricsystem.UpdateSystemRubricCommand;
import com.sep.vox.application.port.input.command.rubricsystem.UpdateSystemRubricCriterionCommand;
import com.sep.vox.application.port.input.command.rubricsystem.UpdateSystemRubricResultBandCommand;
import com.sep.vox.application.port.input.command.rubricsystem.UpdateSystemRubricVersionCommand;
import com.sep.vox.application.port.input.query.rubricschool.SearchSchoolRubricCriteriaQuery;
import com.sep.vox.application.port.input.query.rubricschool.SearchSchoolRubricResultBandsQuery;
import com.sep.vox.application.port.input.query.rubricschool.SearchSchoolRubricVersionsQuery;
import com.sep.vox.application.port.input.query.rubricschool.SearchSchoolRubricsQuery;
import com.sep.vox.application.port.input.query.rubricsystem.SearchSystemRubricCriteriaQuery;
import com.sep.vox.application.port.input.query.rubricsystem.SearchSystemRubricResultBandsQuery;
import com.sep.vox.application.port.input.query.rubricsystem.SearchSystemRubricVersionsQuery;
import com.sep.vox.application.port.input.query.rubricsystem.SearchSystemRubricsQuery;
import com.sep.vox.application.port.input.query.rubricteacher.SearchTeacherRubricsQuery;
import com.sep.vox.application.port.input.query.rubricschool.ViewSchoolRubricCriteriaQuery;
import com.sep.vox.application.port.input.query.rubricschool.ViewSchoolRubricCriterionDetailsQuery;
import com.sep.vox.application.port.input.query.rubricschool.ViewSchoolRubricDetailsQuery;
import com.sep.vox.application.port.input.query.rubricschool.ViewSchoolRubricResultBandDetailsQuery;
import com.sep.vox.application.port.input.query.rubricschool.ViewSchoolRubricResultBandsQuery;
import com.sep.vox.application.port.input.query.rubricschool.ViewSchoolRubricVersionDetailsQuery;
import com.sep.vox.application.port.input.query.rubricschool.ViewSchoolRubricVersionsQuery;
import com.sep.vox.application.port.input.query.rubricschool.ViewSchoolRubricsQuery;
import com.sep.vox.application.port.input.query.rubricsystem.ViewSystemRubricCriteriaQuery;
import com.sep.vox.application.port.input.query.rubricsystem.ViewSystemRubricCriterionDetailsQuery;
import com.sep.vox.application.port.input.query.rubricsystem.ViewSystemRubricDetailsQuery;
import com.sep.vox.application.port.input.query.rubricsystem.ViewSystemRubricResultBandDetailsQuery;
import com.sep.vox.application.port.input.query.rubricsystem.ViewSystemRubricResultBandsQuery;
import com.sep.vox.application.port.input.query.rubricsystem.ViewSystemRubricVersionDetailsQuery;
import com.sep.vox.application.port.input.query.rubricsystem.ViewSystemRubricVersionsQuery;
import com.sep.vox.application.port.input.query.rubricsystem.ViewSystemRubricsQuery;
import com.sep.vox.application.port.input.query.rubricteacher.ViewTeacherRubricVersionsQuery;
import com.sep.vox.interfaces.graphql.key.RubricCriteriaKey;
import com.sep.vox.interfaces.graphql.key.RubricResultBandsKey;
import com.sep.vox.interfaces.graphql.key.RubricVersionsKey;
import com.sep.vox.application.port.input.usecase.rubricschool.*;
import com.sep.vox.application.port.input.usecase.rubricsystem.*;
import com.sep.vox.application.port.input.usecase.rubricteacher.*;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.*;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.interfaces.graphql.input.rubricsystem.SearchRubricCriterionFilterRequest;
import com.sep.vox.interfaces.graphql.input.rubricsystem.SearchRubricFilterRequest;
import com.sep.vox.interfaces.graphql.input.rubricsystem.SearchRubricResultBandFilterRequest;
import com.sep.vox.interfaces.graphql.input.rubricsystem.SearchRubricVersionFilterRequest;
import com.sep.vox.interfaces.graphql.input.rubricsystem.UpdateRubricCriterionInput;
import com.sep.vox.interfaces.graphql.input.rubricsystem.UpdateRubricInput;
import com.sep.vox.interfaces.graphql.input.rubricsystem.UpdateRubricResultBandInput;
import com.sep.vox.interfaces.graphql.input.rubricsystem.UpdateRubricVersionInput;
import com.sep.vox.interfaces.shared.PageArguments;
import graphql.schema.DataFetchingEnvironment;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dataloader.DataLoader;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Controller
@RequiredArgsConstructor
public class GraphQLRubricController {

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


    //============================== RUBRIC ========================================
    // Update the system Rubric
    @MutationMapping(name = "updateSystemRubric")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public UUID updateSystemRubric(
            @Argument(name = "id") UUID id,
            @Argument(name = "input") UpdateRubricInput input
    ) {
        UpdateSystemRubricCommand command = UpdateRubricInput.toUpdateSystemRubricCommand(id, input);

        return updateSystemRubricUseCase.execute(command);
    }


    // Update the school Rubric
    @MutationMapping(name = "updateSchoolRubric")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public UUID updateSchoolRubric(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "id") UUID id,
            @Valid @Argument(name = "input") UpdateRubricInput input
    ) {
        UpdateSchoolRubricCommand command = UpdateRubricInput.toUpdateSchoolRubricCommand(schoolId, id, input);
        return updateSchoolRubricUseCase.execute(command);
    }


    // View the system Rubric
    @QueryMapping(name = "viewSystemRubric")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public RubricDto viewSystemRubricById(@Argument(name = "id") UUID id) {
        ViewSystemRubricDetailsQuery query = new ViewSystemRubricDetailsQuery(id);
        return viewSystemRubricDetailsUseCase.execute(query);
    }


    // View the school Rubric
    @QueryMapping(name = "viewSchoolRubric")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public RubricDto viewSchoolRubricById(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "id") UUID id
    ) {
        ViewSchoolRubricDetailsQuery query = new ViewSchoolRubricDetailsQuery(schoolId, id);
        return viewSchoolRubricDetailsUseCase.execute(query);
    }


    // View the system Rubrics
    @QueryMapping(name = "viewSystemRubrics")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public PageResult<RubricDto> viewSystemRubrics(
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size
    ) {

        PageArguments.validate(page, size);
        ViewSystemRubricsQuery query = new ViewSystemRubricsQuery(page, size);

        return viewSystemRubricsUseCase.execute(query);
    }


    // View the school Rubrics
    @QueryMapping(name = "viewSchoolRubrics")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public PageResult<RubricDto> viewSchoolRubrics(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size
    ) {
        PageArguments.validate(page, size);

        ViewSchoolRubricsQuery query = new ViewSchoolRubricsQuery(schoolId, page, size);
        return viewSchoolRubricsUseCase.execute(query);
    }

    @SchemaMapping(typeName = "Rubric", field = "language")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public CompletableFuture<SupportedLanguageDto> getLanguage(
            RubricDto rubric,
            DataFetchingEnvironment env) {

        DataLoader<UUID, SupportedLanguageDto> loader = GraphQLDataLoaders.require(env, SupportedLanguageGraphQLDataLoaderConfig.SUPPORTED_LANGUAGE_BY_SUPPORTED_LANGUAGE_ID);
        return loader.load(rubric.languageId());
    }

    @SchemaMapping(typeName = "Rubric", field = "framework")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public CompletableFuture<FrameworkDto> getFramework(
            RubricDto rubric,
            DataFetchingEnvironment env) {

        DataLoader<UUID, FrameworkDto> loader = GraphQLDataLoaders.require(env, FrameworkGraphQLDataLoaderConfig.FRAMEWORK_BY_FRAMEWORK_ID);
        return loader.load(rubric.frameworkId());
    }


    // Get all Rubric Versions of the school
    // Cannot be split because one type in the schema is shared
    // Handle roles
    @SchemaMapping(typeName = "Rubric", field = "versions")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public CompletableFuture<PageResult<RubricVersionDto>> getRubricVersions(
            RubricDto rubric,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size,
            @Argument(name = "status") RubricStatus status,
            DataFetchingEnvironment env) {

        PageArguments.validate(page, size);

        DataLoader<RubricVersionsKey, PageResult<RubricVersionDto>> loader = GraphQLDataLoaders.require(env, RubricGraphQLDataLoaderConfig.RUBRIC_VERSIONS_BY_RUBRIC_ID);
        return loader.load(new RubricVersionsKey(rubric.id(), status, page, size));
    }


    // Search system Rubrics by code, name
    @QueryMapping(name = "searchSystemRubrics")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public PageResult<RubricDto> searchSystemRubrics(
            @Argument(name = "filter") SearchRubricFilterRequest filter,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {

        PageArguments.validate(page, size);

        SearchRubricFilterRequest safeFilter = (filter != null) ? filter : new SearchRubricFilterRequest(null, null, null);

        SearchSystemRubricsQuery query = new SearchSystemRubricsQuery(
                safeFilter.keyword(),
                safeFilter.frameworkId(),
                safeFilter.languageId(),
                page,
                size
        );

        return searchSystemRubricsUseCase.execute(query);
    }


    /**
     * Catalog of the system's TEMPLATE criteria sets, for schools to browse before copying them.
     *
     * <p>Returns the same {@code Rubric} type as every other entry point, so going deeper into versions /
     * criteria / result bands reuses the existing resolvers as they are -- which already batch queries through DataLoader and
     * are already open to SCHOOL_ADMIN. A school filters for published versions with the existing argument:
     * {@code versions(status: PUBLISHED)}.
     *
     * <p>Kept separate from {@link #searchSystemRubrics} instead of widening its permissions: the two entry points serve two roles,
     * and if merged, the next person who tightens or loosens one side would accidentally change the other.
     *
     * <p>System drafts are still visible if the school passes no status filter. That is
     * acceptable, because the real gate is on the action, not on visibility:
     * {@code CloneSystemRubricToSchoolUseCase} refuses to copy any version that is not PUBLISHED.
     */
    @QueryMapping(name = "systemRubricTemplates")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public PageResult<RubricDto> systemRubricTemplates(
            @Argument(name = "filter") SearchRubricFilterRequest filter,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {

        PageArguments.validate(page, size);
        SearchRubricFilterRequest safeFilter = (filter != null) ? filter : new SearchRubricFilterRequest(null, null, null);

        return searchSystemRubricsUseCase.execute(new SearchSystemRubricsQuery(
                safeFilter.keyword(),
                safeFilter.frameworkId(),
                safeFilter.languageId(),
                page,
                size
        ));
    }


    // Search school Rubrics by code, name
    @QueryMapping(name = "searchSchoolRubrics")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public PageResult<RubricDto> searchSchoolRubrics(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "filter") SearchRubricFilterRequest filter,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {

        PageArguments.validate(page, size);

        SearchRubricFilterRequest safeFilter = (filter != null) ? filter : new SearchRubricFilterRequest(null, null, null);

        SearchSchoolRubricsQuery query = new SearchSchoolRubricsQuery(
                schoolId,
                safeFilter.keyword(),
                safeFilter.frameworkId(),
                safeFilter.languageId(),
                page,
                size
        );
        return searchSchoolRubricsUseCase.execute(query);
    }

    // Search Rubrics by code, name for a Teacher (only rubrics belonging to the Teacher's school)
    @QueryMapping(name = "searchTeacherRubrics")
    @PreAuthorize("hasRole('TEACHER')")
    public PageResult<RubricDto> searchTeacherRubrics(
            @Argument(name = "filter") SearchRubricFilterRequest filter,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {

        PageArguments.validate(page, size);

        SearchRubricFilterRequest safeFilter = (filter != null) ? filter : new SearchRubricFilterRequest(null, null, null);

        // schoolId is derived by the use case from the signed-in teacher.
        SearchTeacherRubricsQuery query = new SearchTeacherRubricsQuery(
                safeFilter.keyword(),
                safeFilter.frameworkId(),
                safeFilter.languageId(),
                page,
                size
        );
        return searchTeacherRubricsUseCase.execute(query);
    }

    //========================== RUBRIC VERSION ===============================================
    // Update the system Rubric Version
    @MutationMapping(name = "updateSystemRubricVersion")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public UUID updateSystemRubricVersion(
            @Argument(name = "versionId") UUID versionId,
            @Argument(name = "input") UpdateRubricVersionInput input
    ) {
        UpdateSystemRubricVersionCommand command = UpdateRubricVersionInput.toUpdateSystemRubricVersionCommand(versionId, input);

        return updateSystemRubricVersionUseCase.execute(command);
    }


    // Update the Rubric Version of the school Admin
    @MutationMapping(name = "updateSchoolRubricVersion")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public UUID updateSchoolRubricVersion(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "versionId") UUID versionId,
            @Argument(name = "input") UpdateRubricVersionInput input
    ) {
        UpdateSchoolRubricVersionCommand command = UpdateRubricVersionInput.toUpdateSchoolRubricVersionCommand(schoolId, versionId, input);

        return updateSchoolRubricVersionUseCase.execute(command);
    }

    // View the school Rubric Version details
    @QueryMapping(name = "viewSchoolRubricVersion")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public RubricVersionDto viewSchoolRubricVersion(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "versionId") UUID versionId
    ) {
        ViewSchoolRubricVersionDetailsQuery query = new ViewSchoolRubricVersionDetailsQuery(schoolId, versionId);
        return viewSchoolRubricVersionDetailsUseCase.execute(query);
    }

    // View the system Rubric Version details
    @QueryMapping(name = "viewSystemRubricVersion")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public RubricVersionDto viewSystemRubricVersion(
            @Argument(name = "versionId") UUID versionId
    ) {
        ViewSystemRubricVersionDetailsQuery query = new ViewSystemRubricVersionDetailsQuery(versionId);
        return viewSystemRubricVersionDetailsUseCase.execute(query);
    }


    // View the school Rubric Versions
    @QueryMapping(name = "viewSchoolRubricVersions")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public PageResult<RubricVersionDto> viewSchoolRubricVersions(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "rubricId") UUID rubricId,
            @Argument(name = "status") RubricStatus status,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size
    ) {
        PageArguments.validate(page, size);

        ViewSchoolRubricVersionsQuery query = new ViewSchoolRubricVersionsQuery(schoolId, rubricId, status, page, size);
        return viewSchoolRubricVersionsUseCase.execute(query);
    }


    // View the system Rubric Versions
    @QueryMapping(name = "viewSystemRubricVersions")
    @PreAuthorize("isAuthenticated()")
    public PageResult<RubricVersionDto> viewSystemRubricVersions(
            @Argument(name = "rubricId") UUID rubricId,
            @Argument(name = "status") RubricStatus status,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size
    ) {
        PageArguments.validate(page, size);

        ViewSystemRubricVersionsQuery query = new ViewSystemRubricVersionsQuery(rubricId, status, page, size);
        return viewSystemRubricVersionsUseCase.execute(query);
    }

    // View Rubric Versions for a Teacher (only PUBLISHED, only rubrics belonging to the Teacher's school)
    @QueryMapping(name = "viewTeacherRubricVersions")
    @PreAuthorize("hasRole('TEACHER')")
    public PageResult<RubricVersionDto> viewTeacherRubricVersions(
            @Argument(name = "rubricId") UUID rubricId,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size
    ) {
        PageArguments.validate(page, size);

        ViewTeacherRubricVersionsQuery query = new ViewTeacherRubricVersionsQuery(rubricId, page, size);
        return viewTeacherRubricVersionsUseCase.execute(query);
    }

    // Get the criteria (rubric criteria) of the rubric version
    @SchemaMapping(typeName = "RubricVersion", field = "criteria")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public CompletableFuture<PageResult<RubricCriterionDto>> getRubricCriteria(
            RubricVersionDto version,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size,
            DataFetchingEnvironment env) {

        PageArguments.validate(page, size);

        DataLoader<RubricCriteriaKey, PageResult<RubricCriterionDto>> loader = GraphQLDataLoaders.require(env, RubricGraphQLDataLoaderConfig.RUBRIC_CRITERIA_BY_RUBRIC_VERSION_ID);

        return loader.load(new RubricCriteriaKey(version.id(), page, size));
    }


    // Get the Rubric ResultBands from the rubric version
    @SchemaMapping(typeName = "RubricVersion", field = "resultBands")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public CompletableFuture<PageResult<RubricResultBandDto>> getRubricResultBands(
            RubricVersionDto version,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size,
            DataFetchingEnvironment env) {

        PageArguments.validate(page, size);

        DataLoader<RubricResultBandsKey, PageResult<RubricResultBandDto>> loader = GraphQLDataLoaders.require(env, RubricGraphQLDataLoaderConfig.RUBRIC_RESULT_BANDS_BY_RUBRIC_VERSION_ID);

        return loader.load(new RubricResultBandsKey(version.id(), page, size));
    }


    // Search system Rubric versions
    @QueryMapping(name = "searchSystemRubricVersions")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public PageResult<RubricVersionDto> searchSystemRubricVersions(
            @Argument(name = "rubricId") UUID rubricId,
            @Argument(name = "filter") SearchRubricVersionFilterRequest filter,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {

        PageArguments.validate(page, size);

        SearchRubricVersionFilterRequest safeFilter = (filter != null) ? filter : new SearchRubricVersionFilterRequest(null, null);

        SearchSystemRubricVersionsQuery query = new SearchSystemRubricVersionsQuery(
                rubricId,
                safeFilter.keyword(),
                safeFilter.status(),
                page,
                size
        );

        return searchSystemRubricVersionsUseCase.execute(query);
    }

    // Search school Rubric Versions
    @QueryMapping(name = "searchSchoolRubricVersions")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public PageResult<RubricVersionDto> searchSchoolRubricVersions(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "rubricId") UUID rubricId,
            @Argument(name = "filter") SearchRubricVersionFilterRequest filter,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {

        PageArguments.validate(page, size);

        SearchRubricVersionFilterRequest safeFilter = (filter != null) ? filter : new SearchRubricVersionFilterRequest(null, null);

        SearchSchoolRubricVersionsQuery query = new SearchSchoolRubricVersionsQuery(
                schoolId,
                rubricId,
                safeFilter.keyword(),
                safeFilter.status(),
                page,
                size
        );

        return searchSchoolRubricVersionsUseCase.execute(query);
    }

    //========================== RUBRIC CRITERION =======================

    // Update the system Rubric Criterion
    @MutationMapping(name = "updateSystemRubricCriterion")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public UUID updateSystemRubricCriterion(
            @Argument(name = "criterionId") UUID criterionId,
            @Argument(name = "input") UpdateRubricCriterionInput input
    ) {
        UpdateSystemRubricCriterionCommand command = UpdateRubricCriterionInput.toUpdateSystemRubricCriterionCommand(criterionId, input);
        return updateSystemRubricCriterionUseCase.execute(command);
    }

    // Update the school Rubric Criterion
    @MutationMapping(name = "updateSchoolRubricCriterion")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public UUID updateSchoolRubricCriterion(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "criterionId") UUID criterionId,
            @Argument(name = "input") UpdateRubricCriterionInput input
    ) {
        UpdateSchoolRubricCriterionCommand command = UpdateRubricCriterionInput.toUpdateSchoolRubricCriterionCommand(schoolId, criterionId, input);
        return updateSchoolRubricCriterionUseCase.execute(command);
    }

    // View the system Rubric Criterion
    @QueryMapping(name = "viewSystemRubricCriterion")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public RubricCriterionDto viewSystemRubricCriterion(
            @Argument(name = "criterionId") UUID criterionId
    ) {
        ViewSystemRubricCriterionDetailsQuery query = new ViewSystemRubricCriterionDetailsQuery(criterionId);
        return viewSystemRubricCriterionDetailsUseCase.execute(query);
    }

    // View the school Rubric Criterion
    @QueryMapping(name = "viewSchoolRubricCriterion")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public RubricCriterionDto viewSchoolRubricCriterion(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "criterionId") UUID criterionId
    ) {
        ViewSchoolRubricCriterionDetailsQuery query = new ViewSchoolRubricCriterionDetailsQuery(schoolId, criterionId);
        return viewSchoolRubricCriterionDetailsUseCase.execute(query);
    }


    // View the system Rubric Criterion list
    @QueryMapping(name = "viewSystemRubricCriteria")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public PageResult<RubricCriterionDto> viewSystemRubricCriteria(
            @Argument(name = "versionId") UUID versionId,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size
    ) {
        PageArguments.validate(page, size);

        ViewSystemRubricCriteriaQuery query = new ViewSystemRubricCriteriaQuery(versionId, page, size);
        return viewSystemRubricCriteriaUseCase.execute(query);
    }

    // View the school Rubric Criterion list
    @QueryMapping(name = "viewSchoolRubricCriteria")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public PageResult<RubricCriterionDto> viewSchoolRubricCriteria(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "versionId") UUID versionId,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size
    ) {

        PageArguments.validate(page, size);

        ViewSchoolRubricCriteriaQuery query = new ViewSchoolRubricCriteriaQuery(schoolId, versionId, page, size);
        return viewSchoolRubricCriteriaUseCase.execute(query);
    }


    @QueryMapping(name = "searchSystemRubricCriteria")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public PageResult<RubricCriterionDto> searchSystemRubricCriteria(
            @Argument(name = "versionId") UUID versionId,
            @Argument(name = "filter") SearchRubricCriterionFilterRequest filter,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {

        PageArguments.validate(page, size);
        SearchRubricCriterionFilterRequest safeFilter = (filter != null) ? filter : new SearchRubricCriterionFilterRequest(null, null);

        SearchSystemRubricCriteriaQuery query = new SearchSystemRubricCriteriaQuery(
                versionId, safeFilter.keyword(), safeFilter.isRequired(), page, size
        );

        return searchSystemRubricCriteriaUseCase.execute(query);
    }

    @QueryMapping(name = "searchSchoolRubricCriteria")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public PageResult<RubricCriterionDto> searchSchoolRubricCriteria(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "versionId") UUID versionId,
            @Argument(name = "filter") SearchRubricCriterionFilterRequest filter,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {

        PageArguments.validate(page, size);
        SearchRubricCriterionFilterRequest safeFilter = (filter != null) ? filter : new SearchRubricCriterionFilterRequest(null, null);

        SearchSchoolRubricCriteriaQuery query = new SearchSchoolRubricCriteriaQuery(
                schoolId, versionId, safeFilter.keyword(), safeFilter.isRequired(), page, size
        );

        return searchSchoolRubricCriteriaUseCase.execute(query);
    }

    // ====================RUBRIC RESULT BAND =====================
    // Update the system Rubric Result Band
    @MutationMapping(name = "updateSystemRubricResultBand")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public UUID updateSystemRubricResultBand(
            @Argument(name = "resultBandId") UUID resultBandId,
            @Argument(name = "input") UpdateRubricResultBandInput input
    ) {
        UpdateSystemRubricResultBandCommand command = UpdateRubricResultBandInput.toUpdateSystemRubricResultBandCommand(resultBandId, input);
        return updateSystemRubricResultBandUseCase.execute(command);
    }

    // Update the school rubric result band
    @MutationMapping(name = "updateSchoolRubricResultBand")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public UUID updateSchoolRubricResultBand(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "resultBandId") UUID resultBandId,
            @Argument(name = "input") UpdateRubricResultBandInput input
    ) {
        UpdateSchoolRubricResultBandCommand command = UpdateRubricResultBandInput.toUpdateSchoolRubricResultBandCommand(schoolId, resultBandId, input);
        return updateSchoolRubricResultBandUseCase.execute(command);
    }

    // View the system Rubric Result Band
    @QueryMapping(name = "viewSystemRubricResultBand")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public RubricResultBandDto viewSystemRubricResultBand(
            @Argument(name = "resultBandId") UUID resultBandId
    ) {
        ViewSystemRubricResultBandDetailsQuery query = new ViewSystemRubricResultBandDetailsQuery(resultBandId);
        return viewSystemRubricResultBandDetailsUseCase.execute(query);
    }

    // View the school Rubric Result Band
    @QueryMapping(name = "viewSchoolRubricResultBand")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public RubricResultBandDto viewSchoolRubricResultBand(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "resultBandId") UUID resultBandId
    ) {
        ViewSchoolRubricResultBandDetailsQuery query = new ViewSchoolRubricResultBandDetailsQuery(schoolId, resultBandId);
        return viewSchoolRubricResultBandDetailsUseCase.execute(query);
    }

    // View the system Rubric Result Bands
    @QueryMapping(name = "viewSystemRubricResultBands")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public PageResult<RubricResultBandDto> viewSystemRubricResultBands(
            @Argument(name = "versionId") UUID versionId,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size
    ) {

        PageArguments.validate(page, size);

        ViewSystemRubricResultBandsQuery query = new ViewSystemRubricResultBandsQuery(versionId, page, size);
        return viewSystemRubricResultBandsUseCase.execute(query);
    }

    // View the school Rubric Result Bands
    @QueryMapping(name = "viewSchoolRubricResultBands")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public PageResult<RubricResultBandDto> viewSchoolRubricResultBands(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "versionId") UUID versionId,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size
    ) {

        PageArguments.validate(page, size);

        ViewSchoolRubricResultBandsQuery query = new ViewSchoolRubricResultBandsQuery(schoolId, versionId, page, size);
        return viewSchoolRubricResultBandsUseCase.execute(query);
    }

    // GraphQL: SEARCH RESULT BANDS FOR SYSTEM ADMIN
    @QueryMapping(name = "searchSystemRubricResultBands")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public PageResult<RubricResultBandDto> searchSystemRubricResultBands(
            @Argument(name = "versionId") UUID versionId,
            @Argument(name = "filter") SearchRubricResultBandFilterRequest filter,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {

        PageArguments.validate(page, size);
        SearchRubricResultBandFilterRequest safeFilter = (filter != null) ? filter : new SearchRubricResultBandFilterRequest(null);

        SearchSystemRubricResultBandsQuery query = new SearchSystemRubricResultBandsQuery(versionId, safeFilter.keyword(), page, size);
        return searchSystemRubricResultBandsUseCase.execute(query);
    }

    // GraphQL: SEARCH RESULT BANDS FOR SCHOOL ADMIN
    @QueryMapping(name = "searchSchoolRubricResultBands")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public PageResult<RubricResultBandDto> searchSchoolRubricResultBands(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "versionId") UUID versionId,
            @Argument(name = "filter") SearchRubricResultBandFilterRequest filter,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {

        PageArguments.validate(page, size);
        SearchRubricResultBandFilterRequest safeFilter = (filter != null) ? filter : new SearchRubricResultBandFilterRequest(null);

        SearchSchoolRubricResultBandsQuery query = new SearchSchoolRubricResultBandsQuery(schoolId, versionId, safeFilter.keyword(), page, size);
        return searchSchoolRubricResultBandsUseCase.execute(query);
    }
}
