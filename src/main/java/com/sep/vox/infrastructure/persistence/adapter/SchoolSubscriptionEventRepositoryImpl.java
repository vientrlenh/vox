package com.sep.vox.infrastructure.persistence.adapter;

import com.sep.vox.infrastructure.persistence.entity.subscription.SchoolSubscriptionEventJpaEntity;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.subscription.SchoolSubscriptionEvent;
import com.sep.vox.domain.repository.SchoolSubscriptionEventRepository;
import com.sep.vox.infrastructure.persistence.mapper.SchoolSubscriptionEventMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataSchoolSubscriptionEventRepository;

@Repository
@RequiredArgsConstructor
public class SchoolSubscriptionEventRepositoryImpl implements SchoolSubscriptionEventRepository {

    private final SpringDataSchoolSubscriptionEventRepository springDataRepository;

    @Override
    public SchoolSubscriptionEvent save(SchoolSubscriptionEvent event) {
        SchoolSubscriptionEventJpaEntity saved = springDataRepository.save(SchoolSubscriptionEventMapper.toJpa(event));
        return SchoolSubscriptionEventMapper.toDomain(saved);
    }

    @Override
    public List<SchoolSubscriptionEvent> findBySchoolId(UUID schoolId) {
        return springDataRepository.findBySchoolIdOrderByOccurredAtDesc(schoolId).stream()
            .map(SchoolSubscriptionEventMapper::toDomain)
            .toList();
    }
}
