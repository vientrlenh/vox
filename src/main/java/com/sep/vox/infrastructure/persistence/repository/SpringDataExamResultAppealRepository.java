package com.sep.vox.infrastructure.persistence.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.ExamResultAppealJpaEntity;

public interface SpringDataExamResultAppealRepository extends JpaRepository<ExamResultAppealJpaEntity, UUID> {

    /**
     * Đơn còn đang chiếm chỗ. Liệt kê TƯỜNG MINH các trạng thái mở thay vì
     * {@code NOT IN} các trạng thái đóng: mỗi lần thêm một trạng thái kết thúc mới
     * (vừa rồi là {@code WITHDRAWN}) mà quên sửa {@code NOT IN} sẽ khoá học sinh khỏi
     * việc nộp đơn mới mà không ai nhận ra.
     */
    @Query("""
        SELECT COUNT(a) > 0 FROM ExamResultAppealJpaEntity a
        WHERE a.candidateResultId = :candidateResultId
        AND a.status IN ('PENDING', 'APPROVED', 'GRADING')
    """)
    boolean existsOpenByCandidateResultId(@Param("candidateResultId") UUID candidateResultId);

    /** Hạn mức phúc khảo đếm số vòng đã công bố; đơn bị từ chối không đốt lượt. */
    @Query("""
        SELECT COUNT(a) FROM ExamResultAppealJpaEntity a
        WHERE a.candidateResultId = :candidateResultId
        AND a.status = 'PUBLISHED'
    """)
    long countPublishedByCandidateResultId(@Param("candidateResultId") UUID candidateResultId);

    List<ExamResultAppealJpaEntity> findByCandidateResultId(UUID candidateResultId);

    /** candidateResultId/examId là cột FK trần, không có quan hệ JPA — join tường minh qua 3 bảng. */
    @Query("""
        SELECT COUNT(a) FROM ExamResultAppealJpaEntity a, ExamCandidateResultJpaEntity r, ExamJpaEntity e
        WHERE a.candidateResultId = r.id
        AND r.examId = e.id
        AND e.schoolId = :schoolId
        AND a.status IN :statuses
    """)
    long countBySchoolIdAndStatusIn(@Param("schoolId") UUID schoolId, @Param("statuses") Collection<String> statuses);

    /**
     * Mốc nộp của đơn CHỜ XỬ LÝ lâu nhất; null khi không còn đơn nào chờ.
     *
     * <p>Chỉ {@code PENDING}: đơn đã {@code APPROVED}/{@code GRADING} đang được chấm nên đồng hồ của
     * chúng đo một thứ khác — thời gian chấm, không phải thời gian trường bỏ quên đơn.
     *
     * <p>null KHÁC 0 và khác biệt đó phải đi tới tận giao diện: 0 ngày nghĩa là có đơn vừa nộp sáng
     * nay, còn null nghĩa là hàng đợi sạch. Gộp hai cái làm một sẽ vẽ một hàng đợi rỗng trông y hệt
     * một hàng đợi vừa nhận đơn.
     */
    @Query("""
        SELECT MIN(a.requestedAt) FROM ExamResultAppealJpaEntity a, ExamCandidateResultJpaEntity r, ExamJpaEntity e
        WHERE a.candidateResultId = r.id
        AND r.examId = e.id
        AND e.schoolId = :schoolId
        AND a.status = 'PENDING'
    """)
    Instant findOldestPendingRequestedAtBySchoolId(@Param("schoolId") UUID schoolId);
}
