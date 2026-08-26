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
import com.sep.vox.application.port.input.query.ViewSubscriptionPlanDetailQuery;
import com.sep.vox.application.port.input.query.ViewRequestsQuery;
import com.sep.vox.application.port.input.query.ViewSchoolDebtEventsQuery;
import com.sep.vox.application.port.input.query.ViewSchoolSubscriptionQuotaRecordsQuery;
import com.sep.vox.application.port.input.query.ViewSchoolSubscriptionsQuery;
import com.sep.vox.application.port.input.query.ViewSubscriptionHistoryQuery;
import com.sep.vox.application.port.input.query.ViewSubscriptionPlansQuery;
import com.sep.vox.application.port.input.query.ViewTokenPurchasesQuery;
import com.sep.vox.application.port.input.query.ViewUsageQuery;
import com.sep.vox.application.port.input.usecase.subscription.UpdateSubscriptionPlanUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ViewCurrentSubscriptionUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ViewFinancialEventsUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ViewInvoicesUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ViewMyExamQuotaAllocationUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ViewMyPracticeQuotaAllocationUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ViewSubscriptionPlanDetailUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ViewSubscriptionPlansUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ViewRequestsUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ViewSchoolDebtEventsUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ViewSchoolSubscriptionQuotaRecordsUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ViewSchoolSubscriptionsUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ViewSubscriptionHistoryUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ViewTokenPurchasesUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ViewTokenUsageTimeseriesUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ViewUsageUseCase;
import com.sep.vox.application.port.output.QuotaPricingPort;
import com.sep.vox.application.response.input.subscription.ViewSubscriptionPlansResponse;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.FinancialEventDto;
import com.sep.vox.domain.dto.InvoiceDto;
import com.sep.vox.domain.dto.SchoolSubscriptionQuotaUserAllocationDto;
import com.sep.vox.domain.dto.QuotaPricingDto;
import com.sep.vox.domain.dto.SchoolDebtEventDto;
import com.sep.vox.domain.dto.SchoolSubscriptionDto;
import com.sep.vox.domain.dto.SubscriptionPlanDto;
import com.sep.vox.domain.dto.SubscriptionPlanQuotaDto;
import com.sep.vox.domain.dto.SchoolSubscriptionQuotaRecordDto;
import com.sep.vox.domain.dto.SubscriptionRequestDto;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionStatus;
import com.sep.vox.interfaces.graphql.dto.request.UpdateSubscriptionPlanInput;
import com.sep.vox.interfaces.graphql.mapper.UpdateSubscriptionPlanCommandMapper;

@Controller("graphqlSubscriptionController")
public class SubscriptionController {

    private final ViewSubscriptionPlansUseCase viewSubscriptionPlansUseCase;
    private final ViewSubscriptionPlanDetailUseCase viewSubscriptionPlanDetailUseCase;
    private final UpdateSubscriptionPlanUseCase updateSubscriptionPlanUseCase;
    private final ViewSchoolSubscriptionsUseCase viewSchoolSubscriptionsUseCase;
    private final ViewCurrentSubscriptionUseCase viewCurrentSubscriptionUseCase;
    private final ViewSubscriptionHistoryUseCase viewSubscriptionHistoryUseCase;
    private final ViewSchoolSubscriptionQuotaRecordsUseCase viewSchoolSubscriptionQuotaRecordsUseCase;
    private final ViewTokenUsageTimeseriesUseCase viewTokenUsageTimeseriesUseCase;
    private final ViewMyExamQuotaAllocationUseCase viewMyExamQuotaAllocationUseCase;
    private final ViewMyPracticeQuotaAllocationUseCase viewMyPracticeQuotaAllocationUseCase;
    private final ViewInvoicesUseCase viewInvoicesUseCase;
    private final ViewFinancialEventsUseCase viewFinancialEventsUseCase;
    private final ViewSchoolDebtEventsUseCase viewSchoolDebtEventsUseCase;
    private final QuotaPricingPort quotaPricingPort;

