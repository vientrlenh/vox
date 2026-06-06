package com.sep.vox.infrastructure.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.SchoolClassJpaEntity;

public interface SpringDataSchoolClassRepository extends JpaRepository<SchoolClassJpaEntity, UUID>{

    @Query(value = """
        SELECT 
            id, 
            school_id, 
            language_id, 
            school_grade_id, 
            code, name, 
            description, 
            status, 
            created_at, 
            updated_at, 
            created_by, 
            updated_by 
        FROM (
            SELECT 
                sc.*,
                row_number() OVER (
                    PARTITION BY sc.school_id 
                    ORDER BY sc.id DESC
                ) AS rn 
            FROM school_classes sc 
            WHERE sc.school_id IN (:schoolIds)
        ) ranked
        WHERE ranked.rn BETWEEN :fromRow AND :toRow
        ORDER BY ranked.school_id, ranked.rn
    """, nativeQuery = true)
    List<SchoolClassJpaEntity> findBySchoolIdIn(@Param("schoolIds") Collection<UUID> schoolIds, @Param("fromRow") int fromRow, @Param("toRow") int toRow);
}
