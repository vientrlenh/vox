package com.sep.vox.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.InterestQuizItemJpaEntity;

public interface SpringDataInterestQuizItemRepository
        extends JpaRepository<InterestQuizItemJpaEntity, UUID> {

    List<InterestQuizItemJpaEntity> findByIdAndActiveTrue(UUID id);

    // Lấy nguyên kho (giới hạn mềm bằng Top50) chứ không phải Top7: bộ chọn cân bằng cần
    // nhiều ứng viên hơn số câu sẽ hỏi thì mới chọn được bộ phủ đều các chiều.
    List<InterestQuizItemJpaEntity> findTop50ByActiveTrueOrderById();

    List<InterestQuizItemJpaEntity> findTop50ByStudentIdAndActiveTrueOrderById(UUID studentId);

    /**
     * Có bộ câu CÒN DÙNG ĐƯỢC của học sinh này không.
     *
     * <p>Phải kèm {@code AndActiveTrue}: từ khi có nút làm lại quiz, bộ cũ bị tắt {@code active}
     * chứ không bị xoá (đáp án đã nộp còn trỏ tới id của nó). Chỉ hỏi theo {@code studentId} thì
     * sau khi làm lại, hàm này vẫn trả {@code true} trong khi mọi câu đều đã tắt -- học sinh
     * nhận về danh sách RỖNG và onboarding treo.
     *
     * <p>Chưa ai gọi làm lại thì mọi dòng đều {@code active = true}, nên hành vi y hệt bản cũ.
     */
    boolean existsByStudentIdAndActiveTrue(UUID studentId);

    /**
     * Tắt bộ câu hiện có của học sinh, KHÔNG xoá.
     *
     * <p>Xoá thì đáp án đã nộp mất chỗ tham chiếu và không truy lại được vì sao hồ sơ sở thích
     * ra như vậy. Tắt thì {@code findActiveQuizItem} không thấy nữa, nhưng dữ liệu còn nguyên.
     */
    @Modifying
    @Query("UPDATE InterestQuizItemJpaEntity i SET i.active = false "
        + "WHERE i.studentId = :studentId AND i.active = true")
    int deactivateByStudentId(@Param("studentId") UUID studentId);
}
