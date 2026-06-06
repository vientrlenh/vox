package com.sep.vox.infrastructure.persistence.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.sep.vox.infrastructure.persistence.entity.SchoolClassJpaEntity;

public interface SpringDataSchoolClassRepository extends JpaRepository<SchoolClassJpaEntity, UUID>{
    Optional<SchoolClassJpaEntity> findBySchoolIdAndCode(UUID schoolId, String code);

    @Query("""
        SELECT schoolClass
        FROM SchoolClassJpaEntity schoolClass
        WHERE schoolClass.schoolId = :schoolId
            AND (:search IS NULL
                OR LOWER(schoolClass.code) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(schoolClass.name) LIKE LOWER(CONCAT('%', :search, '%')))
            AND (:status IS NULL OR schoolClass.status = :status)
            AND (:languageId IS NULL OR schoolClass.languageId = :languageId)
            AND (:schoolGradeId IS NULL OR schoolClass.schoolGradeId = :schoolGradeId)
        """)
    Page<SchoolClassJpaEntity> findBySchoolIdWithFilters(
        @Param("schoolId") UUID schoolId,
        @Param("search") String search,
        @Param("status") String status,
        @Param("languageId") UUID languageId,
        @Param("schoolGradeId") UUID schoolGradeId,
        Pageable pageable
    );

}
