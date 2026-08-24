package com.sep.vox.infrastructure.persistence.repository;

import com.sep.vox.infrastructure.persistence.entity.GradeLevelJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataGradeLevelRepository extends JpaRepository<GradeLevelJpaEntity, UUID> {
    Optional<GradeLevelJpaEntity> findByCode(String code);
    Optional<GradeLevelJpaEntity> findByName(String name);
    boolean existsByCode(String code);
    boolean existsByOrder(int order);

    // Nhận codes đã được Impl uppercase sẵn; entity-side vẫn bọc UPPER() để phòng dữ liệu cũ lỡ không đồng nhất case.
    @Query("SELECT gl FROM GradeLevelJpaEntity gl WHERE UPPER(gl.code) IN :codes")
    List<GradeLevelJpaEntity> findByCodeIn(@Param("codes") Collection<String> codes);

    List<GradeLevelJpaEntity> findByNameIn(Collection<String> names);

    @Query("""
        SELECT gl
        FROM GradeLevelJpaEntity gl
        WHERE (:searchPattern IS NULL
                OR LOWER(gl.code) LIKE :searchPattern
                OR LOWER(gl.name) LIKE :searchPattern)
            AND (:status IS NULL OR gl.status = :status)
        ORDER BY gl.order ASC
        """)
    Page<GradeLevelJpaEntity> findAllWithFilters(
        @Param("searchPattern") String searchPattern,
        @Param("status") String status,
        Pageable pageable
    );

    @Modifying
    @Query("""
        UPDATE GradeLevelJpaEntity l SET
        l.name = COALESCE(:name, l.name),
        l.description = COALESCE(:description, l.description),
        l.order = COALESCE(:order, l.order),
        l.updatedAt = :updatedAt,
        l.updatedBy = :updatedBy
        WHERE l.id = :id
        """)
    int updateGradeLevelAtomic(
        @Param("id") UUID id,
        @Param("name") String name,
        @Param("description") String description,
        @Param("order") Integer order,
        @Param("updatedAt") Instant updatedAt,
        @Param("updatedBy") UUID updatedBy
    );
}
