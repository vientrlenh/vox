package com.sep.vox.domain.mapper;

import java.time.Instant;
import java.util.List;

import com.sep.vox.domain.dto.PlanQuotaDto;
import com.sep.vox.domain.dto.SubscriptionPlanDto;
import com.sep.vox.domain.model.subscription.PlanQuota;
import com.sep.vox.domain.model.subscription.SubscriptionPlan;

public final class SubscriptionPlanDtoMapper {

    private SubscriptionPlanDtoMapper() {
    }

    /**
     * hasActiveSubscribers luôn false ở overload này — dùng cho các use case trả về DUY NHẤT một
     * plan (create/archive/preview/detail) mà không phải chỗ FE dựa vào field này để ẩn nút Sửa.
     * Chỗ cần giá trị thật (danh sách gói cho system admin) phải gọi overload có tham số bên dưới.
     */
    public static SubscriptionPlanDto toDto(SubscriptionPlan domain, List<PlanQuota> quotas) {
        return toDto(domain, quotas, false);
    }

    public static SubscriptionPlanDto toDto(SubscriptionPlan domain, List<PlanQuota> quotas, boolean hasActiveSubscribers) {
        return new SubscriptionPlanDto(
            domain.getId(),
            domain.getName(),
            domain.getTagline(),
            domain.getPricePerYear(),
            domain.getValidityDays(),
            domain.getMaxTimePerAttemptMin(),
            domain.getMaxStudentCount(),
            domain.isPopular(),
            domain.getStatus().name(),
            domain.getVersion(),
            valueOf(domain.getCreatedAt()),
            domain.getCreatedBy(),
            domain.getReplacedByPlanId(),
            hasActiveSubscribers,
            toQuotaDtoList(quotas)
        );
    }

    public static PlanQuotaDto toQuotaDto(PlanQuota quota) {
        return new PlanQuotaDto(
            quota.getId(),
            quota.getQuotaType().name(),
            quota.getIncludedQuantity(),
            quota.getTokenUnitPrice()
        );
    }

    public static List<PlanQuotaDto> toQuotaDtoList(List<PlanQuota> quotas) {
        return quotas.stream()
            .map(SubscriptionPlanDtoMapper::toQuotaDto)
            .toList();
    }

    private static String valueOf(Instant value) {
        return value == null ? null : value.toString();
    }
}
