package com.sep.vox.infrastructure.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.SchoolGradeJpaEntity;

public interface SpringDataSchoolGradeRepository extends JpaRepository<SchoolGradeJpaEntity, UUID>{
    @Query("""
        SELECT sg
        FROM SchoolGradeJpaEntity sg
        JOIN SchoolGradeLevelJpaEntity sgl 
            ON sgl.id = sg.schoolGradeLevelId
        WHERE sgl.schoolId = :schoolId 
            AND sg.code = :code
        """)
    Optional<SchoolGradeJpaEntity> findBySchoolIdAndCode(@Param("schoolId") UUID schoolId, @Param("code") String code);

    @Query("""
        SELECT sg
        FROM SchoolGradeJpaEntity sg
        JOIN SchoolGradeLevelJpaEntity sgl 
            ON sgl.id = sg.schoolGradeLevelId
        WHERE sgl.schoolId = :schoolId 
            AND sg.code IN (:codes)
        """)
    List<SchoolGradeJpaEntity> findBySchoolIdAndCodeIn(@Param("schoolId") UUID schoolId, @Param("codes") Collection<String> codes);

    List<SchoolGradeJpaEntity> findByIdIn(Collection<UUID> ids);
}
