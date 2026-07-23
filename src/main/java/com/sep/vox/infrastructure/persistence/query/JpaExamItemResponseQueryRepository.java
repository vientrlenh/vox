package com.sep.vox.infrastructure.persistence.query;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.application.query.dto.ExamItemResponseDto;
import com.sep.vox.application.query.repository.ExamItemResponseQueryRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class JpaExamItemResponseQueryRepository implements ExamItemResponseQueryRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<ExamItemResponseDto> findByStudentIdWithAudio(UUID studentId) {
        return em.createQuery("""
            SELECT new com.sep.vox.application.query.dto.ExamItemResponseDto(
                r.id,
                s.examId,
                r.audioUrl,
                r.durationSeconds,
                r.transcript,
                r.submittedAt
            )
            FROM ExamItemResponseJpaEntity r
            JOIN ExamSessionJpaEntity s ON s.id = r.sessionId
            JOIN ExamCandidateJpaEntity c ON c.id = s.candidateId
            WHERE r.audioUrl IS NOT NULL
                AND c.studentId = :studentId
            ORDER BY r.submittedAt DESC
        """, ExamItemResponseDto.class)
        .setParameter("studentId", studentId)
        .getResultList();
    }
}
