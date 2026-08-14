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

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.port.input.query.ViewCurrentSubscriptionQuery;
import com.sep.vox.application.port.input.query.ViewFinancialEventsQuery;
import com.sep.vox.application.port.input.query.ViewInvoicesQuery;
import com.sep.vox.application.port.input.query.ViewPlanDetailQuery;
import com.sep.vox.application.port.input.query.ViewPlansQuery;
import com.sep.vox.application.port.input.query.ViewRequestsQuery;
import com.sep.vox.application.port.input.query.ViewSchoolDebtEventsQuery;
import com.sep.vox.application.port.input.query.ViewSchoolSubscriptionsQuery;
import com.sep.vox.application.port.input.query.ViewSubscriptionHistoryQuery;
import com.sep.vox.application.port.input.query.ViewTokenPurchasesQuery;
import com.sep.vox.application.port.input.query.ViewTokenUsageTimeseriesQuery;
import com.sep.vox.application.port.input.query.ViewUsageQuery;
import com.sep.vox.application.port.input.usecase.subscription.UpdatePlanUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ViewCurrentSubscriptionUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ViewFinancialEventsUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ViewInvoicesUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ViewMyClassTestQuotaAllocationUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ViewMyPracticeQuotaAllocationUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ViewPlanDetailUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ViewPlansUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ViewRequestsUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ViewSchoolDebtEventsUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ViewSchoolSubscriptionsUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ViewSubscriptionHistoryUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ViewTokenPurchasesUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ViewTokenUsageTimeseriesUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ViewUsageUseCase;
import com.sep.vox.application.port.output.QuotaPricingPort;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.FinancialEventDto;
import com.sep.vox.domain.dto.InvoiceDto;
import com.sep.vox.domain.dto.MyClassTestQuotaAllocationDto;
import com.sep.vox.domain.dto.MyPracticeQuotaAllocationDto;
import com.sep.vox.domain.dto.QuotaPricingDto;
import com.sep.vox.domain.dto.SchoolDebtEventDto;
import com.sep.vox.domain.dto.SchoolSubscriptionDto;
import com.sep.vox.domain.dto.SubscriptionPlanDto;
import com.sep.vox.domain.dto.SubscriptionQuotaDto;
import com.sep.vox.domain.dto.SubscriptionRequestDto;
import com.sep.vox.domain.dto.TokenPurchaseDto;
import com.sep.vox.domain.dto.TokenUsageTimeseriesDto;
import com.sep.vox.domain.model.subscription.RequestStatus;
import com.sep.vox.domain.model.subscription.SubscriptionStatus;
import com.sep.vox.domain.model.subscription.TokenUsageGranularity;
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
    private final ViewTokenUsageTimeseriesUseCase viewTokenUsageTimeseriesUseCase;
    private final ViewMyClassTestQuotaAllocationUseCase viewMyClassTestQuotaAllocationUseCase;
    private final ViewMyPracticeQuotaAllocationUseCase viewMyPracticeQuotaAllocationUseCase;
    private final ViewInvoicesUseCase viewInvoicesUseCase;
    private final ViewFinancialEventsUseCase viewFinancialEventsUseCase;
    private final ViewSchoolDebtEventsUseCase viewSchoolDebtEventsUseCase;
    private final QuotaPricingPort quotaPricingPort;

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
            ViewTokenUsageTimeseriesUseCase viewTokenUsageTimeseriesUseCase,
            ViewMyClassTestQuotaAllocationUseCase viewMyClassTestQuotaAllocationUseCase,
            ViewMyPracticeQuotaAllocationUseCase viewMyPracticeQuotaAllocationUseCase,
            ViewInvoicesUseCase viewInvoicesUseCase,
            ViewFinancialEventsUseCase viewFinancialEventsUseCase,
            ViewSchoolDebtEventsUseCase viewSchoolDebtEventsUseCase,
            QuotaPricingPort quotaPricingPort) {
        this.viewPlansUseCase = viewPlansUseCase;
        this.viewPlanDetailUseCase = viewPlanDetailUseCase;
        this.updatePlanUseCase = updatePlanUseCase;
        this.viewSchoolSubscriptionsUseCase = viewSchoolSubscriptionsUseCase;
        this.viewCurrentSubscriptionUseCase = viewCurrentSubscriptionUseCase;
        this.viewSubscriptionHistoryUseCase = viewSubscriptionHistoryUseCase;
        this.viewRequestsUseCase = viewRequestsUseCase;
        this.viewTokenPurchasesUseCase = viewTokenPurchasesUseCase;
        this.viewUsageUseCase = viewUsageUseCase;
        this.viewTokenUsageTimeseriesUseCase = viewTokenUsageTimeseriesUseCase;
        this.viewMyClassTestQuotaAllocationUseCase = viewMyClassTestQuotaAllocationUseCase;
        this.viewMyPracticeQuotaAllocationUseCase = viewMyPracticeQuotaAllocationUseCase;
        this.viewInvoicesUseCase = viewInvoicesUseCase;
        this.viewFinancialEventsUseCase = viewFinancialEventsUseCase;
        this.viewSchoolDebtEventsUseCase = viewSchoolDebtEventsUseCase;
        this.quotaPricingPort = quotaPricingPort;
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

    @QueryMapping(name = "quotaPricing")
    public QuotaPricingDto quotaPricing() {
        return new QuotaPricingDto(
            quotaPricingPort.currentEstimatedCostPerExamSecondUsd(),
            quotaPricingPort.currentEstimatedCostPerPracticeSecondUsd(),
            quotaPricingPort.usdToVndRate()
        );
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

    @QueryMapping(name = "schoolTokenUsageTimeseries")
    public TokenUsageTimeseriesDto schoolTokenUsageTimeseries(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "dateFrom") String dateFrom,
            @Argument(name = "dateTo") String dateTo,
            @Argument(name = "granularity") TokenUsageGranularity granularity) {
        return viewTokenUsageTimeseriesUseCase.execute(new ViewTokenUsageTimeseriesQuery(
            schoolId,
            DateMapper.toInstant(dateFrom),
            DateMapper.toInstant(dateTo),
            granularity
        ));
    }

    @QueryMapping(name = "myClassTestQuotaAllocation")
    public MyClassTestQuotaAllocationDto myClassTestQuotaAllocation() {
        return viewMyClassTestQuotaAllocationUseCase.execute(null);
    }

    @QueryMapping(name = "myPracticeQuotaAllocation")
    public MyPracticeQuotaAllocationDto myPracticeQuotaAllocation() {
        return viewMyPracticeQuotaAllocationUseCase.execute(null);
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

    @QueryMapping(name = "schoolDebtEvents")
    public PageResult<SchoolDebtEventDto> schoolDebtEvents(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "page") int page,
            @Argument(name = "size") int size) {
        return viewSchoolDebtEventsUseCase.execute(new ViewSchoolDebtEventsQuery(schoolId, page, size));
    }

    @MutationMapping(name = "updateSubscriptionPlan")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
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
