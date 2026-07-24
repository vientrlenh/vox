package com.sep.vox.infrastructure.persistence.repository;

import com.sep.vox.infrastructure.persistence.entity.FrameworkResultBandJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface SpringDataFrameworkResultBandRepository extends JpaRepository<FrameworkResultBandJpaEntity, UUID> {
    boolean existsByFrameworkVersionId(UUID frameworkVersionId);
    boolean existsByFrameworkVersionIdAndCodeAndIdNot(UUID frameworkVersionId, String code, UUID id);
    boolean existsByFrameworkVersionIdAndLabelAndIdNot(UUID frameworkVersionId, String label, UUID id);
    List<FrameworkResultBandJpaEntity> findByFrameworkVersionId(UUID frameworkVersionId);
    List<FrameworkResultBandJpaEntity> findByFrameworkVersionIdIn(Collection<UUID> frameworkVersionIds);
    List<FrameworkResultBandJpaEntity> findByFrameworkVersionIdAndCodeIn(UUID frameworkVersionId, Collection<String> codes);

    @Query("SELECT COUNT(b) > 0 FROM FrameworkResultBandJpaEntity b WHERE b.frameworkVersionId = :versionId AND b.code IN :codes")
    boolean existsByFrameworkVersionIdAndCodeIn(@Param("versionId") UUID versionId, @Param("codes") Collection<String> codes);

    @Query("SELECT COUNT(b) > 0 FROM FrameworkResultBandJpaEntity b WHERE b.frameworkVersionId = :versionId AND b.label IN :labels")
    boolean existsByFrameworkVersionIdAndLabelIn(@Param("versionId") UUID versionId, @Param("labels") Collection<String> labels);
    Optional<FrameworkResultBandJpaEntity> findByFrameworkVersionIdAndCode(UUID frameworkVersionId, String code);
    Optional<FrameworkResultBandJpaEntity> findByFrameworkVersionIdAndLabel(UUID frameworkVersionId, String label);

    @Modifying
    @Query("DELETE FROM FrameworkResultBandJpaEntity b WHERE b.frameworkVersionId = :frameworkVersionId")
    void deleteByFrameworkVersionId(@Param("frameworkVersionId") UUID frameworkVersionId);
}
