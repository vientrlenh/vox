package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.invoice.Invoice;
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
    public Invoice save(Invoice invoice) {
        var entity = InvoiceMapper.toJpa(invoice);
        var saved = springDataInvoiceRepository.save(entity);
        return InvoiceMapper.toDomain(saved);
    }

    @Override
    public Optional<Invoice> findByOrderId(UUID orderId) {
        return springDataInvoiceRepository.findByOrderId(orderId).map(InvoiceMapper::toDomain);
    }

    @Override
    public List<Invoice> findByOrderIdIn(Collection<UUID> orderIds) {
        return springDataInvoiceRepository.findByOrderIdIn(orderIds).stream()
            .map(InvoiceMapper::toDomain)
            .toList();
    }

    @Override
    public Optional<Invoice> findByInvoiceNumber(String invoiceNumber) {
        return springDataInvoiceRepository.findByInvoiceNumber(invoiceNumber).map(InvoiceMapper::toDomain);
    }

    @Override
    public boolean existsByOrderId(UUID orderId) {
        return springDataInvoiceRepository.existsByOrderId(orderId);
    }
}
