package com.sep.vox.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.subscription.PlanQuota;
import com.sep.vox.domain.repository.PlanQuotaRepository;
import com.sep.vox.infrastructure.persistence.mapper.PlanQuotaMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataPlanQuotaRepository;

@Repository
public class PlanQuotaRepositoryImpl implements PlanQuotaRepository {

    private final SpringDataPlanQuotaRepository springDataPlanQuotaRepository;

    public PlanQuotaRepositoryImpl(SpringDataPlanQuotaRepository springDataPlanQuotaRepository) {
        this.springDataPlanQuotaRepository = springDataPlanQuotaRepository;
    }

    @Override
    public Optional<PlanQuota> findById(UUID id) {
        return springDataPlanQuotaRepository.findById(id).map(PlanQuotaMapper::toDomain);
    }

    @Override
    public PlanQuota save(PlanQuota planQuota) {
        var entity = PlanQuotaMapper.toJpa(planQuota);
        var saved = springDataPlanQuotaRepository.save(entity);
        return PlanQuotaMapper.toDomain(saved);
    }

    @Override
    public List<PlanQuota> findAllByPlanId(UUID planId) {
        return springDataPlanQuotaRepository.findAllByPlanId(planId).stream()
            .map(PlanQuotaMapper::toDomain)
            .toList();
    }

    @Override
    public void deleteAllByPlanId(UUID planId) {
        springDataPlanQuotaRepository.deleteAllByPlanId(planId);
    }
}
