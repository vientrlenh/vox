package com.sep.vox.domain.mapper;

import java.time.OffsetDateTime;
import java.util.List;

import com.sep.vox.domain.dto.TokenPurchaseDto;
import com.sep.vox.domain.dto.TokenPurchaseItemDto;
import com.sep.vox.domain.model.subscription.TokenPurchase;
import com.sep.vox.domain.model.subscription.TokenPurchaseItem;

public final class TokenPurchaseDtoMapper {

    private TokenPurchaseDtoMapper() {
    }

    public static TokenPurchaseDto toDto(TokenPurchase domain, List<TokenPurchaseItem> items) {
        return new TokenPurchaseDto(
            domain.getId(),
            domain.getSubscriptionId(),
            domain.getTotalAmount(),
            domain.getStatus().name(),
            valueOf(domain.getPurchasedAt()),
            toItemDtoList(items)
        );
    }

    public static TokenPurchaseItemDto toItemDto(TokenPurchaseItem domain) {
        return new TokenPurchaseItemDto(
            domain.getId(),
            domain.getPurchaseId(),
            domain.getQuotaType().name(),
            domain.getQuantity(),
            domain.getUnitPriceSnapshot(),
            domain.getSubtotal()
        );
    }

    public static List<TokenPurchaseItemDto> toItemDtoList(List<TokenPurchaseItem> domains) {
        return domains.stream().map(TokenPurchaseDtoMapper::toItemDto).toList();
    }

    private static String valueOf(OffsetDateTime value) {
        return value == null ? null : value.toString();
    }
}
