package com.sep.vox.domain.mapper;

import java.time.OffsetDateTime;
import java.util.List;

import com.sep.vox.domain.dto.PlanQuotaDto;
import com.sep.vox.domain.dto.SubscriptionPlanDto;
import com.sep.vox.domain.model.subscription.PlanQuota;
import com.sep.vox.domain.model.subscription.SubscriptionPlan;

public final class SubscriptionPlanDtoMapper {

    private SubscriptionPlanDtoMapper() {
    }

    public static SubscriptionPlanDto toDto(SubscriptionPlan domain, List<PlanQuota> quotas) {
        return new SubscriptionPlanDto(
            domain.getId(),
            domain.getName(),
            domain.getTagline(),
            domain.getPricePerYear(),
            domain.getValidityDays(),
            domain.getMaxTimePerAttemptMin(),
            domain.isPopular(),
            domain.getStatus().name(),
            domain.getVersion(),
            valueOf(domain.getCreatedAt()),
            domain.getCreatedBy(),
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

    private static String valueOf(OffsetDateTime value) {
        return value == null ? null : value.toString();
    }
}
