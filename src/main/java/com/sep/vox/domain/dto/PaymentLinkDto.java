package com.sep.vox.domain.dto;

import java.util.UUID;

public record PaymentLinkDto(
    UUID invoiceId,
    Long orderCode,
    String paymentLinkId,
    String checkoutUrl
) {
}