package com.sep.vox.infrastructure.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.FrameworkVersionJpaEntity;

import jakarta.persistence.LockModeType;

public interface SpringDataFrameworkVersionRepository extends JpaRepository<FrameworkVersionJpaEntity, UUID> {
    boolean existsByFrameworkId(UUID frameworkId);
    Optional<FrameworkVersionJpaEntity> findByCode(String code);
    Optional<FrameworkVersionJpaEntity> findByName(String name);
    // Nhận input đã được Impl uppercase sẵn; entity-side vẫn bọc UPPER() để phòng dữ liệu cũ lỡ không đồng nhất case.
    @Query("SELECT v FROM FrameworkVersionJpaEntity v WHERE UPPER(v.code) IN :codes")
    List<FrameworkVersionJpaEntity> findByCodeIn(@Param("codes") Collection<String> codes);
    List<FrameworkVersionJpaEntity> findByNameIn(Collection<String> names);
    Page<FrameworkVersionJpaEntity> findByFrameworkId(UUID frameworkId, Pageable pageable);
    List<FrameworkVersionJpaEntity> findByFrameworkIdAndStatus(UUID frameworkId, String status);
    Page<FrameworkVersionJpaEntity> findByFrameworkIdAndStatus(UUID frameworkId, String status, Pageable pageable);
    Optional<FrameworkVersionJpaEntity> findByFrameworkIdAndVersion(UUID frameworkId, int version);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM FrameworkVersionJpaEntity v WHERE v.id = :id")
    Optional<FrameworkVersionJpaEntity> findByIdForUpdate(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE FrameworkVersionJpaEntity v SET v.status = :status WHERE v.id = :id")
    int updateStatus(@Param("id") UUID id, @Param("status") String status);
}
