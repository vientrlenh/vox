package com.sep.vox.domain.repository;

import java.util.Optional;

import com.sep.vox.domain.model.subscription.ExchangeRateSnapshot;

public interface ExchangeRateSnapshotRepository {
    ExchangeRateSnapshot save(ExchangeRateSnapshot snapshot);

    /** Lần fetch gần nhất đã thành công (giá trị hợp lệ) -- rỗng nếu chưa có lần nào. */
    Optional<ExchangeRateSnapshot> findLatest();
}
