package com.sep.vox.infrastructure.persistence.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.ExchangeRateSnapshotJpaEntity;

public interface SpringDataExchangeRateSnapshotRepository
        extends JpaRepository<ExchangeRateSnapshotJpaEntity, UUID> {
    /**
     * Sắp thêm theo id DESC để thứ tự CHỐT HẲN: hai snapshot cùng currency_code có thể trùng
     * fetched_at (job chạy lại trong cùng một tick, hoặc backfill ghi hàng loạt), khi đó chỉ sắp theo
     * fetched_at là Postgres trả về bản nào tùy ý -- giá bán quota nhảy qua lại giữa hai lần gọi.
     * id là uuidv7 nên "id DESC" chính là "mới nhất trước", và vì id duy nhất nên không còn hòa.
     */
    Optional<ExchangeRateSnapshotJpaEntity> findFirstByCurrencyCodeOrderByFetchedAtDescIdDesc(String currencyCode);
}
