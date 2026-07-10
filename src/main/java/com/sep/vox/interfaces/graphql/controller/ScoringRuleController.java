package com.sep.vox.interfaces.graphql.controller;

import java.util.UUID;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.sep.vox.application.port.input.query.SearchSchoolScoringRuleQuery;
import com.sep.vox.application.port.input.query.SearchScoringRuleQuery;
import com.sep.vox.application.port.input.query.ViewAllSchoolScoringRuleQuery;
import com.sep.vox.application.port.input.query.ViewAllScoringRuleQuery;
import com.sep.vox.application.port.input.query.ViewSchoolScoringRuleDetailQuery;
import com.sep.vox.application.port.input.query.ViewScoringRuleDetailQuery;
import com.sep.vox.application.port.input.usecase.scoringrule.SearchSchoolScoringRuleUseCase;
import com.sep.vox.application.port.input.usecase.scoringrule.SearchSystemScoringRuleUseCase;
import com.sep.vox.application.port.input.usecase.scoringrule.UpdateSchoolScoringRuleUseCase;
import com.sep.vox.application.port.input.usecase.scoringrule.UpdateSystemScoringRuleUseCase;
import com.sep.vox.application.port.input.usecase.scoringrule.ViewAllSchoolScoringRuleUseCase;
import com.sep.vox.application.port.input.usecase.scoringrule.ViewAllSystemScoringRuleUseCase;
import com.sep.vox.application.port.input.usecase.scoringrule.ViewSchoolScoringRuleDetailUseCase;
import com.sep.vox.application.port.input.usecase.scoringrule.ViewSystemScoringRuleDetailUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.ScoringRuleDto;
import com.sep.vox.domain.mapper.ScoringRuleParamsMapper;
import com.sep.vox.interfaces.graphql.dto.request.UpdateScoringRuleInput;
import com.sep.vox.interfaces.graphql.mapper.UpdateScoringRuleGraphQLMapper;


@Controller("graphqlScoringRuleController")
public class ScoringRuleController {

    private final SearchSystemScoringRuleUseCase searchSystemScoringRuleUseCase;
    private final SearchSchoolScoringRuleUseCase searchSchoolScoringRuleUseCase;
    private final ViewAllSystemScoringRuleUseCase viewAllSystemScoringRuleUseCase;
    private final ViewAllSchoolScoringRuleUseCase viewAllSchoolScoringRuleUseCase;
    private final ViewSystemScoringRuleDetailUseCase viewSystemScoringRuleDetailUseCase;
    private final ViewSchoolScoringRuleDetailUseCase viewSchoolScoringRuleDetailUseCase;
    private final UpdateSystemScoringRuleUseCase updateSystemScoringRuleUseCase;
    private final UpdateSchoolScoringRuleUseCase updateSchoolScoringRuleUseCase;

    public ScoringRuleController(
            SearchSystemScoringRuleUseCase searchSystemScoringRuleUseCase,
            SearchSchoolScoringRuleUseCase searchSchoolScoringRuleUseCase,
            ViewAllSystemScoringRuleUseCase viewAllSystemScoringRuleUseCase,
            ViewAllSchoolScoringRuleUseCase viewAllSchoolScoringRuleUseCase,
            ViewSystemScoringRuleDetailUseCase viewSystemScoringRuleDetailUseCase,
            ViewSchoolScoringRuleDetailUseCase viewSchoolScoringRuleDetailUseCase,
            UpdateSystemScoringRuleUseCase updateSystemScoringRuleUseCase,
            UpdateSchoolScoringRuleUseCase updateSchoolScoringRuleUseCase) {
        this.searchSystemScoringRuleUseCase = searchSystemScoringRuleUseCase;
        this.searchSchoolScoringRuleUseCase = searchSchoolScoringRuleUseCase;
        this.viewAllSystemScoringRuleUseCase = viewAllSystemScoringRuleUseCase;
        this.viewAllSchoolScoringRuleUseCase = viewAllSchoolScoringRuleUseCase;
        this.viewSystemScoringRuleDetailUseCase = viewSystemScoringRuleDetailUseCase;
        this.viewSchoolScoringRuleDetailUseCase = viewSchoolScoringRuleDetailUseCase;
        this.updateSystemScoringRuleUseCase = updateSystemScoringRuleUseCase;
        this.updateSchoolScoringRuleUseCase = updateSchoolScoringRuleUseCase;
    }