    public SubscriptionController(
            ViewSubscriptionPlansUseCase viewSubscriptionPlansUseCase,
            ViewSubscriptionPlanDetailUseCase viewSubscriptionPlanDetailUseCase,
            UpdateSubscriptionPlanUseCase updateSubscriptionPlanUseCase,
            ViewSchoolSubscriptionsUseCase viewSchoolSubscriptionsUseCase,
            ViewCurrentSubscriptionUseCase viewCurrentSubscriptionUseCase,
            ViewSubscriptionHistoryUseCase viewSubscriptionHistoryUseCase,
            ViewSchoolSubscriptionQuotaRecordsUseCase viewSchoolSubscriptionQuotaRecordsUseCase,
            ViewTokenUsageTimeseriesUseCase viewTokenUsageTimeseriesUseCase,
            ViewMyExamQuotaAllocationUseCase viewMyExamQuotaAllocationUseCase,
            ViewMyPracticeQuotaAllocationUseCase viewMyPracticeQuotaAllocationUseCase,
            ViewInvoicesUseCase viewInvoicesUseCase,
            ViewFinancialEventsUseCase viewFinancialEventsUseCase,
            ViewSchoolDebtEventsUseCase viewSchoolDebtEventsUseCase,
            QuotaPricingPort quotaPricingPort) {
        this.viewSubscriptionPlansUseCase = viewSubscriptionPlansUseCase;
        this.viewSubscriptionPlanDetailUseCase = viewSubscriptionPlanDetailUseCase;
        this.updateSubscriptionPlanUseCase = updateSubscriptionPlanUseCase;
        this.viewSchoolSubscriptionsUseCase = viewSchoolSubscriptionsUseCase;
        this.viewCurrentSubscriptionUseCase = viewCurrentSubscriptionUseCase;
        this.viewSubscriptionHistoryUseCase = viewSubscriptionHistoryUseCase;
        this.viewSchoolSubscriptionQuotaRecordsUseCase = viewSchoolSubscriptionQuotaRecordsUseCase;
        this.viewTokenUsageTimeseriesUseCase = viewTokenUsageTimeseriesUseCase;
        this.viewMyExamQuotaAllocationUseCase = viewMyExamQuotaAllocationUseCase;
        this.viewMyPracticeQuotaAllocationUseCase = viewMyPracticeQuotaAllocationUseCase;
        this.viewInvoicesUseCase = viewInvoicesUseCase;
        this.viewFinancialEventsUseCase = viewFinancialEventsUseCase;
        this.viewSchoolDebtEventsUseCase = viewSchoolDebtEventsUseCase;
        this.quotaPricingPort = quotaPricingPort;
    }

    @QueryMapping(name = "subscriptionPlans")
    public PageResult<ViewSubscriptionPlansResponse> subscriptionPlans(
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {
        validatePageSize(page, size);
        return viewSubscriptionPlansUseCase.execute(new ViewSubscriptionPlansQuery(page, size));
    }

    @QueryMapping(name = "subscriptionPlan")
    public SubscriptionPlanDto subscriptionPlan(@Argument(name = "id") UUID id) {
        return viewSubscriptionPlanDetailUseCase.execute(new ViewSubscriptionPlanDetailQuery(id));
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
            @Argument(name = "status") SchoolSubscriptionStatus status,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {
        validatePageSize(page, size);
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

    @QueryMapping(name = "subscriptionUsage")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public List<SchoolSubscriptionQuotaRecordDto> subscriptionUsage(@Argument(name = "schoolId") UUID schoolId) {
        return viewSchoolSubscriptionQuotaRecordsUseCase.execute(new ViewSchoolSubscriptionQuotaRecordsQuery(schoolId));
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

    @QueryMapping(name = "myExamQuotaAllocation")
    public SchoolSubscriptionQuotaUserAllocationDto myExamQuotaAllocation() {
        return viewMyExamQuotaAllocationUseCase.execute(null);
    }

    @QueryMapping(name = "myPracticeQuotaAllocation")
    public SchoolSubscriptionQuotaUserAllocationDto myPracticeQuotaAllocation() {
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
    public UUID updateSubscriptionPlan(
            @Argument(name = "id") UUID id,
            @Argument(name = "input") UpdateSubscriptionPlanInput input) {
        return updateSubscriptionPlanUseCase.execute(UpdateSubscriptionPlanCommandMapper.fromInput(id, input));
    }

    @SchemaMapping(typeName = "SchoolSubscription", field = "plan")
    public CompletableFuture<SubscriptionPlanDto> plan(SchoolSubscriptionDto source, DataFetchingEnvironment env) {
        DataLoader<UUID, SubscriptionPlanDto> loader = env.getDataLoader("subscriptionPlanById");
        return loader.load(source.planId());
    }

    @SchemaMapping(typeName = "SubscriptionPlan", field = "quotas")
    public CompletableFuture<List<SubscriptionPlanQuotaDto>> subscriptionPlanQuotas(SubscriptionPlanDto plan, DataFetchingEnvironment env) {
        DataLoader<UUID, List<SubscriptionPlanQuotaDto>> loader = env.getDataLoader("quotasBySubscriptionPlanId");
        if (loader == null) 
            throw new IllegalStateException("Không tìm thấy data loader quotasBySubscriptionPlanId");
        return loader.load(plan.id());
    }


    private void validatePageSize(Integer page, Integer size) {
        if (page == null || size == null || page <= 0 || size <= 0) {
            throw new IllegalArgumentException("Trang hoặc kích thước yêu cầu phải lớn hơn 0");
        }
    }
}
