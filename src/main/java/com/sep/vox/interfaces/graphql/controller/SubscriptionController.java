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

import com.sep.vox.application.port.input.query.ViewSubscriptionPlanDetailQuery;
import com.sep.vox.application.port.input.query.ViewSchoolSubscriptionHistoryQuery;
import com.sep.vox.application.port.input.query.ViewSchoolSubscriptionQuery;
import com.sep.vox.application.port.input.query.ViewSchoolSubscriptionQuotaRecordsQuery;
import com.sep.vox.application.port.input.query.ViewSchoolSubscriptionsQuery;
import com.sep.vox.application.port.input.query.ViewSubscriptionPlansQuery;
import com.sep.vox.application.port.input.usecase.subscription.UpdateSubscriptionPlanUseCase;
import com.sep.vox.application.port.input.usecase.subscription.PreviewSchoolSubscriptionRenewalUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ViewMyExamQuotaAllocationUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ViewMyPracticeQuotaAllocationUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ViewSubscriptionPlanDetailUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ViewSubscriptionPlansUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ViewSchoolSubscriptionQuotaRecordsUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ViewSchoolSubscriptionHistoryUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ViewSchoolSubscriptionUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ViewSchoolSubscriptionsUseCase;
import com.sep.vox.application.port.output.QuotaPricingPort;
import com.sep.vox.application.response.input.subscription.QuotaPricingResponse;
import com.sep.vox.application.response.input.subscription.SchoolSubscriptionRenewalPreviewResponse;
import com.sep.vox.application.response.input.subscription.ViewSubscriptionPlansResponse;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.SchoolSubscriptionQuotaUserAllocationDto;
import com.sep.vox.domain.dto.SchoolSubscriptionDto;
import com.sep.vox.domain.dto.SubscriptionPlanDto;
import com.sep.vox.domain.dto.SubscriptionPlanQuotaDto;
import com.sep.vox.domain.dto.SchoolSubscriptionQuotaRecordDto;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionStatus;
import com.sep.vox.interfaces.graphql.dto.request.UpdateSubscriptionPlanInput;
import com.sep.vox.interfaces.graphql.mapper.UpdateSubscriptionPlanCommandMapper;

@Controller("graphqlSubscriptionController")
public class SubscriptionController {

    private final ViewSubscriptionPlansUseCase viewSubscriptionPlansUseCase;
    private final ViewSubscriptionPlanDetailUseCase viewSubscriptionPlanDetailUseCase;
    private final UpdateSubscriptionPlanUseCase updateSubscriptionPlanUseCase;
    private final ViewSchoolSubscriptionsUseCase viewSchoolSubscriptionsUseCase;
    private final ViewSchoolSubscriptionQuotaRecordsUseCase viewSchoolSubscriptionQuotaRecordsUseCase;
    private final ViewMyExamQuotaAllocationUseCase viewMyExamQuotaAllocationUseCase;
    private final PreviewSchoolSubscriptionRenewalUseCase previewSchoolSubscriptionRenewalUseCase;
    private final ViewMyPracticeQuotaAllocationUseCase viewMyPracticeQuotaAllocationUseCase;
    private final ViewSchoolSubscriptionUseCase viewSchoolSubscriptionUseCase;
    private final ViewSchoolSubscriptionHistoryUseCase viewSchoolSubscriptionHistoryUseCase;
    private final QuotaPricingPort quotaPricingPort;

