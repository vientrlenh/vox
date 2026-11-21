package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.payment.PaymentMethod;
import com.sep.vox.domain.model.payment.PaymentProvider;
import com.sep.vox.domain.model.payment.PaymentRecord;
import com.sep.vox.domain.model.payment.PaymentStatus;
import com.sep.vox.infrastructure.persistence.entity.payment.PaymentRecordJpaEntity;

public final class PaymentRecordMapper {

    private PaymentRecordMapper() {}

    public static PaymentRecord toDomain(PaymentRecordJpaEntity jpa) {
        return PaymentRecord.builder()
            .id(jpa.getId())
            .orderId(jpa.getOrderId())
            .amountVnd(jpa.getAmountVnd())
            .method(methodFromString(jpa.getMethod()))
            .provider(providerFromString(jpa.getProvider()))
            .status(statusFromString(jpa.getStatus()))
            .providerOrderRef(jpa.getProviderOrderRef())
            .checkoutUrl(jpa.getCheckoutUrl())
            .providerPayloadJson(jpa.getProviderPayloadJson())
            .paidAt(jpa.getPaidAt())
            .createdAt(jpa.getCreatedAt())
            .build();
    }

    public static PaymentRecordJpaEntity toJpa(PaymentRecord domain) {
        return PaymentRecordJpaEntity.builder()
            .id(domain.getId())
            .orderId(domain.getOrderId())
            .amountVnd(domain.getAmountVnd())
            .method(valueOf(domain.getMethod()))
            .provider(valueOf(domain.getProvider()))
            .status(valueOf(domain.getStatus()))
            .providerOrderRef(domain.getProviderOrderRef())
            .checkoutUrl(domain.getCheckoutUrl())
            .providerPayloadJson(domain.getProviderPayloadJson())
            .paidAt(domain.getPaidAt())
            .createdAt(domain.getCreatedAt())
            .build();
    }

    private static PaymentMethod methodFromString(String method) {
        if (method == null)
            return null;
        try {
            return PaymentMethod.valueOf(method);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Phương thức thanh toán khi chuyển đổi sang domain model không hợp lệ: " + method);
        }
    }

    private static PaymentProvider providerFromString(String provider) {
        if (provider == null)
            return null;
        try {
            return PaymentProvider.valueOf(provider);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Cổng thanh toán khi chuyển đổi sang domain model không hợp lệ: " + provider);
        }
    }

    private static PaymentStatus statusFromString(String status) {
        if (status == null)
            return null;
        try {
            return PaymentStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Trạng thái thanh toán khi chuyển đổi sang domain model không hợp lệ: " + status);
        }
    }

    private static String valueOf(PaymentMethod method) {
        return method == null ? null : method.name();
    }

    private static String valueOf(PaymentProvider provider) {
        return provider == null ? null : provider.name();
    }

    private static String valueOf(PaymentStatus status) {
        return status == null ? null : status.name();
    }
}
