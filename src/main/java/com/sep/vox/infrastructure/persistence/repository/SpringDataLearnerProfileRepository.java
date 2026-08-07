package com.sep.vox.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import com.sep.vox.infrastructure.persistence.entity.FrameworkResultBandJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.LearnerProfileJpaEntity;

public interface SpringDataLearnerProfileRepository
        extends JpaRepository<LearnerProfileJpaEntity, UUID> {

    Optional<LearnerProfileJpaEntity> findTopByStudentIdOrderByVersionDesc(UUID studentId);

    /** Khoá FOR SHARE bản mới nhất trước khi nối thêm version -- tránh hai request cùng ghi đè nhau. */
    @Lock(LockModeType.PESSIMISTIC_READ)
    Optional<LearnerProfileJpaEntity> findTopWithLockByStudentIdOrderByVersionDesc(UUID studentId);






    /**
     * Số bậc của thang năng lực (= bậc cao nhất trong framework version truyền vào).
     *
     * Cần vì trước đây code giả định cứng thang 6 bậc kiểu VSTEP (`Math.min(6, ...)` rải rác).
     * Đổi trường sang CEFR/IELTS thì con số 6 đó sai mà KHÔNG nổ -- chỉ lặng lẽ kẹp trần sai.
     * Đọc từ dữ liệu thay vì đoán.
     *
     * Từ V13 nhận thẳng frameworkVersionId thay vì suy từ assessment policy theo lớp của học
     * sinh. Hai hàm này và {@link #findFrameworkBandLadder} PHẢI được gọi với cùng một
     * frameworkVersionId -- caller chịu trách nhiệm, xem PracticeFrameworkResolver.
     */
    @Query(value = """
        SELECT MAX(band.result_band_order)
        FROM framework_result_bands band
        WHERE band.framework_version_id = :frameworkVersionId
        """, nativeQuery = true)
    List<Integer> findFrameworkBandCount(
        @Param("frameworkVersionId") UUID frameworkVersionId
    );

    /**
     * Toàn bộ thang bậc (thứ tự, mã, mô tả) của framework đang áp cho học sinh -- gửi xuống
     * Python để dựng ladder mô tả bậc trong prompt chấm câu hỏi, thay cho hằng số viết cứng
     * `BAC_1..BAC_6` bằng tiếng Anh vốn khoá hệ thống vào VSTEP.
     *
     * Không LIMIT 1 như hai hàm trên vì cần cả thang; policy được chọn bằng subquery theo đúng
     * thứ tự ưu tiên lớp > khối > trường để nhất quán với chúng.
     *
     * Trả thẳng FrameworkResultBandJpaEntity (SELECT band.*) thay vì một projection riêng: bậc
     * thang đã có sẵn model/entity/mapper ở gói framework, dựng thêm kiểu riêng chỉ để bớt vài
     * cột là nhân đôi khái niệm. Query phải nằm ở đây (không gọi FrameworkResultBandRepository)
     * vì luồng này chỉ có studentId, và một adapter chỉ được phép dùng đúng một repo.
     */
    @Query(value = """
        SELECT band.*
        FROM framework_result_bands band
        WHERE band.framework_version_id = :frameworkVersionId
        ORDER BY band.result_band_order
        """, nativeQuery = true)
    List<FrameworkResultBandJpaEntity> findFrameworkBandLadder(
        @Param("frameworkVersionId") UUID frameworkVersionId
    );
}
