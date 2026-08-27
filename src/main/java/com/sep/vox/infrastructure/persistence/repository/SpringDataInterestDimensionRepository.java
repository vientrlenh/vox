package com.sep.vox.infrastructure.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.InterestDimensionJpaEntity;

public interface SpringDataInterestDimensionRepository
        extends JpaRepository<InterestDimensionJpaEntity, String> {

    List<InterestDimensionJpaEntity> findAllByOrderByDisplayOrderAscCodeAsc();

    List<InterestDimensionJpaEntity> findByActiveTrueOrderByDisplayOrderAscCodeAsc();

    List<InterestDimensionJpaEntity> findByActiveTrueAndQuizEligibleTrueOrderByDisplayOrderAscCodeAsc();

    @Modifying
    @Query(value = """
        UPDATE interest_dimensions
        SET active = FALSE, updated_at = CURRENT_TIMESTAMP
        WHERE code = :code
        """, nativeQuery = true)
    void deactivate(@Param("code") String code);
}