    public SubscriptionController(
            ViewSubscriptionPlansUseCase viewSubscriptionPlansUseCase,
            ViewSubscriptionPlanDetailUseCase viewSubscriptionPlanDetailUseCase,
            UpdateSubscriptionPlanUseCase updateSubscriptionPlanUseCase,
            ViewSchoolSubscriptionsUseCase viewSchoolSubscriptionsUseCase,
            ViewSchoolSubscriptionQuotaRecordsUseCase viewSchoolSubscriptionQuotaRecordsUseCase,
            ViewMyExamQuotaAllocationUseCase viewMyExamQuotaAllocationUseCase,
            PreviewSchoolSubscriptionRenewalUseCase previewSchoolSubscriptionRenewalUseCase,
            ViewMyPracticeQuotaAllocationUseCase viewMyPracticeQuotaAllocationUseCase,
            ViewSchoolSubscriptionUseCase viewSchoolSubscriptionUseCase,
            ViewSchoolSubscriptionHistoryUseCase viewSchoolSubscriptionHistoryUseCase,
            QuotaPricingPort quotaPricingPort) {
        this.viewSubscriptionPlansUseCase = viewSubscriptionPlansUseCase;
        this.viewSubscriptionPlanDetailUseCase = viewSubscriptionPlanDetailUseCase;
        this.updateSubscriptionPlanUseCase = updateSubscriptionPlanUseCase;
        this.viewSchoolSubscriptionsUseCase = viewSchoolSubscriptionsUseCase;
        this.viewSchoolSubscriptionQuotaRecordsUseCase = viewSchoolSubscriptionQuotaRecordsUseCase;
        this.viewMyExamQuotaAllocationUseCase = viewMyExamQuotaAllocationUseCase;
        this.previewSchoolSubscriptionRenewalUseCase = previewSchoolSubscriptionRenewalUseCase;
        this.viewMyPracticeQuotaAllocationUseCase = viewMyPracticeQuotaAllocationUseCase;
        this.viewSchoolSubscriptionUseCase = viewSchoolSubscriptionUseCase;
        this.viewSchoolSubscriptionHistoryUseCase = viewSchoolSubscriptionHistoryUseCase;
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

    @QueryMapping(name = "schoolSubscriptions")
    public PageResult<SchoolSubscriptionDto> schoolSubscriptions(
            @Argument(name = "keyword") String keyword,
            @Argument(name = "subscriptionPlanId") UUID subscriptionPlanId,
            @Argument(name = "status") SchoolSubscriptionStatus status,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {
        validatePageSize(page, size);
        return viewSchoolSubscriptionsUseCase.execute(new ViewSchoolSubscriptionsQuery(keyword, subscriptionPlanId, status, page, size));
    } 

    @QueryMapping(name = "schoolSubscription")
    public SchoolSubscriptionDto schoolSubscription(@Argument(name = "schoolId") UUID schoolId) {
        return viewSchoolSubscriptionUseCase.execute(new ViewSchoolSubscriptionQuery(schoolId));
    }

    @QueryMapping(name = "schoolSubscriptionHistory")
    public List<SchoolSubscriptionDto> schoolSubscriptionHistory(@Argument(name = "schoolId") UUID schoolId) {
        return viewSchoolSubscriptionHistoryUseCase.execute(new ViewSchoolSubscriptionHistoryQuery(schoolId));
    }

    /**
     * Giá vốn hiện hành, đọc thẳng từ cổng pricing -- không có use case vì không có quyết định nghiệp
     * vụ nào ở giữa, chỉ là ba giá trị cấu hình mà QuotaPricingPort đã tự lo fallback.
     */
    @QueryMapping(name = "quotaPricing")
    public QuotaPricingResponse quotaPricing() {
        return new QuotaPricingResponse(
            quotaPricingPort.currentEstimatedCostPerExamSecondUsd(),
            quotaPricingPort.currentEstimatedCostPerPracticeSecondUsd(),
            quotaPricingPort.usdToVndRate()
        );
    }

    @QueryMapping(name = "subscriptionUsage")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public List<SchoolSubscriptionQuotaRecordDto> subscriptionUsage(@Argument(name = "schoolId") UUID schoolId) {
        return viewSchoolSubscriptionQuotaRecordsUseCase.execute(new ViewSchoolSubscriptionQuotaRecordsQuery(schoolId));
    }

    @QueryMapping(name = "schoolSubscriptionRenewalPreview")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public SchoolSubscriptionRenewalPreviewResponse schoolSubscriptionRenewalPreview() {
        return previewSchoolSubscriptionRenewalUseCase.execute(null);
    }

    @QueryMapping(name = "myExamQuotaAllocation")
    public SchoolSubscriptionQuotaUserAllocationDto myExamQuotaAllocation() {
        return viewMyExamQuotaAllocationUseCase.execute(null);
    }

    @QueryMapping(name = "myPracticeQuotaAllocation")
    public SchoolSubscriptionQuotaUserAllocationDto myPracticeQuotaAllocation() {
        return viewMyPracticeQuotaAllocationUseCase.execute(null);
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
        return loader.load(source.subscriptionPlanId());
    }

    @SchemaMapping(typeName = "SubscriptionPlan", field = "quotas")
    public CompletableFuture<List<SubscriptionPlanQuotaDto>> subscriptionPlanQuotas(SubscriptionPlanDto plan, DataFetchingEnvironment env) {
        DataLoader<UUID, List<SubscriptionPlanQuotaDto>> loader = env.getDataLoader("quotasBySubscriptionPlanId");
        if (loader == null) 
            throw new IllegalStateException("Không tìm thấy data loader quotasBySubscriptionPlanId");
        return loader.load(plan.id());
    }

    // page ĐẾM TỪ 1 theo quy ước chung của dự án -- các repository adapter trừ 1 trước khi xuống
    // PageRequest (xem OrderRepositoryImpl/SubscriptionPlanRepositoryImpl). Guard này vốn đã đúng;
    // thứ sai là schema từng khai mặc định `page: Int = 0`, nên client KHÔNG truyền page -- đường đi
    // mặc định -- nhận số 0 rồi bị chính guard này từ chối.
    private void validatePageSize(Integer page, Integer size) {
        if (page == null || size == null || page <= 0 || size <= 0) {
            throw new IllegalArgumentException("Trang hoặc kích thước yêu cầu phải lớn hơn 0");
        }
    }
}
