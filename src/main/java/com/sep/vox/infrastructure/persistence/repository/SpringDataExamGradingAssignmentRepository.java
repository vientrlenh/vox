package com.sep.vox.infrastructure.persistence.repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.ExamGradingAssignmentJpaEntity;

public interface SpringDataExamGradingAssignmentRepository
        extends JpaRepository<ExamGradingAssignmentJpaEntity, UUID> {

    /**
     * Dòng đang mở của một bài. Lọc bằng {@code active_result_id} chứ không bằng
     * {@code candidate_result_id + status}: cột active chính là thứ unique index đang
     * canh, nên hai chỗ luôn nói cùng một chuyện.
     */
    Optional<ExamGradingAssignmentJpaEntity> findByActiveResultId(UUID activeResultId);

    List<ExamGradingAssignmentJpaEntity> findByActiveResultIdIn(Collection<UUID> activeResultIds);

    List<ExamGradingAssignmentJpaEntity> findByCandidateResultIdOrderByAssignedAtDesc(UUID candidateResultId);

    List<ExamGradingAssignmentJpaEntity> findByCandidateResultIdIn(Collection<UUID> candidateResultIds);

    List<ExamGradingAssignmentJpaEntity> findByAppealId(UUID appealId);

    @Query("""
        SELECT ga FROM ExamGradingAssignmentJpaEntity ga
        WHERE ga.status = 'ASSIGNED' AND ga.deadlineAt IS NOT NULL AND ga.deadlineAt < :now
        ORDER BY ga.deadlineAt ASC
    """)
    List<ExamGradingAssignmentJpaEntity> findOverdue(@Param("now") OffsetDateTime now);

    /**
     * Quá hạn TRONG một trường (và một kỳ thi nếu có). Thu hồi hàng loạt đi lối này
     * thay vì {@link #findOverdue} + lọc ở Java: bản cũ quét toàn hệ thống rồi gọi
     * access service từng dòng để đọc ra schoolId — 4N+1 query, và thời gian phản hồi
     * của một trường phụ thuộc vào dữ liệu của các trường khác.
     *
     * <p>{@code Pageable} ở đây là TRẦN của một lượt thu hồi, không phải phân trang cho
     * UI: một cú bấm "Thu hồi toàn bộ" không được ôm cả trường vào một transaction ghi.
     * Cũ nhất trước để lượt sau tiếp đúng chỗ lượt trước dừng.
     */
    @Query("""
        SELECT ga FROM ExamGradingAssignmentJpaEntity ga
        JOIN ExamCandidateResultJpaEntity cr ON cr.id = ga.candidateResultId
        JOIN ExamJpaEntity e ON e.id = cr.examId
        WHERE ga.status = 'ASSIGNED' AND ga.deadlineAt IS NOT NULL AND ga.deadlineAt < :now
        AND e.schoolId = :schoolId
        AND (:examId IS NULL OR cr.examId = :examId)
        ORDER BY ga.deadlineAt ASC
    """)
    List<ExamGradingAssignmentJpaEntity> findOverdueInSchool(
        @Param("now") OffsetDateTime now,
        @Param("schoolId") UUID schoolId,
        @Param("examId") UUID examId,
        Pageable pageable);

    /**
     * Sắp/đã tới hạn mà chưa nhắc lần nào — {@code reminded_at} là chốt chống trùng.
     *
     * <p>Native + {@code FOR UPDATE SKIP LOCKED} vì {@code reminded_at} một mình chỉ
     * chống trùng qua các LƯỢT chạy, không chống trùng giữa các INSTANCE: hai replica
     * cùng đọc trước khi bên nào commit thì cả hai đều thấy {@code NULL} và cùng gửi
     * mail. {@code SKIP LOCKED} cho chúng chia nhau các dòng khác nhau; {@code LIMIT}
     * chặn một lượt chạy ôm cả nghìn dòng trong một transaction.
     */
    @Query(value = """
        SELECT * FROM exam_grading_assignments
        WHERE status = 'ASSIGNED' AND reminded_at IS NULL
        AND deadline_at IS NOT NULL AND deadline_at < :threshold
        ORDER BY deadline_at ASC
        LIMIT 200
        FOR UPDATE SKIP LOCKED
    """, nativeQuery = true)
    List<ExamGradingAssignmentJpaEntity> findDueForReminder(@Param("threshold") OffsetDateTime threshold);

    void deleteByCandidateResultIdIn(Collection<UUID> candidateResultIds);
}
