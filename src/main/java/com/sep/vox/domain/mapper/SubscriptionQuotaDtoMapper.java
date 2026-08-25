package com.sep.vox.domain.mapper;

import java.util.List;

import com.sep.vox.domain.dto.SubscriptionQuotaDto;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionQuotaRecord;

public final class SubscriptionQuotaDtoMapper {

    private SubscriptionQuotaDtoMapper() {
    }

    public static SubscriptionQuotaDto toDto(SchoolSubscriptionQuotaRecord domain) {
        return new SubscriptionQuotaDto(
            domain.getId(),
            domain.getSubscriptionId(),
            domain.getQuotaType().name(),
            domain.getTotalAllocated(),
            domain.getUsedQuantity(),
            domain.getUsedQuantity().compareTo(domain.getTotalAllocated()) > 0
        );
    }

    public static List<SubscriptionQuotaDto> toDtoList(List<SchoolSubscriptionQuotaRecord> domains) {
        return domains.stream().map(SubscriptionQuotaDtoMapper::toDto).toList();
    }
}
