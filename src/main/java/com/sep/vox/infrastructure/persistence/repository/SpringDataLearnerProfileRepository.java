package com.sep.vox.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import com.sep.vox.infrastructure.persistence.entity.FrameworkResultBandJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.LearnerProfileJpaEntity;

public interface SpringDataLearnerProfileRepository
        extends JpaRepository<LearnerProfileJpaEntity, UUID> {

    Optional<LearnerProfileJpaEntity> findByStudentId(UUID studentId);

    /** Khoá FOR SHARE hồ sơ trước khi cập nhật tại chỗ -- tránh hai request cùng ghi đè nhau. */
    @Lock(LockModeType.PESSIMISTIC_READ)
    Optional<LearnerProfileJpaEntity> findWithLockByStudentId(UUID studentId);
    @Query(value = """
        WITH active_version AS (
            SELECT fv.id
            FROM framework_versions fv
            JOIN frameworks f ON f.id = fv.framework_id AND f.is_active = true
            WHERE fv.status = 'PUBLISHED'
              AND fv.effective_from <= CURRENT_TIMESTAMP
              AND (fv.effective_to IS NULL OR fv.effective_to >= CURRENT_TIMESTAMP)
              AND (CAST(:code AS varchar) IS NULL OR fv.code = CAST(:code AS varchar))
            ORDER BY fv.version DESC
            LIMIT 1
        ),
        ranked AS (
            SELECT band.id,
                   ROW_NUMBER() OVER (ORDER BY band.result_band_order) AS rn,
                   COUNT(*) OVER () AS total
            FROM framework_result_bands band
            JOIN active_version ON active_version.id = band.framework_version_id
        )
        SELECT band.*
        FROM framework_result_bands band
        JOIN ranked ON ranked.id = band.id AND ranked.rn = (ranked.total + 1) / 2
        """, nativeQuery = true)
    List<FrameworkResultBandJpaEntity> findDefaultTargetBand(@Param("code") String code);


    @Query(value = """
        SELECT MAX(band.result_band_order)
        FROM framework_result_bands band
        WHERE band.framework_version_id = :frameworkVersionId
        """, nativeQuery = true)
    List<Integer> findFrameworkBandCount(
        @Param("frameworkVersionId") UUID frameworkVersionId
    );

    
    @Query(value = """
        SELECT band.*
        FROM framework_result_bands band
        WHERE band.framework_version_id = :frameworkVersionId
        ORDER BY band.result_band_order
        """, nativeQuery = true)
    List<FrameworkResultBandJpaEntity> findFrameworkBandLadder(
        @Param("frameworkVersionId") UUID frameworkVersionId
    );
}
