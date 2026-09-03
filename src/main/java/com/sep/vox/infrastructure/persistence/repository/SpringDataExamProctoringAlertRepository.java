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
     * Phiên này có cảnh báo giám sát mức NGHIÊM TRỌNG nào không.
     *
     * <p>{@code exists} chứ không nạp danh sách rồi lọc: câu hỏi ở đây là có/không, và hàm được gọi
     * một lần cho MỖI câu trả lời khi bộ chấm ghi kết quả -- bài 4 câu là 4 lượt. Nạp cả danh sách
     * để rồi vứt đi là trả giá cho thứ không dùng.
     *
     * <p>So sánh không phân biệt hoa thường và bỏ khoảng trắng thừa, khớp đúng cách
     * {@code getAlertSeverity} bên FE đọc cột này -- nguồn ghi cảnh báo là sự kiện ngoài, không có
     * gì bảo đảm nó luôn viết hoa chuẩn.
     */
    boolean existsByExamSessionIdAndLevelIgnoreCase(UUID examSessionId, String level);

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
