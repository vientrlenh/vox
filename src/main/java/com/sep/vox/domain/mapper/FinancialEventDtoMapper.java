package com.sep.vox.domain.mapper;

import java.time.OffsetDateTime;
import java.util.List;

import com.sep.vox.domain.dto.FinancialEventDto;
import com.sep.vox.domain.model.subscription.FinancialEvent;

public final class FinancialEventDtoMapper {

    private FinancialEventDtoMapper() {
    }

    public static FinancialEventDto toDto(FinancialEvent domain) {
        return new FinancialEventDto(
            domain.getId(),
            domain.getSchoolId(),
            domain.getSubscriptionId(),
            domain.getEventType().name(),
            domain.getAmountSigned(),
            domain.getCurrency(),
            domain.getActorId(),
            valueOf(domain.getOccurredAt())
        );
    }

    public static List<FinancialEventDto> toDtoList(List<FinancialEvent> domains) {
        return domains.stream().map(FinancialEventDtoMapper::toDto).toList();
    }

    private static String valueOf(OffsetDateTime value) {
        return value == null ? null : value.toString();
    }
}
