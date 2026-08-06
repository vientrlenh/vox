package com.sep.vox.infrastructure.persistence.adapter;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.subscription.Invoice;
import com.sep.vox.domain.model.subscription.InvoiceStatus;
import com.sep.vox.domain.model.subscription.PaymentMethod;
import com.sep.vox.domain.repository.InvoiceRepository;
import com.sep.vox.infrastructure.persistence.mapper.InvoiceMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataInvoiceRepository;

@Repository
public class InvoiceRepositoryImpl implements InvoiceRepository {

    private final SpringDataInvoiceRepository springDataInvoiceRepository;

    public InvoiceRepositoryImpl(SpringDataInvoiceRepository springDataInvoiceRepository) {
        this.springDataInvoiceRepository = springDataInvoiceRepository;
    }

    @Override
    public Optional<Invoice> findById(UUID id) {
        return springDataInvoiceRepository.findById(id).map(InvoiceMapper::toDomain);
    }

    @Override
    public Optional<Invoice> findByIdForUpdate(UUID id) {
        return springDataInvoiceRepository.findWithLockById(id).map(InvoiceMapper::toDomain);
    }

    @Override
    public Invoice save(Invoice invoice) {
        var entity = InvoiceMapper.toJpa(invoice);
        var saved = springDataInvoiceRepository.save(entity);
        return InvoiceMapper.toDomain(saved);
    }

    @Override
    public List<Invoice> findAllBySubscriptionId(UUID subscriptionId) {
        return springDataInvoiceRepository.findAllBySubscriptionId(subscriptionId).stream()
            .map(InvoiceMapper::toDomain)
            .toList();
    }

    @Override
    public List<Invoice> findAllBySubscriptionIdIn(Collection<UUID> subscriptionIds) {
        return springDataInvoiceRepository.findAllBySubscriptionIdIn(subscriptionIds).stream()
            .map(InvoiceMapper::toDomain)
            .toList();
    }

    @Override
    public Optional<Invoice> findByPaymentProviderAndProviderOrderRef(PaymentMethod paymentProvider, String providerOrderRef) {
        return springDataInvoiceRepository
            .findByPaymentProviderAndProviderOrderRef(paymentProvider.name(), providerOrderRef)
            .map(InvoiceMapper::toDomain);
    }

    @Override
    public List<Invoice> findAllByStatus(InvoiceStatus status) {
        return springDataInvoiceRepository.findAllByStatus(status.name()).stream()
            .map(InvoiceMapper::toDomain)
            .toList();
    }

    @Override
    public BigDecimal sumAmountByStatus(InvoiceStatus status) {
        return springDataInvoiceRepository.sumAmountByStatus(status.name());
    }
}
