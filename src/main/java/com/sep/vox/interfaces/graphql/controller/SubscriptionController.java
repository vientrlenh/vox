package com.sep.vox.interfaces.graphql.controller;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.dataloader.DataLoader;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import graphql.schema.DataFetchingEnvironment;

import com.sep.vox.application.port.input.query.ViewCurrentSubscriptionQuery;
import com.sep.vox.application.port.input.query.ViewFinancialEventsQuery;
import com.sep.vox.application.port.input.query.ViewInvoicesQuery;
import com.sep.vox.application.port.input.query.ViewPlanDetailQuery;
import com.sep.vox.application.port.input.query.ViewPlansQuery;
import com.sep.vox.application.port.input.query.ViewRequestsQuery;
import com.sep.vox.application.port.input.query.ViewSchoolSubscriptionsQuery;
import com.sep.vox.application.port.input.query.ViewSubscriptionHistoryQuery;
import com.sep.vox.application.port.input.query.ViewTokenPurchasesQuery;
import com.sep.vox.application.port.input.query.ViewUsageQuery;
import com.sep.vox.application.port.input.usecase.subscription.UpdatePlanUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ViewCurrentSubscriptionUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ViewFinancialEventsUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ViewInvoicesUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ViewPlanDetailUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ViewPlansUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ViewRequestsUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ViewSchoolSubscriptionsUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ViewSubscriptionHistoryUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ViewTokenPurchasesUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ViewUsageUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.FinancialEventDto;
import com.sep.vox.domain.dto.InvoiceDto;
import com.sep.vox.domain.dto.SchoolSubscriptionDto;
import com.sep.vox.domain.dto.SubscriptionPlanDto;
import com.sep.vox.domain.dto.SubscriptionQuotaDto;
import com.sep.vox.domain.dto.SubscriptionRequestDto;
import com.sep.vox.domain.dto.TokenPurchaseDto;
import com.sep.vox.domain.model.subscription.RequestStatus;
import com.sep.vox.domain.model.subscription.SubscriptionStatus;
import com.sep.vox.interfaces.graphql.dto.request.UpdateSubscriptionPlanInput;
import com.sep.vox.interfaces.graphql.mapper.UpdateSubscriptionPlanCommandMapper;

@Controller("graphqlSubscriptionController")
public class SubscriptionController {

    private final ViewPlansUseCase viewPlansUseCase;
    private final ViewPlanDetailUseCase viewPlanDetailUseCase;
    private final UpdatePlanUseCase updatePlanUseCase;
    private final ViewSchoolSubscriptionsUseCase viewSchoolSubscriptionsUseCase;
    private final ViewCurrentSubscriptionUseCase viewCurrentSubscriptionUseCase;
    private final ViewSubscriptionHistoryUseCase viewSubscriptionHistoryUseCase;
    private final ViewRequestsUseCase viewRequestsUseCase;
    private final ViewTokenPurchasesUseCase viewTokenPurchasesUseCase;
    private final ViewUsageUseCase viewUsageUseCase;
    private final ViewInvoicesUseCase viewInvoicesUseCase;
    private final ViewFinancialEventsUseCase viewFinancialEventsUseCase;

    public SubscriptionController(
            ViewPlansUseCase viewPlansUseCase,
            ViewPlanDetailUseCase viewPlanDetailUseCase,
            UpdatePlanUseCase updatePlanUseCase,
            ViewSchoolSubscriptionsUseCase viewSchoolSubscriptionsUseCase,
            ViewCurrentSubscriptionUseCase viewCurrentSubscriptionUseCase,
            ViewSubscriptionHistoryUseCase viewSubscriptionHistoryUseCase,
            ViewRequestsUseCase viewRequestsUseCase,
            ViewTokenPurchasesUseCase viewTokenPurchasesUseCase,
            ViewUsageUseCase viewUsageUseCase,
            ViewInvoicesUseCase viewInvoicesUseCase,
            ViewFinancialEventsUseCase viewFinancialEventsUseCase) {
        this.viewPlansUseCase = viewPlansUseCase;
        this.viewPlanDetailUseCase = viewPlanDetailUseCase;
        this.updatePlanUseCase = updatePlanUseCase;
        this.viewSchoolSubscriptionsUseCase = viewSchoolSubscriptionsUseCase;
        this.viewCurrentSubscriptionUseCase = viewCurrentSubscriptionUseCase;
        this.viewSubscriptionHistoryUseCase = viewSubscriptionHistoryUseCase;
        this.viewRequestsUseCase = viewRequestsUseCase;
        this.viewTokenPurchasesUseCase = viewTokenPurchasesUseCase;
        this.viewUsageUseCase = viewUsageUseCase;
        this.viewInvoicesUseCase = viewInvoicesUseCase;
        this.viewFinancialEventsUseCase = viewFinancialEventsUseCase;
    }

