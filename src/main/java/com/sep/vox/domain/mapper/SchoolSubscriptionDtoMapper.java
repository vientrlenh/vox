package com.sep.vox.domain.mapper;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.SchoolSubscriptionDto;
import com.sep.vox.domain.model.subscription.SchoolSubscription;

public final class SchoolSubscriptionDtoMapper {

    private SchoolSubscriptionDtoMapper() {
    }

    public static SchoolSubscriptionDto toDto(SchoolSubscription domain) {
        return new SchoolSubscriptionDto(
            domain.getId(),
            domain.getSchoolId(),
            domain.getPlanId(),
            valueOf(domain.getStartDate()),
            valueOf(domain.getEndDate()),
            domain.getStatus().name(),
            domain.getPricePaidSnapshot(),
            valueOf(domain.getCancelledAt()),
            valueOf(domain.getCreatedAt())
        );
    }

    public static PageResult<SchoolSubscriptionDto> toDtoPage(PageResult<SchoolSubscription> page) {
        return new PageResult<>(
            toDtoList(page.content()),
            page.page(),
            page.size(),
            page.totalElements(),
            page.totalPages()
        );
    }

    public static List<SchoolSubscriptionDto> toDtoList(List<SchoolSubscription> domains) {
        return domains.stream()
            .map(SchoolSubscriptionDtoMapper::toDto)
            .toList();
    }

    private static String valueOf(LocalDate value) {
        return value == null ? null : value.toString();
    }

    private static String valueOf(OffsetDateTime value) {
        return value == null ? null : value.toString();
    }
}
