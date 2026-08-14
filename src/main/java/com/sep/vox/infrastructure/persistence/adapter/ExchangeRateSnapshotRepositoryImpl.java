package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.subscription.ExchangeRateSnapshot;
import com.sep.vox.domain.repository.ExchangeRateSnapshotRepository;
import com.sep.vox.infrastructure.persistence.mapper.ExchangeRateSnapshotMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataExchangeRateSnapshotRepository;

@Repository
public class ExchangeRateSnapshotRepositoryImpl implements ExchangeRateSnapshotRepository {

    private final SpringDataExchangeRateSnapshotRepository springDataExchangeRateSnapshotRepository;

    public ExchangeRateSnapshotRepositoryImpl(
            SpringDataExchangeRateSnapshotRepository springDataExchangeRateSnapshotRepository) {
        this.springDataExchangeRateSnapshotRepository = springDataExchangeRateSnapshotRepository;
    }

    @Override
    public ExchangeRateSnapshot save(ExchangeRateSnapshot snapshot) {
        var saved = springDataExchangeRateSnapshotRepository.save(ExchangeRateSnapshotMapper.toJpa(snapshot));
        return ExchangeRateSnapshotMapper.toDomain(saved);
    }

    @Override
    public Optional<ExchangeRateSnapshot> findLatest() {
        return springDataExchangeRateSnapshotRepository.findFirstByOrderByFetchedAtDesc()
            .map(ExchangeRateSnapshotMapper::toDomain);
    }
}