    @QueryMapping(name = "searchSystemScoringRules")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public PageResult<ScoringRuleDto> searchSystemScoringRules(
            @Argument(name = "policyId") UUID policyId,
            @Argument(name = "keyword") String keyword,
            @Argument(name = "isActive") Boolean isActive,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {
        int validPage = (page != null && page > 0) ? page : 1;
        int validSize = (size != null && size > 0) ? size : 10;
        return searchSystemScoringRuleUseCase.execute(
                new SearchScoringRuleQuery(policyId, keyword, isActive, validPage, validSize));
    }

    @QueryMapping(name = "searchSchoolScoringRules")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public PageResult<ScoringRuleDto> searchSchoolScoringRules(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "policyId") UUID policyId,
            @Argument(name = "keyword") String keyword,
            @Argument(name = "isActive") Boolean isActive,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {
        int validPage = (page != null && page > 0) ? page : 1;
        int validSize = (size != null && size > 0) ? size : 10;
        return searchSchoolScoringRuleUseCase.execute(
                new SearchSchoolScoringRuleQuery(schoolId, policyId, keyword, isActive, validPage, validSize));
    }

    // Xem toàn bộ (phân trang) danh sách Scoring Rule thuộc 1 Assessment Policy hệ thống.
    @QueryMapping(name = "viewAllSystemScoringRules")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public PageResult<ScoringRuleDto> viewAllSystemScoringRules(
            @Argument(name = "policyId") UUID policyId,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {
        int validPage = (page != null && page > 0) ? page : 1;
        int validSize = (size != null && size > 0) ? size : 10;
        return viewAllSystemScoringRuleUseCase.execute(new ViewAllScoringRuleQuery(policyId, validPage, validSize));
    }

    // Xem toàn bộ (phân trang) danh sách Scoring Rule thuộc 1 Assessment Policy của trường học.
    @QueryMapping(name = "viewAllSchoolScoringRules")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public PageResult<ScoringRuleDto> viewAllSchoolScoringRules(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "policyId") UUID policyId,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {
        int validPage = (page != null && page > 0) ? page : 1;
        int validSize = (size != null && size > 0) ? size : 10;
        return viewAllSchoolScoringRuleUseCase.execute(
                new ViewAllSchoolScoringRuleQuery(schoolId, policyId, validPage, validSize));
    }

    // Xem chi tiết 1 Scoring Rule của Assessment Policy hệ thống.
    @QueryMapping(name = "viewSystemScoringRuleDetails")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ScoringRuleDto viewSystemScoringRuleDetails(
            @Argument(name = "policyId") UUID policyId,
            @Argument(name = "ruleId") UUID ruleId) {
        return viewSystemScoringRuleDetailUseCase.execute(new ViewScoringRuleDetailQuery(policyId, ruleId));
    }

    // Xem chi tiết 1 Scoring Rule của Assessment Policy của trường học.
    @QueryMapping(name = "viewSchoolScoringRuleDetails")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ScoringRuleDto viewSchoolScoringRuleDetails(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "policyId") UUID policyId,
            @Argument(name = "ruleId") UUID ruleId) {
        return viewSchoolScoringRuleDetailUseCase.execute(new ViewSchoolScoringRuleDetailQuery(schoolId, policyId, ruleId));
    }

    // Cập nhật Scoring Rule (chỉ khi Policy đang DRAFT - business rule được kiểm tra trong UseCase)
    @MutationMapping(name = "updateSystemScoringRule")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public UUID updateSystemScoringRule(
            @Argument(name = "policyId") UUID policyId,
            @Argument(name = "ruleId") UUID ruleId,
            @Argument(name = "input") UpdateScoringRuleInput input) {
        var command = UpdateScoringRuleGraphQLMapper.fromInput(policyId, ruleId, input);
        return updateSystemScoringRuleUseCase.execute(command);
    }

    @MutationMapping(name = "updateSchoolScoringRule")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public UUID updateSchoolScoringRule(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "policyId") UUID policyId,
            @Argument(name = "ruleId") UUID ruleId,
            @Argument(name = "input") UpdateScoringRuleInput input) {
        var command = UpdateScoringRuleGraphQLMapper.fromSchoolInput(schoolId, policyId, ruleId, input);
        return updateSchoolScoringRuleUseCase.execute(command);
    }

    // conditionParams/actionParams trong ScoringRuleDto là Map<String,Object> (dùng cho REST),
    // nhưng schema GraphQL không có scalar Map/JSON nên phải convert thành chuỗi JSON ở đây.
    @SchemaMapping(typeName = "ScoringRule", field = "conditionParamsJson")
    public String getConditionParamsJson(ScoringRuleDto rule) {
        return ScoringRuleParamsMapper.toJsonString(rule.conditionParams());
    }

    @SchemaMapping(typeName = "ScoringRule", field = "actionParamsJson")
    public String getActionParamsJson(ScoringRuleDto rule) {
        return ScoringRuleParamsMapper.toJsonString(rule.actionParams());
    }
}