package com.sep.vox.interfaces.graphql.controller;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.port.input.query.ViewSchoolAssessmentPoliciesQuery;
import com.sep.vox.application.port.input.query.ViewSchoolAssessmentPolicyDetailsQuery;
import com.sep.vox.application.port.input.query.ViewSystemAssessmentPoliciesQuery;
import com.sep.vox.application.port.input.query.ViewSystemAssessmentPolicyDetailsQuery;
import com.sep.vox.application.port.input.query.ViewTeacherAssessmentPoliciesQuery;
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
import com.sep.vox.interfaces.graphql.dto.request.UpdateAssessmentPolicyInput;
import com.sep.vox.interfaces.graphql.mapper.UpdateAssessmentPolicyGraphQLMapper;
import org.dataloader.DataLoader;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import graphql.schema.DataFetchingEnvironment;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Controller("graphqlAssessmentPolicyController")
public class AssessmentPolicyController {

    private final UpdateSystemAssessmentPolicyUseCase updateSystemAssessmentPolicyUseCase;
    private final UpdateSchoolAssessmentPolicyUseCase updateSchoolAssessmentPolicyUseCase;
    private final ViewSystemAssessmentPoliciesUseCase viewSystemAssessmentPoliciesUseCase;
    private final ViewSchoolAssessmentPoliciesUseCase viewSchoolAssessmentPoliciesUseCase;
    private final ViewTeacherAssessmentPoliciesUseCase viewTeacherAssessmentPoliciesUseCase;
    private final ViewSystemAssessmentPolicyDetailsUseCase viewSystemAssessmentPolicyDetailsUseCase;
    private final ViewSchoolAssessmentPolicyDetailsUseCase viewSchoolAssessmentPolicyDetailsUseCase;

    public AssessmentPolicyController(
            UpdateSystemAssessmentPolicyUseCase updateSystemAssessmentPolicyUseCase,
            UpdateSchoolAssessmentPolicyUseCase updateSchoolAssessmentPolicyUseCase,
            ViewSystemAssessmentPoliciesUseCase viewSystemAssessmentPoliciesUseCase,
            ViewSchoolAssessmentPoliciesUseCase viewSchoolAssessmentPoliciesUseCase,
            ViewTeacherAssessmentPoliciesUseCase viewTeacherAssessmentPoliciesUseCase,
            ViewSystemAssessmentPolicyDetailsUseCase viewSystemAssessmentPolicyDetailsUseCase,
            ViewSchoolAssessmentPolicyDetailsUseCase viewSchoolAssessmentPolicyDetailsUseCase) {
        this.updateSystemAssessmentPolicyUseCase = updateSystemAssessmentPolicyUseCase;
        this.updateSchoolAssessmentPolicyUseCase = updateSchoolAssessmentPolicyUseCase;
        this.viewSystemAssessmentPoliciesUseCase = viewSystemAssessmentPoliciesUseCase;
        this.viewSchoolAssessmentPoliciesUseCase = viewSchoolAssessmentPoliciesUseCase;
        this.viewTeacherAssessmentPoliciesUseCase = viewTeacherAssessmentPoliciesUseCase;
        this.viewSystemAssessmentPolicyDetailsUseCase = viewSystemAssessmentPolicyDetailsUseCase;
        this.viewSchoolAssessmentPolicyDetailsUseCase = viewSchoolAssessmentPolicyDetailsUseCase;
    }

