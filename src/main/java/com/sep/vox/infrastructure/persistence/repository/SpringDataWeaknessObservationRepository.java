package com.sep.vox.infrastructure.persistence.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.application.query.dto.WeaknessFrequencyInfo;
import com.sep.vox.infrastructure.persistence.entity.WeaknessObservationJpaEntity;

public interface SpringDataWeaknessObservationRepository
        extends JpaRepository<WeaknessObservationJpaEntity, UUID> {

    boolean existsBySourceEvaluationIdAndFrameworkCriterionIdAndSubAttributeAndEvidenceSpan(
        UUID sourceEvaluationId,
        UUID frameworkCriterionId,
        String subAttribute,
        String evidenceSpan
    );

    /**
     * Tần suất mỗi nhãn lỗi, kèm tần suất đã PHÂN RÃ theo số lần chấm đã trôi qua.
     *
     * <p>Trước đây trả về hai con số đếm trên hai cửa sổ cứng (60 ngày và 14 ngày), rồi Java
     * chuẩn hoá và trộn bằng hai trọng số 0,6/0,4. Cách đó là một đường phân rã HAI BẬC trá
     * hình, và có vách đứng vô lý: lỗi 13 ngày tuổi tính đủ 1,00 còn lỗi 15 ngày chỉ 0,38.
     *
     * <p>Nay là một đường phân rã mượt, và đếm theo <b>LẦN CHẤM</b> chứ không theo ngày:
     *
     * <ul>
     *   <li>Job làm mới chạy sau MỖI buổi luyện ({@code onPracticeSessionEnded}); vòng quét
     *       60 giây chỉ là lưới an toàn. Nhịp tự nhiên của hệ là buổi, không phải ngày.</li>
     *   <li>Dùng lại {@code alpha} của EMA điểm yếu ngay cạnh đó -- cả hệ chỉ còn MỘT tốc độ
     *       quên, không thêm hằng số nào.</li>
     *   <li>Nghỉ ba tháng không luyện thì phân rã theo ngày sẽ "quên", nhưng vắng mặt không
     *       phải bằng chứng của tiến bộ.</li>
     * </ul>
     *
     * <p>{@code DENSE_RANK} chia theo (học sinh, tiêu chí) chứ KHÔNG theo nhãn: các nhãn của
     * cùng một tiêu chí phải dùng chung một mốc thời gian thì so sánh với nhau mới có nghĩa.
     * Chia thêm theo nhãn thì nhãn nào cũng có một quan sát hạng 1, và độ mới hết phân biệt
     * được gì.
     *
     * <p>Cột {@code frequency} thô vẫn giữ -- nó là ngưỡng lọc nhiễu
     * ({@code minimumSubAttributeFrequency}), nơi "đã sai 3 lần" cần một con số đếm thật chứ
     * không phải một tổng phân rã.
     *
     * @param decayBase {@code 1 - alpha}. Trọng số của một quan sát cách đây k lần chấm là
     *     {@code decayBase^k}.
     */
    @Query(value = """
        SELECT student_id AS studentId,
               framework_criterion_id AS frameworkCriterionId,
               sub_attribute AS subAttribute,
               COUNT(*)::int AS frequency,
               SUM(POWER(CAST(:decayBase AS double precision), occasion_rank - 1))
                   AS decayedFrequency
        FROM (
            SELECT student_id,
                   framework_criterion_id,
                   sub_attribute,
                   DENSE_RANK() OVER (
                       PARTITION BY student_id, framework_criterion_id
                       ORDER BY observed_at DESC
                   ) AS occasion_rank
            FROM weakness_observation
            WHERE student_id IN :studentIds
              AND observed_at >= :windowStart
        ) ranked
        GROUP BY student_id, framework_criterion_id, sub_attribute
        """, nativeQuery = true)
    List<WeaknessFrequencyInfo> findWeaknessFrequencies(
        @Param("studentIds") List<UUID> studentIds,
        @Param("windowStart") Instant windowStart,
        @Param("decayBase") double decayBase
    );
}
