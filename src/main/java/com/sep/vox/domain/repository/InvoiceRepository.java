package com.sep.vox.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.subscription.Invoice;
import com.sep.vox.domain.model.subscription.InvoiceStatus;
import com.sep.vox.domain.model.subscription.PaymentMethod;

public interface InvoiceRepository {
    Optional<Invoice> findById(UUID id);
    Optional<Invoice> findByIdForUpdate(UUID id);
    Invoice save(Invoice invoice);
    List<Invoice> findAllBySubscriptionId(UUID subscriptionId);
    List<Invoice> findAllBySubscriptionIdIn(Collection<UUID> subscriptionIds);
    // Mã đơn chỉ duy nhất trong phạm vi một cổng, nên phải tra theo cặp — hai cổng khác nhau
    // hoàn toàn có thể sinh ra cùng một chuỗi mã.
    Optional<Invoice> findByPaymentProviderAndProviderOrderRef(PaymentMethod paymentProvider, String providerOrderRef);
    List<Invoice> findAllByStatus(InvoiceStatus status);
}
