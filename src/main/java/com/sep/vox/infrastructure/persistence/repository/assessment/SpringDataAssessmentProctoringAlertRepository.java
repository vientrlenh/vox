package com.sep.vox.infrastructure.persistence.repository.assessment;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentProctoringAlertJpaEntity;

public interface SpringDataAssessmentProctoringAlertRepository extends JpaRepository<AssessmentProctoringAlertJpaEntity, UUID> {

    boolean existsByEventId(String eventId);

    List<AssessmentProctoringAlertJpaEntity> findByAssessmentSessionIdOrderByCapturedAtAsc(UUID assessmentSessionId);

    /**
     * Phiên này có cảnh báo nào thuộc nhóm đặt nghi vấn về LIÊM CHÍNH của bài không.
     *
     * <p>{@code exists} chứ không nạp danh sách rồi lọc: câu hỏi ở đây là có/không, và hàm được gọi
     * một lần cho MỖI câu trả lời khi bộ chấm ghi kết quả -- bài 4 câu là 4 lượt. Nạp cả danh sách
     * để rồi vứt đi là trả giá cho thứ không dùng.
     *
     * <p>{@code UPPER(...)} vì nguồn ghi cảnh báo là sự kiện ngoài, không có gì bảo đảm nó luôn viết
     * hoa chuẩn -- cùng lý do mà bản trước so sánh {@code level} theo kiểu IgnoreCase. Danh sách
     * truyền vào đã là chữ hoa (xem {@code AssessmentProctoringAlert.INTEGRITY_ALERT_TYPES}).
     *
     * <p>Câu trước hỏi theo {@code level = 'CRITICAL'}; vì sao đổi sang hỏi theo LOẠI, xem javadoc
     * của {@code INTEGRITY_ALERT_TYPES}.
     */
    @Query("""
        SELECT COUNT(alert) > 0 FROM AssessmentProctoringAlertJpaEntity alert
        WHERE alert.assessmentSessionId = :assessmentSessionId
          AND UPPER(TRIM(alert.alertType)) IN :alertTypes
    """)
    boolean existsIntegrityAlert(
        @Param("assessmentSessionId") UUID assessmentSessionId,
        @Param("alertTypes") Collection<String> alertTypes);

    /**
     * Mọi cảnh báo của một ca thi, gộp qua tất cả phiên thi trong ca.
     *
     * <p>Bảng cảnh báo chỉ biết tới phiên thi, còn ca thi thì nằm cách đó hai chặng
     * (phiên -> thí sinh -> ca), nên phải viết JPQL thay vì dựa vào tên phương thức. Dùng subquery
     * chứ không join thẳng để kết quả không nhân bản nếu chặng giữa trả về nhiều hàng.
     */
    @Query("""
        SELECT alert FROM AssessmentProctoringAlertJpaEntity alert
        WHERE alert.assessmentSessionId IN (
            SELECT session.id FROM AssessmentSessionJpaEntity session
            JOIN AssessmentCandidateJpaEntity candidate ON candidate.id = session.candidateId
            WHERE candidate.scheduleId = :scheduleId
        )
        ORDER BY alert.capturedAt ASC
    """)
    List<AssessmentProctoringAlertJpaEntity> findByScheduleIdOrderByCapturedAtAsc(@Param("scheduleId") UUID scheduleId);
}
