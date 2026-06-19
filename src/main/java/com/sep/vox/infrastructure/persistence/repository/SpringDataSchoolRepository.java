package com.sep.vox.infrastructure.persistence.repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.SchoolJpaEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataSchoolRepository extends JpaRepository<SchoolJpaEntity, UUID> {
    Optional<SchoolJpaEntity> findByCode(String code);

    Optional<SchoolJpaEntity> findByDomain(String domain);

    boolean existsByCode(String code);
    boolean existsByContactEmail(String contactEmail);

    boolean existsByContactPhone(String contactPhone);

    boolean existsByDomainAndIdNot(String domain, UUID id);
    boolean existsByContactEmailAndIdNot(String email, UUID id);
    boolean existsByContactPhoneAndIdNot(String phone, UUID id);

    boolean existsByCodeAndIdNot(String normalizedCode, UUID id);
    boolean existsByDomain(String domain);

    //COALESCE sẽ đóng vai trò hoạt động như sau : nhập name nếu ma null -> s.name
    //            s.name = COALESCE(:name, s.name, 'Vit'),
    //nếu mà s.name null -> Vit
    @Modifying
    @Query("""
            UPDATE SchoolJpaEntity s SET 
            s.name = COALESCE(:name, s.name),
            s.description = COALESCE(:description, s.description),
            s.contactPhone = COALESCE(:phone, s.contactPhone),
            s.contactEmail = COALESCE(:email, s.contactEmail),
            s.domain = COALESCE(:domain, s.domain),
            s.address = COALESCE(:address, s.address),
            s.studentCount = COALESCE(:studentCount, s.studentCount),
            s.updatedAt = :updatedAt,
            s.updatedBy = :updatedBy
            WHERE s.id = :id
            """)
    int updateSchoolAtomic(
            @Param("id") UUID id,
            @Param("name") String name,
            @Param("description") String description,
            @Param("phone") String phone,
            @Param("email") String email,
            @Param("domain") String domain,
            @Param("address") String address,
            @Param("studentCount") Integer studentCount,
            @Param("updatedAt") OffsetDateTime updatedAt,
            @Param("updatedBy") UUID updatedBy
    );
    List<SchoolJpaEntity> findByIdIn(Collection<UUID> ids);
    boolean existsByCode(String code);
}
