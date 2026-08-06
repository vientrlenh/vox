package com.sep.vox.infrastructure.persistence.repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.InvoiceJpaEntity;

import jakarta.persistence.LockModeType;

public interface SpringDataInvoiceRepository extends JpaRepository<InvoiceJpaEntity, UUID> {
    List<InvoiceJpaEntity> findAllBySubscriptionId(UUID subscriptionId);
    List<InvoiceJpaEntity> findAllBySubscriptionIdIn(Collection<UUID> subscriptionIds);
    Optional<InvoiceJpaEntity> findByPaymentProviderAndProviderOrderRef(String paymentProvider, String providerOrderRef);
    List<InvoiceJpaEntity> findAllByStatus(String status);

    @Query("SELECT COALESCE(SUM(i.amount), 0) FROM InvoiceJpaEntity i WHERE i.status = :status")
    BigDecimal sumAmountByStatus(@Param("status") String status);

    // PESSIMISTIC_WRITE: chặn các transaction settle() khác trên cùng invoice cho tới khi transaction
    // hiện tại commit, để tránh 2 lần "chốt" thanh toán chạy song song (vd: FE gọi sync-status 2 lần do
    // React StrictMode double-invoke effect, hoặc sync-status đua với webhook PayOS/reconciler job).
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<InvoiceJpaEntity> findWithLockById(UUID id);
}
