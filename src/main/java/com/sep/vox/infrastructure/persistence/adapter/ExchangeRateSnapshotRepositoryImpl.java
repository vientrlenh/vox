package com.sep.vox.infrastructure.persistence.adapter;

import com.sep.vox.infrastructure.persistence.entity.financial.ExchangeRateSnapshotJpaEntity;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.financial.CurrencyCode;
import com.sep.vox.domain.model.financial.ExchangeRateSnapshot;
import com.sep.vox.domain.repository.ExchangeRateSnapshotRepository;
import com.sep.vox.infrastructure.persistence.mapper.ExchangeRateSnapshotMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataExchangeRateSnapshotRepository;

@Repository
@RequiredArgsConstructor
public class ExchangeRateSnapshotRepositoryImpl implements ExchangeRateSnapshotRepository {

    private final SpringDataExchangeRateSnapshotRepository springDataExchangeRateSnapshotRepository;

    @Override
    public ExchangeRateSnapshot save(ExchangeRateSnapshot snapshot) {
        ExchangeRateSnapshotJpaEntity saved = springDataExchangeRateSnapshotRepository.save(ExchangeRateSnapshotMapper.toJpa(snapshot));
        return ExchangeRateSnapshotMapper.toDomain(saved);
    }

    @Override
    public Optional<ExchangeRateSnapshot> findLatest(CurrencyCode currencyCode) {
        return springDataExchangeRateSnapshotRepository
            .findFirstByCurrencyCodeOrderByFetchedAtDescIdDesc(currencyCode.name())
            .map(ExchangeRateSnapshotMapper::toDomain);
    }
}
