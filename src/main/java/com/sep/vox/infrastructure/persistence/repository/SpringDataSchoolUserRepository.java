package com.sep.vox.infrastructure.persistence.repository;

import com.sep.vox.infrastructure.persistence.entity.SchoolUserJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SpringDataSchoolUserRepository extends JpaRepository<SchoolUserJpaEntity, UUID> {
    boolean existsByUserIdAndSchoolId(UUID userId, UUID schoolId);
}
