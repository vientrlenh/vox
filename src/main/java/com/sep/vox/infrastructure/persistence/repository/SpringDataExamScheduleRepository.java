package com.sep.vox.infrastructure.persistence.repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.ExamScheduleJpaEntity;

public interface SpringDataExamScheduleRepository extends JpaRepository<ExamScheduleJpaEntity, UUID> {
    List<ExamScheduleJpaEntity> findByExamId(UUID examId);

    @Query("""
        SELECT es 
        FROM ExamScheduleJpaEntity es 
        WHERE es.examId = :examId 
            AND es.startDate <= :now 
            AND es.endDate > :now
    """)
    List<ExamScheduleJpaEntity> findByExamIdAndInSchedule(@Param("examId") UUID examId, @Param("now") OffsetDateTime now);

    @Query("""
        SELECT es 
        FROM ExamScheduleJpaEntity es 
        WHERE es.id = :id 
            AND es.startDate <= :now 
            AND es.endDate > :now 
    """)
    Optional<ExamScheduleJpaEntity> findByIdAndInSchedule(@Param("id") UUID id, @Param("now") OffsetDateTime now);

    @Query("""
        SELECT es 
        FROM ExamScheduleJpaEntity es
        WHERE es.id IN :ids
            AND es.startDate <= :now 
            AND es.endDate > :now    
    """)
    List<ExamScheduleJpaEntity> findByIdInAndInSchedule(@Param("ids") Collection<UUID> ids, @Param("now") OffsetDateTime now);
}
