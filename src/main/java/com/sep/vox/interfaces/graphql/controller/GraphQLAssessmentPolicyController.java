package com.sep.vox.interfaces.graphql.controller;

import com.sep.vox.interfaces.graphql.dataloader.SchoolGraphQLDataLoaderConfig;
import com.sep.vox.interfaces.graphql.dataloader.GraphQLDataLoaders;
import com.sep.vox.interfaces.graphql.dataloader.FrameworkGraphQLDataLoaderConfig;
import com.sep.vox.interfaces.graphql.dataloader.RubricGraphQLDataLoaderConfig;
import com.sep.vox.interfaces.graphql.dataloader.SupportedLanguageGraphQLDataLoaderConfig;
import com.sep.vox.application.port.input.command.assessmentpolicysystem.UpdateAssessmentPolicyCommand;
import com.sep.vox.application.port.input.query.assessmentpolicyschool.ViewSchoolAssessmentPoliciesQuery;
import com.sep.vox.application.port.input.query.assessmentpolicyschool.ViewSchoolAssessmentPolicyDetailsQuery;
import com.sep.vox.application.port.input.query.assessmentpolicysystem.ViewSystemAssessmentPoliciesQuery;
import com.sep.vox.application.port.input.query.assessmentpolicysystem.ViewSystemAssessmentPolicyDetailsQuery;
import com.sep.vox.application.port.input.query.assessmentpolicyteacher.ViewTeacherAssessmentPoliciesQuery;
import com.sep.vox.application.port.input.usecase.assessmentpolicyschool.UpdateSchoolAssessmentPolicyUseCase;
import com.sep.vox.application.port.input.usecase.assessmentpolicyschool.ViewSchoolAssessmentPoliciesUseCase;
import com.sep.vox.application.port.input.usecase.assessmentpolicyschool.ViewSchoolAssessmentPolicyDetailsUseCase;
import com.sep.vox.application.port.input.usecase.assessmentpolicysystem.UpdateSystemAssessmentPolicyUseCase;
import com.sep.vox.application.port.input.usecase.assessmentpolicysystem.ViewSystemAssessmentPoliciesUseCase;
import com.sep.vox.application.port.input.usecase.assessmentpolicysystem.ViewSystemAssessmentPolicyDetailsUseCase;
import com.sep.vox.application.port.input.usecase.assessmentpolicyteacher.ViewTeacherAssessmentPoliciesUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.AssessmentPolicyDto;
import com.sep.vox.domain.dto.FrameworkResultBandDto;
import com.sep.vox.domain.dto.FrameworkVersionDto;
import com.sep.vox.domain.dto.RubricVersionDto;
import com.sep.vox.domain.dto.SchoolClassDto;
import com.sep.vox.domain.dto.SchoolDto;
import com.sep.vox.domain.dto.SchoolGradeDto;
import com.sep.vox.domain.dto.GradeLevelDto;
import com.sep.vox.domain.dto.SupportedLanguageDto;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStatus;
import com.sep.vox.interfaces.graphql.input.assessmentpolicysystem.UpdateAssessmentPolicyInput;
import com.sep.vox.interfaces.shared.PageArguments;
import lombok.RequiredArgsConstructor;
import org.dataloader.DataLoader;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import graphql.schema.DataFetchingEnvironment;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Controller
@RequiredArgsConstructor
public class GraphQLAssessmentPolicyController {

    private final UpdateSystemAssessmentPolicyUseCase updateSystemAssessmentPolicyUseCase;
    private final UpdateSchoolAssessmentPolicyUseCase updateSchoolAssessmentPolicyUseCase;
    private final ViewSystemAssessmentPoliciesUseCase viewSystemAssessmentPoliciesUseCase;
    private final ViewSchoolAssessmentPoliciesUseCase viewSchoolAssessmentPoliciesUseCase;
    private final ViewTeacherAssessmentPoliciesUseCase viewTeacherAssessmentPoliciesUseCase;
    private final ViewSystemAssessmentPolicyDetailsUseCase viewSystemAssessmentPolicyDetailsUseCase;
    private final ViewSchoolAssessmentPolicyDetailsUseCase viewSchoolAssessmentPolicyDetailsUseCase;

