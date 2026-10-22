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
import lombok.RequiredArgsConstructor;

import com.sep.vox.application.port.input.query.ViewSubscriptionPlanDetailQuery;
import com.sep.vox.application.port.input.query.ViewSchoolSubscriptionHistoryQuery;
import com.sep.vox.application.port.input.query.ViewSchoolSubscriptionQuery;
import com.sep.vox.application.port.input.query.ViewSchoolSubscriptionQuotaRecordsQuery;
import com.sep.vox.application.port.input.query.ViewSchoolSubscriptionsQuery;
import com.sep.vox.application.port.input.query.ViewSubscriptionPlansQuery;
import com.sep.vox.application.port.input.usecase.subscription.UpdateSubscriptionPlanUseCase;
import com.sep.vox.application.port.input.usecase.subscription.PreviewSchoolSubscriptionRenewalUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ViewMyAssessmentQuotaAllocationUseCase;
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
import com.sep.vox.application.shared.AppPageRequest;
import com.sep.vox.domain.dto.SchoolSubscriptionQuotaUserAllocationDto;
import com.sep.vox.domain.dto.SchoolDto;
import com.sep.vox.domain.dto.SchoolSubscriptionDto;
import com.sep.vox.domain.dto.SubscriptionPlanDto;
import com.sep.vox.domain.dto.SubscriptionPlanQuotaDto;
import com.sep.vox.domain.dto.SchoolSubscriptionQuotaRecordDto;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionStatus;
import com.sep.vox.domain.shared.PageResult;
import com.sep.vox.interfaces.graphql.dto.request.UpdateSubscriptionPlanInput;
import com.sep.vox.application.port.input.query.ViewQuotaUserAllocationsQuery;
import com.sep.vox.application.port.input.usecase.subscription.ViewAssessmentQuotaUserAllocationsUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ViewPracticeQuotaUserAllocationsUseCase;
import com.sep.vox.application.response.input.subscription.QuotaUserAllocationPageResponse;
import com.sep.vox.domain.dto.UserDto;

@Controller
@RequiredArgsConstructor
public class GraphQLSubscriptionController {

    private final ViewSubscriptionPlansUseCase viewSubscriptionPlansUseCase;
    private final ViewSubscriptionPlanDetailUseCase viewSubscriptionPlanDetailUseCase;
    private final UpdateSubscriptionPlanUseCase updateSubscriptionPlanUseCase;
    private final ViewSchoolSubscriptionsUseCase viewSchoolSubscriptionsUseCase;
    private final ViewSchoolSubscriptionQuotaRecordsUseCase viewSchoolSubscriptionQuotaRecordsUseCase;
    private final ViewMyAssessmentQuotaAllocationUseCase viewMyAssessmentQuotaAllocationUseCase;
    private final PreviewSchoolSubscriptionRenewalUseCase previewSchoolSubscriptionRenewalUseCase;
    private final ViewMyPracticeQuotaAllocationUseCase viewMyPracticeQuotaAllocationUseCase;
    private final ViewSchoolSubscriptionUseCase viewSchoolSubscriptionUseCase;
    private final ViewSchoolSubscriptionHistoryUseCase viewSchoolSubscriptionHistoryUseCase;
    private final ViewAssessmentQuotaUserAllocationsUseCase viewAssessmentQuotaUserAllocationsUseCase;
    private final ViewPracticeQuotaUserAllocationsUseCase viewPracticeQuotaUserAllocationsUseCase;
    private final QuotaPricingPort quotaPricingPort;

    @QueryMapping(name = "subscriptionPlans")
    public PageResult<ViewSubscriptionPlansResponse> subscriptionPlans(
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {
        AppPageRequest pageRequest = AppPageRequest.pageRequest(page, size);
        return viewSubscriptionPlansUseCase.execute(new ViewSubscriptionPlansQuery(pageRequest));
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
        AppPageRequest pageRequest = AppPageRequest.pageRequest(page, size);
        return viewSchoolSubscriptionsUseCase.execute(new ViewSchoolSubscriptionsQuery(keyword, subscriptionPlanId, status, pageRequest));
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
            quotaPricingPort.currentEstimatedCostPerAssessmentSecondUsd(),
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

    @QueryMapping(name = "myAssessmentQuotaAllocation")
    public SchoolSubscriptionQuotaUserAllocationDto myAssessmentQuotaAllocation() {
        return viewMyAssessmentQuotaAllocationUseCase.execute(null);
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
        return updateSubscriptionPlanUseCase.execute(UpdateSubscriptionPlanInput.toCommand(id, input));
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

    @SchemaMapping(typeName = "SchoolSubscription", field = "school")
    public CompletableFuture<SchoolDto> schoolFromSchoolSubscription(SchoolSubscriptionDto dto, DataFetchingEnvironment env) {
        DataLoader<UUID, SchoolDto> loader = env.getDataLoader("schoolBySchoolSubscription");
        if (loader == null) {
            throw new IllegalStateException("Không tìm thấy dataloader schoolBySchoolSubscription");
        }
        return loader.load(dto.schoolId());
    }

    @QueryMapping(name = "schoolAssessmentQuotaUserAllocations")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public QuotaUserAllocationPageResponse schoolAssessmentQuotaUserAllocations(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "search") String search,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {
        AppPageRequest pageRequest = AppPageRequest.pageRequest(page, size);
        return viewAssessmentQuotaUserAllocationsUseCase.execute(
            new ViewQuotaUserAllocationsQuery(schoolId, search, pageRequest));
    }

    @QueryMapping(name = "schoolPracticeQuotaUserAllocations")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public QuotaUserAllocationPageResponse schoolPracticeQuotaUserAllocations(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "search") String search,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {
        AppPageRequest pageRequest = AppPageRequest.pageRequest(page, size);
        return viewPracticeQuotaUserAllocationsUseCase.execute(
            new ViewQuotaUserAllocationsQuery(schoolId, search, pageRequest));
    }

    /**
     * Nối tên người dùng vào từng dòng phân bổ.
     *
     * <p>Qua data loader nên cả trang gom lại thành ĐÚNG MỘT truy vấn users, không phải một truy vấn
     * mỗi dòng. Đây là chỗ sửa lỗi cột "Họ tên" luôn hiện dấu gạch: đường REST cũ trả về dòng phân bổ
     * không kèm tên, còn client thì khai sẵn một trường fullName không bao giờ được điền.
     */
    @SchemaMapping(typeName = "SchoolQuotaUserAllocation", field = "user")
    public CompletableFuture<UserDto> allocationUser(
            QuotaUserAllocationPageResponse.Row row, DataFetchingEnvironment env) {
        DataLoader<UUID, UserDto> loader = env.getDataLoader("userById");
        return loader.load(row.userId());
    }
}
