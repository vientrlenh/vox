package com.sep.vox.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.subscription.SchoolSubscription;
import com.sep.vox.domain.model.subscription.SubscriptionStatus;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.infrastructure.persistence.mapper.SchoolSubscriptionMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataSchoolSubscriptionRepository;

@Repository
public class SchoolSubscriptionRepositoryImpl implements SchoolSubscriptionRepository {

    private final SpringDataSchoolSubscriptionRepository springDataSchoolSubscriptionRepository;

    public SchoolSubscriptionRepositoryImpl(SpringDataSchoolSubscriptionRepository springDataSchoolSubscriptionRepository) {
        this.springDataSchoolSubscriptionRepository = springDataSchoolSubscriptionRepository;
    }

    @Override
    public Optional<SchoolSubscription> findById(UUID id) {
        return springDataSchoolSubscriptionRepository.findById(id).map(SchoolSubscriptionMapper::toDomain);
    }

    @Override
    public SchoolSubscription save(SchoolSubscription subscription) {
        var entity = SchoolSubscriptionMapper.toJpa(subscription);
        var saved = springDataSchoolSubscriptionRepository.save(entity);
        return SchoolSubscriptionMapper.toDomain(saved);
    }

    @Override
    public Optional<SchoolSubscription> findActiveBySchoolId(UUID schoolId) {
        return springDataSchoolSubscriptionRepository
            .findFirstBySchoolIdAndStatus(schoolId, SubscriptionStatus.ACTIVE.name())
            .map(SchoolSubscriptionMapper::toDomain);
    }

    @Override
    public List<SchoolSubscription> findAllBySchoolId(UUID schoolId) {
        return springDataSchoolSubscriptionRepository.findAllBySchoolId(schoolId).stream()
            .map(SchoolSubscriptionMapper::toDomain)
            .toList();
    }
}
