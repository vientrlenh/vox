package com.sep.vox.infrastructure.persistence.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.financial.ExchangeRateSnapshotJpaEntity;

public interface SpringDataExchangeRateSnapshotRepository
        extends JpaRepository<ExchangeRateSnapshotJpaEntity, UUID> {
    /**
     * Additionally ordered by id DESC so the order is DEFINITIVE: two snapshots with the same currency_code can have the same
     * fetched_at (the job re-runs in the same tick, or a backfill writes in bulk), and then ordering by
     * fetched_at alone lets Postgres return either one -- the quota selling price flips between calls.
     * id is a uuidv7, so "id DESC" is "newest first", and since id is unique there are no ties left.
     */
    Optional<ExchangeRateSnapshotJpaEntity> findFirstByCurrencyCodeOrderByFetchedAtDescIdDesc(String currencyCode);
}
