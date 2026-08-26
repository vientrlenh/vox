package com.sep.vox.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.SchoolSubscriptionEventJpaEntity;

public interface SpringDataSchoolSubscriptionEventRepository
        extends JpaRepository<SchoolSubscriptionEventJpaEntity, UUID> {

    List<SchoolSubscriptionEventJpaEntity> findBySchoolIdOrderByOccurredAtDesc(UUID schoolId);
}