    // School Admin can read this too: it is the store of TEMPLATE policies a school picks from and
    // copies, the same way a school browses the system's template criteria set. The use case itself
    // forces only PUBLISHED versions for anyone who is not a system admin.
    @QueryMapping(name = "viewSystemAssessmentPolicies")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public PageResult<AssessmentPolicyDto> viewSystemAssessmentPolicies(
            @Argument(name = "status") AssessmentPolicyStatus status,
            @Argument(name = "languageId") UUID languageId,
            @Argument(name = "rubricVersionId") UUID rubricVersionId,
            @Argument(name = "effectiveFrom") Instant effectiveFrom,
            @Argument(name = "effectiveTo") Instant effectiveTo,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {
        PageArguments.validate(page, size);
        return viewSystemAssessmentPoliciesUseCase
                .execute(new ViewSystemAssessmentPoliciesQuery(status, languageId, rubricVersionId,
                        effectiveFrom,
                        effectiveTo, page, size));
    }

    @QueryMapping(name = "viewSchoolAssessmentPolicies")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public PageResult<AssessmentPolicyDto> viewSchoolAssessmentPolicies(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "status") AssessmentPolicyStatus status,
            @Argument(name = "languageId") UUID languageId,
            @Argument(name = "rubricVersionId") UUID rubricVersionId,
            @Argument(name = "effectiveFrom") Instant effectiveFrom,
            @Argument(name = "effectiveTo") Instant effectiveTo,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {
        PageArguments.validate(page, size);
        return viewSchoolAssessmentPoliciesUseCase
                .execute(new ViewSchoolAssessmentPoliciesQuery(schoolId, status, languageId, rubricVersionId,
                        effectiveFrom,
                        effectiveTo,
                        page, size));
    }

    @QueryMapping(name = "viewTeacherAssessmentPolicies")
    @PreAuthorize("hasRole('TEACHER')")
    public PageResult<AssessmentPolicyDto> viewTeacherAssessmentPolicies(
            @Argument(name = "languageId") UUID languageId,
            @Argument(name = "rubricVersionId") UUID rubricVersionId,
            @Argument(name = "effectiveFrom") Instant effectiveFrom,
            @Argument(name = "effectiveTo") Instant effectiveTo,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {
        PageArguments.validate(page, size);
        return viewTeacherAssessmentPoliciesUseCase.execute(new ViewTeacherAssessmentPoliciesQuery(
                languageId,
                rubricVersionId,
                effectiveFrom,
                effectiveTo,
                page,
                size));
    }

    @QueryMapping(name = "viewSystemAssessmentPolicy")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public AssessmentPolicyDto viewSystemAssessmentPolicy(@Argument(name = "policyId") UUID policyId) {
        return viewSystemAssessmentPolicyDetailsUseCase.execute(new ViewSystemAssessmentPolicyDetailsQuery(policyId));
    }

    @QueryMapping(name = "viewSchoolAssessmentPolicy")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public AssessmentPolicyDto viewSchoolAssessmentPolicy(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "policyId") UUID policyId) {
        return viewSchoolAssessmentPolicyDetailsUseCase
                .execute(new ViewSchoolAssessmentPolicyDetailsQuery(schoolId, policyId));
    }

    @SchemaMapping(typeName = "AssessmentPolicy", field = "school")
    public CompletableFuture<SchoolDto> getSchool(AssessmentPolicyDto policy, DataFetchingEnvironment env) {
        if (policy.schoolId() == null) {
            return CompletableFuture.completedFuture(null);
        }
        DataLoader<UUID, SchoolDto> loader = GraphQLDataLoaders.require(env, SchoolGraphQLDataLoaderConfig.SCHOOL_BY_SCHOOL_ID);
        return loader.load(policy.schoolId());
    }

    @SchemaMapping(typeName = "AssessmentPolicy", field = "gradeLevel")
    public CompletableFuture<GradeLevelDto> getGradeLevel(AssessmentPolicyDto policy,
            DataFetchingEnvironment env) {
        if (policy.gradeLevelId() == null) {
            return CompletableFuture.completedFuture(null);
        }
        DataLoader<UUID, GradeLevelDto> loader = GraphQLDataLoaders.require(env, SchoolGraphQLDataLoaderConfig.GRADE_LEVEL_BY_GRADE_LEVEL_ID);
        return loader.load(policy.gradeLevelId());
    }

    @SchemaMapping(typeName = "AssessmentPolicy", field = "schoolGrade")
    public CompletableFuture<SchoolGradeDto> getSchoolGrade(AssessmentPolicyDto policy, DataFetchingEnvironment env) {
        if (policy.schoolGradeId() == null) {
            return CompletableFuture.completedFuture(null);
        }
        DataLoader<UUID, SchoolGradeDto> loader = GraphQLDataLoaders.require(env, SchoolGraphQLDataLoaderConfig.SCHOOL_GRADE_BY_SCHOOL_GRADE_ID);
        return loader.load(policy.schoolGradeId());
    }

    @SchemaMapping(typeName = "AssessmentPolicy", field = "schoolClass")
    public CompletableFuture<SchoolClassDto> getSchoolClass(AssessmentPolicyDto policy, DataFetchingEnvironment env) {
        if (policy.schoolClassId() == null) {
            return CompletableFuture.completedFuture(null);
        }
        DataLoader<UUID, SchoolClassDto> loader = GraphQLDataLoaders.require(env, SchoolGraphQLDataLoaderConfig.SCHOOL_CLASS_BY_SCHOOL_CLASS_ID);
        return loader.load(policy.schoolClassId());
    }

    @SchemaMapping(typeName = "AssessmentPolicy", field = "language")
    public CompletableFuture<SupportedLanguageDto> getLanguage(AssessmentPolicyDto policy,
            DataFetchingEnvironment env) {
        DataLoader<UUID, SupportedLanguageDto> loader = GraphQLDataLoaders.require(env, SupportedLanguageGraphQLDataLoaderConfig.SUPPORTED_LANGUAGE_BY_SUPPORTED_LANGUAGE_ID);
        return loader.load(policy.languageId());
    }

    @SchemaMapping(typeName = "AssessmentPolicy", field = "frameworkVersion")
    public CompletableFuture<FrameworkVersionDto> getFrameworkVersion(AssessmentPolicyDto policy,
            DataFetchingEnvironment env) {
        DataLoader<UUID, FrameworkVersionDto> loader = GraphQLDataLoaders.require(env, FrameworkGraphQLDataLoaderConfig.FRAMEWORK_VERSION_BY_FRAMEWORK_VERSION_ID);
        return loader.load(policy.frameworkVersionId());
    }

    @SchemaMapping(typeName = "AssessmentPolicy", field = "rubricVersion")
    public CompletableFuture<RubricVersionDto> getRubricVersion(AssessmentPolicyDto policy,
            DataFetchingEnvironment env) {
        DataLoader<UUID, RubricVersionDto> loader = GraphQLDataLoaders.require(env, RubricGraphQLDataLoaderConfig.RUBRIC_VERSION_BY_RUBRIC_VERSION_ID);
        return loader.load(policy.rubricVersionId());
    }

    @SchemaMapping(typeName = "AssessmentPolicy", field = "targetFrameworkBand")
    public CompletableFuture<FrameworkResultBandDto> getTargetFrameworkBand(AssessmentPolicyDto policy,
            DataFetchingEnvironment env) {
        DataLoader<UUID, FrameworkResultBandDto> loader = GraphQLDataLoaders.require(env, FrameworkGraphQLDataLoaderConfig.FRAMEWORK_RESULT_BAND_BY_FRAMEWORK_RESULT_BAND_ID);
        return loader.load(policy.targetFrameworkBandId());
    }

    @MutationMapping(name = "updateSystemAssessmentPolicy")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public UUID updateSystemAssessmentPolicy(
            @Argument(name = "policyId") UUID policyId,
            @Argument(name = "input") UpdateAssessmentPolicyInput input) {
        UpdateAssessmentPolicyCommand command = UpdateAssessmentPolicyInput.toUpdateAssessmentPolicyCommand(policyId, input);
        return updateSystemAssessmentPolicyUseCase.execute(command);
    }

    @MutationMapping(name = "updateSchoolAssessmentPolicy")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public UUID updateSchoolAssessmentPolicy(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "policyId") UUID policyId,
            @Argument(name = "input") UpdateAssessmentPolicyInput input) {
        UpdateAssessmentPolicyCommand command = UpdateAssessmentPolicyInput.toUpdateAssessmentPolicyCommand(schoolId, policyId, input);
        return updateSchoolAssessmentPolicyUseCase.execute(command);
    }
}
