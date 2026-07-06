package com.sep.vox.infrastructure.persistence.repository;


import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.SchoolRoomJpaEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataSchoolRoomRepository extends JpaRepository<SchoolRoomJpaEntity, UUID> {
    // Thêm dòng này để Spring Data tự động query kiểm tra mã code
    boolean existsBySchoolIdAndCode(UUID schoolId, String code);

    boolean existsBySchoolIdAndIsActive(UUID schoolId, boolean isActive);

    Page<SchoolRoomJpaEntity> findBySchoolId(UUID schoolId, Pageable pageable);

    List<SchoolRoomJpaEntity> findBySchoolIdAndCodeIn(UUID schoolId, Collection<String> codes);

    List<SchoolRoomJpaEntity> findByIdIn(Collection<UUID> ids);


    @Modifying
    @Query("""
            UPDATE SchoolRoomJpaEntity r SET
            r.name = COALESCE(:name, r.name),
            r.description = COALESCE(:description, r.description),
            r.updatedAt = :updatedAt,
            r.updatedBy = :updatedBy
            WHERE r.id = :id
            """)
    int updateSchoolRoomAtomic(
            @Param("id") UUID id,
            @Param("name") String name,
            @Param("description") String description,
            @Param("updatedAt") OffsetDateTime updatedAt,
            @Param("updatedBy") UUID updatedBy
    );
}
