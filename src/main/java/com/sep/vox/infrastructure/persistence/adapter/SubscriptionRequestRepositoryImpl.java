package com.sep.vox.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.subscription.RequestStatus;
import com.sep.vox.domain.model.subscription.SubscriptionRequest;
import com.sep.vox.domain.repository.SubscriptionRequestRepository;
import com.sep.vox.infrastructure.persistence.mapper.SubscriptionRequestMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataSubscriptionRequestRepository;

@Repository
public class SubscriptionRequestRepositoryImpl implements SubscriptionRequestRepository {

    private final SpringDataSubscriptionRequestRepository springDataSubscriptionRequestRepository;

    public SubscriptionRequestRepositoryImpl(SpringDataSubscriptionRequestRepository springDataSubscriptionRequestRepository) {
        this.springDataSubscriptionRequestRepository = springDataSubscriptionRequestRepository;
    }

    @Override
    public Optional<SubscriptionRequest> findById(UUID id) {
        return springDataSubscriptionRequestRepository.findById(id).map(SubscriptionRequestMapper::toDomain);
    }

    @Override
    public SubscriptionRequest save(SubscriptionRequest request) {
        var entity = SubscriptionRequestMapper.toJpa(request);
        var saved = springDataSubscriptionRequestRepository.save(entity);
        return SubscriptionRequestMapper.toDomain(saved);
    }

    @Override
    public List<SubscriptionRequest> findAllBySchoolId(UUID schoolId) {
        return springDataSubscriptionRequestRepository.findAllBySchoolId(schoolId).stream()
            .map(SubscriptionRequestMapper::toDomain)
            .toList();
    }

    @Override
    public List<SubscriptionRequest> findAllByStatus(RequestStatus status) {
        return springDataSubscriptionRequestRepository.findAllByStatus(status.name()).stream()
            .map(SubscriptionRequestMapper::toDomain)
            .toList();
    }
}