    // School Admin cũng đọc được: đây là kho chính sách MẪU để trường chọn rồi sao về, giống
    // cách trường duyệt bộ tiêu chí mẫu của hệ thống. Use case tự ép chỉ trả bản PUBLISHED cho
    // người không phải quản trị hệ thống.
    @QueryMapping(name = "viewSystemAssessmentPolicies")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public PageResult<AssessmentPolicyDto> viewSystemAssessmentPolicies(
            @Argument(name = "status") String status,
            @Argument(name = "languageId") UUID languageId,
            @Argument(name = "rubricVersionId") UUID rubricVersionId,
            @Argument(name = "effectiveFrom") String effectiveFrom,
            @Argument(name = "effectiveTo") String effectiveTo,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {
        int validPage = (page != null && page > 0) ? page : 1;
        int validSize = (size != null && size > 0) ? size : 10;
        return viewSystemAssessmentPoliciesUseCase
                .execute(new ViewSystemAssessmentPoliciesQuery(status, languageId, rubricVersionId,
                        DateMapper.toInstant(effectiveFrom),
                        DateMapper.toInstant(effectiveTo), validPage, validSize));
    }

    @QueryMapping(name = "viewSchoolAssessmentPolicies")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public PageResult<AssessmentPolicyDto> viewSchoolAssessmentPolicies(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "status") String status,
            @Argument(name = "languageId") UUID languageId,
            @Argument(name = "rubricVersionId") UUID rubricVersionId,
            @Argument(name = "effectiveFrom") String effectiveFrom,
            @Argument(name = "effectiveTo") String effectiveTo,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {
        int validPage = (page != null && page > 0) ? page : 1;
        int validSize = (size != null && size > 0) ? size : 10;
        return viewSchoolAssessmentPoliciesUseCase
                .execute(new ViewSchoolAssessmentPoliciesQuery(schoolId, status, languageId, rubricVersionId,
                        DateMapper.toInstant(effectiveFrom),
                        DateMapper.toInstant(effectiveTo),
                        validPage, validSize));
    }

    @QueryMapping(name = "viewTeacherAssessmentPolicies")
    @PreAuthorize("hasRole('TEACHER')")
    public PageResult<AssessmentPolicyDto> viewTeacherAssessmentPolicies(
            @Argument(name = "languageId") UUID languageId,
            @Argument(name = "rubricVersionId") UUID rubricVersionId,
            @Argument(name = "effectiveFrom") String effectiveFrom,
            @Argument(name = "effectiveTo") String effectiveTo,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {
        int validPage = (page != null && page > 0) ? page : 1;
        int validSize = (size != null && size > 0) ? size : 10;
        return viewTeacherAssessmentPoliciesUseCase.execute(new ViewTeacherAssessmentPoliciesQuery(
                languageId,
                rubricVersionId,
                DateMapper.toInstant(effectiveFrom),
                DateMapper.toInstant(effectiveTo),
                validPage,
                validSize));
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
        DataLoader<UUID, SchoolDto> loader = env.getDataLoader("schoolDataLoader");
        return loader.load(policy.schoolId());
    }

    @SchemaMapping(typeName = "AssessmentPolicy", field = "gradeLevel")
    public CompletableFuture<GradeLevelDto> getGradeLevel(AssessmentPolicyDto policy,
            DataFetchingEnvironment env) {
        if (policy.gradeLevelId() == null) {
            return CompletableFuture.completedFuture(null);
        }
        DataLoader<UUID, GradeLevelDto> loader = env.getDataLoader("gradeLevelDataLoader");
        return loader.load(policy.gradeLevelId());
    }

    @SchemaMapping(typeName = "AssessmentPolicy", field = "schoolGrade")
    public CompletableFuture<SchoolGradeDto> getSchoolGrade(AssessmentPolicyDto policy, DataFetchingEnvironment env) {
        if (policy.schoolGradeId() == null) {
            return CompletableFuture.completedFuture(null);
        }
        DataLoader<UUID, SchoolGradeDto> loader = env.getDataLoader("schoolGradeById");
        return loader.load(policy.schoolGradeId());
    }

    @SchemaMapping(typeName = "AssessmentPolicy", field = "schoolClass")
    public CompletableFuture<SchoolClassDto> getSchoolClass(AssessmentPolicyDto policy, DataFetchingEnvironment env) {
        if (policy.schoolClassId() == null) {
            return CompletableFuture.completedFuture(null);
        }
        DataLoader<UUID, SchoolClassDto> loader = env.getDataLoader("schoolClassDataLoader");
        return loader.load(policy.schoolClassId());
    }

    @SchemaMapping(typeName = "AssessmentPolicy", field = "language")
    public CompletableFuture<SupportedLanguageDto> getLanguage(AssessmentPolicyDto policy,
            DataFetchingEnvironment env) {
        DataLoader<UUID, SupportedLanguageDto> loader = env.getDataLoader("supportedLanguageDataLoader");
        return loader.load(policy.languageId());
    }

    @SchemaMapping(typeName = "AssessmentPolicy", field = "frameworkVersion")
    public CompletableFuture<FrameworkVersionDto> getFrameworkVersion(AssessmentPolicyDto policy,
            DataFetchingEnvironment env) {
        DataLoader<UUID, FrameworkVersionDto> loader = env.getDataLoader("frameworkVersionDataLoader");
        return loader.load(policy.frameworkVersionId());
    }

    @SchemaMapping(typeName = "AssessmentPolicy", field = "rubricVersion")
    public CompletableFuture<RubricVersionDto> getRubricVersion(AssessmentPolicyDto policy,
            DataFetchingEnvironment env) {
        DataLoader<UUID, RubricVersionDto> loader = env.getDataLoader("rubricVersionDataLoader");
        return loader.load(policy.rubricVersionId());
    }

    @SchemaMapping(typeName = "AssessmentPolicy", field = "targetFrameworkBand")
    public CompletableFuture<FrameworkResultBandDto> getTargetFrameworkBand(AssessmentPolicyDto policy,
            DataFetchingEnvironment env) {
        DataLoader<UUID, FrameworkResultBandDto> loader = env.getDataLoader("frameworkResultBandDataLoader");
        return loader.load(policy.targetFrameworkBandId());
    }

    @MutationMapping(name = "updateSystemAssessmentPolicy")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public UUID updateSystemAssessmentPolicy(
            @Argument(name = "policyId") UUID policyId,
            @Argument(name = "input") UpdateAssessmentPolicyInput input) {
        var command = UpdateAssessmentPolicyGraphQLMapper.fromSystemInput(policyId, input);
        return updateSystemAssessmentPolicyUseCase.execute(command);
    }

    @MutationMapping(name = "updateSchoolAssessmentPolicy")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public UUID updateSchoolAssessmentPolicy(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "policyId") UUID policyId,
            @Argument(name = "input") UpdateAssessmentPolicyInput input) {
        var command = UpdateAssessmentPolicyGraphQLMapper.fromSchoolInput(schoolId, policyId, input);
        return updateSchoolAssessmentPolicyUseCase.execute(command);
    }
}
