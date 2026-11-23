package com.sep.vox.infrastructure.persistence.adapter;

import com.sep.vox.infrastructure.persistence.entity.payment.PaymentRecordJpaEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.payment.PaymentProvider;
import com.sep.vox.domain.model.payment.PaymentRecord;
import com.sep.vox.domain.model.payment.PaymentStatus;
import com.sep.vox.domain.repository.PaymentRecordRepository;
import com.sep.vox.infrastructure.persistence.mapper.PaymentRecordMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataPaymentRecordRepository;

@Repository
@RequiredArgsConstructor
public class PaymentRecordRepositoryImpl implements PaymentRecordRepository {

    private final SpringDataPaymentRecordRepository springDataPaymentRecordRepository;

    @Override
    public Optional<PaymentRecord> findById(UUID id) {
        return springDataPaymentRecordRepository.findById(id).map(PaymentRecordMapper::toDomain);
    }

    @Override
    public PaymentRecord save(PaymentRecord paymentRecord) {
        PaymentRecordJpaEntity entity = PaymentRecordMapper.toJpa(paymentRecord);
        PaymentRecordJpaEntity saved = springDataPaymentRecordRepository.save(entity);
        return PaymentRecordMapper.toDomain(saved);
    }

    @Override
    public List<PaymentRecord> findByOrderId(UUID orderId) {
        return springDataPaymentRecordRepository.findByOrderId(orderId).stream()
            .map(PaymentRecordMapper::toDomain)
            .toList();
    }

    @Override
    public List<PaymentRecord> findByOrderIdIn(Collection<UUID> orderIds) {
        return springDataPaymentRecordRepository.findByOrderIdInOrderByIdDesc(orderIds).stream()
            .map(PaymentRecordMapper::toDomain)
            .toList();
    }

    @Override
    public Optional<PaymentRecord> findByProviderAndProviderOrderRef(PaymentProvider provider, String providerOrderRef) {
        return springDataPaymentRecordRepository
            .findByProviderAndProviderOrderRef(provider.name(), providerOrderRef)
            .map(PaymentRecordMapper::toDomain);
    }

    @Override
    public Optional<PaymentRecord> findPendingByOrderId(UUID orderId) {
        return springDataPaymentRecordRepository
            .findByOrderIdAndStatus(orderId, PaymentStatus.PENDING.name())
            .map(PaymentRecordMapper::toDomain);
    }

    @Override
    public List<PaymentRecord> findByStatus(PaymentStatus status) {
        return springDataPaymentRecordRepository.findByStatus(status.name()).stream()
            .map(PaymentRecordMapper::toDomain)
            .toList();
    }

    @Override
    public long countByOrderId(UUID orderId) {
        return springDataPaymentRecordRepository.countByOrderId(orderId);
    }
}
