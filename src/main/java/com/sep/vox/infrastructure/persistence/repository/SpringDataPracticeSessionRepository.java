package com.sep.vox.infrastructure.persistence.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.application.query.dto.CriterionFrameworkInfo;
import com.sep.vox.application.query.dto.PracticeDashboardCountsInfo;
import com.sep.vox.application.query.dto.SessionRowInfo;
import com.sep.vox.infrastructure.persistence.entity.PracticeSessionJpaEntity;

public interface SpringDataPracticeSessionRepository
        extends JpaRepository<PracticeSessionJpaEntity, UUID> {

    // Khoá row cho suốt transaction resolve-next-question (gói 11 mục 2.4 bước 4) -- một
    // request thứ 2 (Python retry sau timeout, xem practice_session_client.py) cho CÙNG
    // session phải đợi request đầu commit xong rồi mới đọc, chứ không được chạy song song
    // và cùng chọn/lưu 1 câu MAIN mới độc lập với nhau (race → có thể vỡ ràng buộc slot).
    @Query(value = "SELECT * FROM practice_session WHERE id = :id FOR UPDATE", nativeQuery = true)
    Optional<PracticeSessionJpaEntity> findByIdForUpdate(@Param("id") UUID id);

    boolean existsByIdAndStudentIdAndStatus(UUID id, UUID studentId, String status);

    @Query(value = """
        SELECT session.id AS id,
               session.practice_paper_id AS practicePaperId,
               session.chosen_practice_topic_id AS chosenPracticeTopicId,
               topic.name AS topicName,
               session.origin AS origin,
               session.status AS status,
               session.abandon_diagnosis AS abandonDiagnosis,
               session.overall_score AS overallScore,
               session.graded_seconds AS gradedSeconds,
               session.offered_topic_ids_json AS offeredTopicIdsJson,
               session.started_at AS startedAt,
               session.ended_at AS endedAt,
               -- Thang chấm của luyện tập là 0-100 CỐ ĐỊNH -- Azure trả HundredMark nên đây
               -- là thang gốc. Vẫn trả về cho client thay vì để client tự biết: mẫu số viết
               -- cứng ở giao diện chính là lỗi đã gặp (in " / 100" khi dữ liệu là 0-10).
               0   AS scoreScaleMin,
               100 AS scoreScaleMax
        FROM practice_session session
        JOIN practice_topic topic ON topic.id = session.chosen_practice_topic_id
        WHERE session.id = :sessionId AND session.student_id = :studentId
        """, nativeQuery = true)
    Optional<SessionRowInfo> findSessionRow(
        @Param("sessionId") UUID sessionId,
        @Param("studentId") UUID studentId
    );

    @Query(value = """
        SELECT session.id AS id,
               session.practice_paper_id AS practicePaperId,
               session.chosen_practice_topic_id AS chosenPracticeTopicId,
               topic.name AS topicName,
               session.origin AS origin,
               session.status AS status,
               session.abandon_diagnosis AS abandonDiagnosis,
               session.overall_score AS overallScore,
               session.graded_seconds AS gradedSeconds,
               session.offered_topic_ids_json AS offeredTopicIdsJson,
               session.started_at AS startedAt,
               session.ended_at AS endedAt,
               -- Thang chấm của luyện tập là 0-100 CỐ ĐỊNH -- Azure trả HundredMark nên đây
               -- là thang gốc. Vẫn trả về cho client thay vì để client tự biết: mẫu số viết
               -- cứng ở giao diện chính là lỗi đã gặp (in " / 100" khi dữ liệu là 0-10).
               0   AS scoreScaleMin,
               100 AS scoreScaleMax
        FROM practice_session session
        JOIN practice_topic topic ON topic.id = session.chosen_practice_topic_id
        WHERE session.id = :sessionId
        """, nativeQuery = true)
    Optional<SessionRowInfo> findSessionRowById(@Param("sessionId") UUID sessionId);

    @Query(value = """
        SELECT session.id AS id,
               session.practice_paper_id AS practicePaperId,
               session.chosen_practice_topic_id AS chosenPracticeTopicId,
               topic.name AS topicName,
               session.origin AS origin,
               session.status AS status,
               session.abandon_diagnosis AS abandonDiagnosis,
               session.overall_score AS overallScore,
               session.graded_seconds AS gradedSeconds,
               session.offered_topic_ids_json AS offeredTopicIdsJson,
               session.started_at AS startedAt,
               session.ended_at AS endedAt
        FROM practice_session session
        JOIN practice_topic topic ON topic.id = session.chosen_practice_topic_id
        WHERE session.student_id = :studentId
        ORDER BY session.started_at DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<SessionRowInfo> findHistory(@Param("studentId") UUID studentId, @Param("limit") int limit);

    @Query(value = """
        SELECT EXISTS (
            SELECT 1
            FROM practice_session session
            JOIN school_class_users student_membership
              ON student_membership.user_id = session.student_id
             AND student_membership.is_active = true
            JOIN school_classes class
              ON class.id = student_membership.school_class_id
            JOIN school_users teacher
              ON teacher.school_id = class.school_id
             AND teacher.user_id = :teacherId
             AND (teacher.end_date IS NULL OR teacher.end_date >= CURRENT_TIMESTAMP)
            WHERE session.id = :sessionId
        )
        """, nativeQuery = true)
    boolean canTeacherReadSession(@Param("teacherId") UUID teacherId, @Param("sessionId") UUID sessionId);

    @Modifying
    @Query(value = """
        UPDATE practice_session
        SET last_heartbeat_at = CURRENT_TIMESTAMP
        WHERE id = :sessionId AND status = 'IN_PROGRESS'
        """, nativeQuery = true)
    void touchHeartbeat(@Param("sessionId") UUID sessionId);

    @Query(value = """
        SELECT *
        FROM practice_session
        WHERE status = 'IN_PROGRESS'
          AND last_heartbeat_at < :staleBefore
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<PracticeSessionJpaEntity> findStaleInProgressForUpdate(@Param("staleBefore") Instant staleBefore);

  
    // KHÔNG lọc question_complete nữa (2026-08-07).
    //
    // Luật cũ: câu bỏ dở vẫn được chấm để lấy quan sát điểm yếu, nhưng không tính vào điểm
    // phiên -- "chấm một câu dở dang theo rubric của câu đầy đủ thì chắc chắn thấp, phạt học
    // sinh vì mất mạng là sai".
    //
    // Hai lý do bỏ:
    //   1. Hồ sơ điểm yếu đã gỡ 2026-08-06, nên vế "vẫn chấm để lấy quan sát" không còn đích
    //      nào. Giờ chấm xong rồi vứt.
    //   2. Giả định "chắc chắn thấp" không đúng. Đo trên phiên thật 2026-08-07: câu bỏ dở
    //      (2 lượt, 26 giây) được 80,07 -- CAO HƠN cả hai câu hoàn tất (72,56 và 73,97).
    //
    // Và khi câu bỏ dở là câu DUY NHẤT, loại nó không bảo vệ được gì: AVG trên tập rỗng ra
    // NULL, cả phiên hiện 0 điểm. Bảo vệ khỏi bị kéo xuống mà thành xoá trắng.
    //
    // marked_invalid mới là cờ đúng vai cho "có đáng tính không" -- ValidityNode đã hạ cờ đó
    // cho câu quá ngắn hoặc lạc đề.
    @Modifying
    @Query(value = """
        UPDATE practice_session
        SET overall_score = (
            SELECT AVG(evaluation.item_score)
            FROM practice_item_response response
            JOIN practice_item_evaluation evaluation
              ON evaluation.practice_response_id = response.id
            WHERE response.practice_session_id = :sessionId
              AND evaluation.marked_invalid = false
        )
        WHERE id = :sessionId
        """, nativeQuery = true)
    void refreshOverallScore(@Param("sessionId") UUID sessionId);

    /**
     * Khung chấm gửi xuống Python cho MỘT phiên luyện.
     *
     * <p>Từ V13 KHÔNG còn đi qua rubric. Ba khác biệt so với bản cũ:
     * <ul>
     *   <li>tiêu chí lấy thẳng từ {@code framework_criteria} thay vì {@code rubric_criterions};</li>
     *   <li>thang chấm cố định 0-100 -- Azure trả HundredMark nên đây là thang gốc, không phải
     *       một lựa chọn tuỳ tiện, và không có rubric nào để tra dải;</li>
     *   <li>chỉ trả ĐÚNG MỘT bậc -- bậc học sinh chọn -- thay vì cả thang. Luyện tập chấm
     *       "đáp ứng mô tả bậc này tới đâu", không xếp loại, nên các bậc khác không có việc gì
     *       để làm trong prompt.</li>
     * </ul>
     *
     * <p>{@code framework_version_id} suy từ chính bậc đích chứ không tra lại khung đang hoạt
     * động: bậc học sinh đã chọn thuộc một version cụ thể, dùng nó là nhất quán nhất kể cả khi
     * quản trị đổi khung giữa chừng.
     */
    @Query(value = """
        SELECT fc.id                     AS criterionId,
               fc.code                   AS criterionCode,
               0                         AS minScore,
               100                       AS maxScore,
               fc.code                   AS frameworkCode,
               fc.name                   AS frameworkName,
               fc.description            AS frameworkDescription,
               CAST(target.id AS varchar) AS targetBandId,
               target.code               AS targetBandCode,
               target.label              AS targetBandLabel,
               target.code               AS bandCode,
               target.label              AS bandLabel,
               target.result_band_order  AS bandOrder,
               fcb.descriptor            AS descriptor
        FROM practice_session session
        JOIN framework_result_bands target
          ON target.id = session.target_framework_band_id
        JOIN framework_criteria fc
          ON fc.framework_version_id = target.framework_version_id
        JOIN framework_criterion_bands fcb
          ON fcb.framework_criterion_id = fc.id
         AND fcb.framework_result_band_id = target.id
        WHERE session.id = :sessionId
        ORDER BY fc.code
        """, nativeQuery = true)
    List<CriterionFrameworkInfo> findCriteriaFrameworks(@Param("sessionId") UUID sessionId);

    @Query(value = """
        SELECT COUNT(*) AS sessionsDone,
               COALESCE(AVG(overall_score), 0) AS averageScore
        FROM practice_session
        WHERE student_id = :studentId AND status = 'COMPLETED'
        """, nativeQuery = true)
    PracticeDashboardCountsInfo findDashboardCounts(@Param("studentId") UUID studentId);

    @Query(value = """
        SELECT DISTINCT started_at::date AS d
        FROM practice_session
        WHERE student_id = :studentId AND status = 'COMPLETED'
        ORDER BY d DESC
        """, nativeQuery = true)
    List<java.time.LocalDate> findCompletedSessionDatesDesc(@Param("studentId") UUID studentId);
}
