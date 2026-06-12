package com.sep.vox.infrastructure.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.SchoolClassUserJpaEntity;

public interface SpringDataSchoolClassUserRepository extends JpaRepository<SchoolClassUserJpaEntity, UUID>{
    Optional<SchoolClassUserJpaEntity> findByUserIdAndSchoolClassId(UUID userId, UUID schoolClassId);
    List<SchoolClassUserJpaEntity> findByUserIdInAndSchoolClassIdIn(Collection<UUID> userIds, Collection<UUID> schoolClassIds);
    List<SchoolClassUserJpaEntity> findByUserId(UUID userId);
    List<SchoolClassUserJpaEntity> findBySchoolClassId(UUID schoolClassId);
    boolean existsBySchoolClassId(UUID schoolClassId);
}
