package com.sep.vox.infrastructure.persistence.repository;

import java.util.UUID;

import com.sep.vox.domain.model.rubric.RubricOwnerType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.RubricJpaEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataRubricRepository extends JpaRepository<RubricJpaEntity, UUID> {
    boolean existsByOwnerTypeAndSchoolIdAndLanguageId(String ownerType, UUID schoolId, UUID languageId);

    boolean existsByOwnerTypeAndLanguageId(String ownerType, UUID languageId);


    @Modifying
    @Query("""
            
                UPDATE RubricJpaEntity r SET 
            r.name = COALESCE(:name, r.name),
            r.description = COALESCE(:description, r.description)
            WHERE r.id = :id
            """)
    int updateRubricAtomic(
            @Param("id") UUID id,
            @Param("name") String name,
            @Param("description") String description
    );


    Page<RubricJpaEntity> findAllByOwnerType(String ownerType, Pageable pageable);

    Page<RubricJpaEntity> findAllByOwnerTypeAndSchoolId(String ownerType, UUID schoolId, Pageable pageable);
}