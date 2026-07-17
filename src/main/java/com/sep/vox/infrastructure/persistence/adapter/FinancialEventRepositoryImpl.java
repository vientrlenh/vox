package com.sep.vox.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.subscription.FinancialEvent;
import com.sep.vox.domain.repository.FinancialEventRepository;
import com.sep.vox.infrastructure.persistence.mapper.FinancialEventMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataFinancialEventRepository;

@Repository
public class FinancialEventRepositoryImpl implements FinancialEventRepository {

    private final SpringDataFinancialEventRepository springDataFinancialEventRepository;

    public FinancialEventRepositoryImpl(SpringDataFinancialEventRepository springDataFinancialEventRepository) {
        this.springDataFinancialEventRepository = springDataFinancialEventRepository;
    }

    @Override
    public Optional<FinancialEvent> findById(UUID id) {
        return springDataFinancialEventRepository.findById(id).map(FinancialEventMapper::toDomain);
    }

    @Override
    public FinancialEvent save(FinancialEvent event) {
        var entity = FinancialEventMapper.toJpa(event);
        var saved = springDataFinancialEventRepository.save(entity);
        return FinancialEventMapper.toDomain(saved);
    }

    @Override
    public List<FinancialEvent> findAllBySchoolId(UUID schoolId) {
        return springDataFinancialEventRepository.findAllBySchoolId(schoolId).stream()
            .map(FinancialEventMapper::toDomain)
            .toList();
    }
}
