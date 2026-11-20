package com.sep.vox.infrastructure.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

import com.sep.vox.infrastructure.persistence.entity.ExamCandidateResultJpaEntity;

public interface SpringDataExamCandidateResultRepository extends JpaRepository<ExamCandidateResultJpaEntity, UUID> {
    Optional<ExamCandidateResultJpaEntity> findBySessionId(UUID sessionId);
    List<ExamCandidateResultJpaEntity> findByIdIn(Collection<UUID> ids);
    List<ExamCandidateResultJpaEntity> findBySessionIdIn(Collection<UUID> sessionIds);
    List<ExamCandidateResultJpaEntity> findByExamId(UUID examId);
    void deleteBySessionId(UUID sessionId);

    /**
     * Xoá mềm kết quả theo phiên thi. Đi cùng {@code SpringDataExamSessionRepository#softDelete}:
     * điểm phải biến mất khỏi hàng đợi chấm, phúc khảo và bảng kết quả cùng lúc với phiên, nếu không
     * sẽ còn một dòng điểm mồ côi trỏ vào phiên đã xoá.
     *
     * @return số dòng vừa xoá (0 nếu phiên chưa từng có kết quả, hoặc đã xoá từ trước).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE ExamCandidateResultJpaEntity r
        SET r.status = 'DELETED', r.deletedAt = :deletedAt, r.deletedReason = :reason
        WHERE r.sessionId = :sessionId AND r.deletedAt IS NULL
    """)
    int softDeleteBySessionId(@Param("sessionId") UUID sessionId, @Param("deletedAt") Instant deletedAt,
        @Param("reason") String reason);
}
