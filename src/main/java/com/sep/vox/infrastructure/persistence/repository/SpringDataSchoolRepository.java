package com.sep.vox.infrastructure.persistence.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.SchoolJpaEntity;

public interface SpringDataSchoolRepository extends JpaRepository<SchoolJpaEntity, UUID> {
    Optional<SchoolJpaEntity> findByCode(String code);

    Optional<SchoolJpaEntity> findByDomain(String domain);

    boolean existsByCode(String code);

    boolean existsByDomain(String domain);

    boolean existsByContactEmail(String contactEmail);

    boolean existsByContactPhone(String contactPhone);

    boolean existsByDomainAndIdNot(String domain, UUID id);
    boolean existsByContactEmailAndIdNot(String email, UUID id);
    boolean existsByContactPhoneAndIdNot(String phone, UUID id);

    boolean existsByCodeAndIdNot(String normalizedCode, UUID id);
}