    @QueryMapping(name = "subscriptionPlans")
    public PageResult<SubscriptionPlanDto> subscriptionPlans(
            @Argument(name = "page") int page,
            @Argument(name = "size") int size) {
        return viewPlansUseCase.execute(new ViewPlansQuery(page, size));
    }

    @QueryMapping(name = "subscriptionPlan")
    public SubscriptionPlanDto subscriptionPlan(@Argument(name = "id") UUID id) {
        return viewPlanDetailUseCase.execute(new ViewPlanDetailQuery(id));
    }

    @QueryMapping(name = "schoolSubscriptions")
    public PageResult<SchoolSubscriptionDto> schoolSubscriptions(
            @Argument(name = "keyword") String keyword,
            @Argument(name = "planId") UUID planId,
            @Argument(name = "status") SubscriptionStatus status,
            @Argument(name = "page") int page,
            @Argument(name = "size") int size) {
        return viewSchoolSubscriptionsUseCase.execute(new ViewSchoolSubscriptionsQuery(keyword, planId, status, page, size));
    }

    @QueryMapping(name = "schoolSubscription")
    public SchoolSubscriptionDto schoolSubscription(@Argument(name = "schoolId") UUID schoolId) {
        return viewCurrentSubscriptionUseCase.execute(new ViewCurrentSubscriptionQuery(schoolId));
    }

    @QueryMapping(name = "schoolSubscriptionHistory")
    public List<SchoolSubscriptionDto> schoolSubscriptionHistory(@Argument(name = "schoolId") UUID schoolId) {
        return viewSubscriptionHistoryUseCase.execute(new ViewSubscriptionHistoryQuery(schoolId));
    }

    @QueryMapping(name = "subscriptionRequests")
    public PageResult<SubscriptionRequestDto> subscriptionRequests(
            @Argument(name = "status") RequestStatus status,
            @Argument(name = "page") int page,
            @Argument(name = "size") int size) {
        return viewRequestsUseCase.execute(new ViewRequestsQuery(status, page, size));
    }

    @QueryMapping(name = "tokenPurchases")
    public List<TokenPurchaseDto> tokenPurchases(@Argument(name = "schoolId") UUID schoolId) {
        return viewTokenPurchasesUseCase.execute(new ViewTokenPurchasesQuery(schoolId));
    }

    @QueryMapping(name = "subscriptionUsage")
    public List<SubscriptionQuotaDto> subscriptionUsage(@Argument(name = "schoolId") UUID schoolId) {
        return viewUsageUseCase.execute(new ViewUsageQuery(schoolId));
    }

    @QueryMapping(name = "invoices")
    public PageResult<InvoiceDto> invoices(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "page") int page,
            @Argument(name = "size") int size) {
        return viewInvoicesUseCase.execute(new ViewInvoicesQuery(schoolId, page, size));
    }

    @QueryMapping(name = "financialEvents")
    public PageResult<FinancialEventDto> financialEvents(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "page") int page,
            @Argument(name = "size") int size) {
        return viewFinancialEventsUseCase.execute(new ViewFinancialEventsQuery(schoolId, page, size));
    }

    @MutationMapping(name = "updateSubscriptionPlan")
    public SubscriptionPlanDto updateSubscriptionPlan(
            @Argument(name = "id") UUID id,
            @Argument(name = "input") UpdateSubscriptionPlanInput input) {
        return updatePlanUseCase.execute(UpdateSubscriptionPlanCommandMapper.fromInput(id, input));
    }

    @SchemaMapping(typeName = "SchoolSubscription", field = "plan")
    public CompletableFuture<SubscriptionPlanDto> plan(SchoolSubscriptionDto source, DataFetchingEnvironment env) {
        DataLoader<UUID, SubscriptionPlanDto> loader = env.getDataLoader("subscriptionPlanById");
        return loader.load(source.planId());
    }
}
