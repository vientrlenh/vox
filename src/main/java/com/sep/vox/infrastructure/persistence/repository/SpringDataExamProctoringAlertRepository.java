package com.sep.vox.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.ExamProctoringAlertJpaEntity;

public interface SpringDataExamProctoringAlertRepository extends JpaRepository<ExamProctoringAlertJpaEntity, UUID> {

    boolean existsByEventId(String eventId);

    List<ExamProctoringAlertJpaEntity> findByExamSessionIdOrderByCapturedAtAsc(UUID examSessionId);

    /**
     * Mọi cảnh báo của một ca thi, gộp qua tất cả phiên thi trong ca.
     *
     * <p>Bảng cảnh báo chỉ biết tới phiên thi, còn ca thi thì nằm cách đó hai chặng
     * (phiên -> thí sinh -> ca), nên phải viết JPQL thay vì dựa vào tên phương thức. Dùng subquery
     * chứ không join thẳng để kết quả không nhân bản nếu chặng giữa trả về nhiều hàng.
     */
    @Query("""
        SELECT alert FROM ExamProctoringAlertJpaEntity alert
        WHERE alert.examSessionId IN (
            SELECT session.id FROM ExamSessionJpaEntity session
            JOIN ExamCandidateJpaEntity candidate ON candidate.id = session.candidateId
            WHERE candidate.scheduleId = :scheduleId
        )
        ORDER BY alert.capturedAt ASC
    """)
    List<ExamProctoringAlertJpaEntity> findByScheduleIdOrderByCapturedAtAsc(@Param("scheduleId") UUID scheduleId);
}
