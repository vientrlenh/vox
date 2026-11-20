package com.sep.vox.infrastructure.persistence.adapter;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.subscription.SchoolSubscriptionEvent;
import com.sep.vox.domain.repository.SchoolSubscriptionEventRepository;
import com.sep.vox.infrastructure.persistence.mapper.SchoolSubscriptionEventMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataSchoolSubscriptionEventRepository;

@Repository
public class SchoolSubscriptionEventRepositoryImpl implements SchoolSubscriptionEventRepository {

    private final SpringDataSchoolSubscriptionEventRepository springDataRepository;

    public SchoolSubscriptionEventRepositoryImpl(
            SpringDataSchoolSubscriptionEventRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public SchoolSubscriptionEvent save(SchoolSubscriptionEvent event) {
        var saved = springDataRepository.save(SchoolSubscriptionEventMapper.toJpa(event));
        return SchoolSubscriptionEventMapper.toDomain(saved);
    }

    @Override
    public List<SchoolSubscriptionEvent> findBySchoolId(UUID schoolId) {
        return springDataRepository.findBySchoolIdOrderByOccurredAtDesc(schoolId).stream()
            .map(SchoolSubscriptionEventMapper::toDomain)
            .toList();
    }
}
