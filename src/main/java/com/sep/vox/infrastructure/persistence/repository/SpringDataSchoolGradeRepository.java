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
        select grade
        from SchoolGradeJpaEntity grade
        join SchoolGradeLevelJpaEntity level on level.id = grade.schoolGradeLevelId
        where level.schoolId = :schoolId and grade.code = :code
        """)
    Optional<SchoolGradeJpaEntity> findBySchoolIdAndCode(@Param("schoolId") UUID schoolId, @Param("code") String code);

    @Query("""
        select grade
        from SchoolGradeJpaEntity grade
        join SchoolGradeLevelJpaEntity level on level.id = grade.schoolGradeLevelId
        where level.schoolId = :schoolId and grade.code in :codes
        """)
    List<SchoolGradeJpaEntity> findBySchoolIdAndCodeIn(@Param("schoolId") UUID schoolId, @Param("codes") Collection<String> codes);
}
