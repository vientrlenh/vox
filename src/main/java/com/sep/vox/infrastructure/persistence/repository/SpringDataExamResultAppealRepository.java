package com.sep.vox.infrastructure.persistence.repository;

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
}
