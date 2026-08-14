package com.sep.vox.infrastructure.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.FrameworkVersionJpaEntity;

import jakarta.persistence.LockModeType;

public interface SpringDataFrameworkVersionRepository extends JpaRepository<FrameworkVersionJpaEntity, UUID> {
    boolean existsByFrameworkId(UUID frameworkId);
    Optional<FrameworkVersionJpaEntity> findByCode(String code);
    Optional<FrameworkVersionJpaEntity> findByName(String name);
    // Nhận input đã được Impl uppercase sẵn; entity-side vẫn bọc UPPER() để phòng dữ liệu cũ lỡ không đồng nhất case.
    @Query("SELECT v FROM FrameworkVersionJpaEntity v WHERE UPPER(v.code) IN :codes")
    List<FrameworkVersionJpaEntity> findByCodeIn(@Param("codes") Collection<String> codes);
    List<FrameworkVersionJpaEntity> findByNameIn(Collection<String> names);
    Page<FrameworkVersionJpaEntity> findByFrameworkId(UUID frameworkId, Pageable pageable);
    List<FrameworkVersionJpaEntity> findByFrameworkIdAndStatus(UUID frameworkId, String status);
    Page<FrameworkVersionJpaEntity> findByFrameworkIdAndStatus(UUID frameworkId, String status, Pageable pageable);
    Optional<FrameworkVersionJpaEntity> findByFrameworkIdAndVersion(UUID frameworkId, int version);

    /**
     * Mỗi khung đánh giá còn hiệu lực kèm BẢN MỚI NHẤT đã ban hành của nó -- nguồn cho ô chọn
     * khung ở màn luyện tập.
     *
     * <p>{@code DISTINCT ON (f.id)} cộng {@code ORDER BY f.id, fv.version DESC} cho đúng một
     * dòng mỗi khung, là bản version cao nhất. Cùng bộ điều kiện hiệu lực với
     * {@code findActiveVersionId} -- hai đường phải nhất quán, nếu không màn hình liệt kê ra
     * một khung mà chỗ khác lại không chọn nó.
     *
     * <p>Khác {@code findActiveVersionId} ở chỗ: hàm kia trả về ĐÚNG MỘT khung cho cả hệ, còn
     * hàm này trả về TẤT CẢ để học sinh tự chọn.
     */
    @Query(value = """
        SELECT DISTINCT ON (f.id)
               fv.id            AS versionId,
               fv.code          AS versionCode,
               fv.version       AS versionNumber,
               f.id             AS frameworkId,
               f.code           AS frameworkCode,
               f.name           AS frameworkName,
               f.description    AS frameworkDescription
        FROM framework_versions fv
        JOIN frameworks f ON f.id = fv.framework_id AND f.is_active = true
        WHERE fv.status = 'PUBLISHED'
          AND fv.effective_from <= CURRENT_TIMESTAMP
          AND (fv.effective_to IS NULL OR fv.effective_to >= CURRENT_TIMESTAMP)
        ORDER BY f.id, fv.version DESC
        """, nativeQuery = true)
    List<ActiveFrameworkProjection> findActiveFrameworks();

    interface ActiveFrameworkProjection {
        UUID getVersionId();
        String getVersionCode();
        Integer getVersionNumber();
        UUID getFrameworkId();
        String getFrameworkCode();
        String getFrameworkName();
        String getFrameworkDescription();
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM FrameworkVersionJpaEntity v WHERE v.id = :id")
    Optional<FrameworkVersionJpaEntity> findByIdForUpdate(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE FrameworkVersionJpaEntity v SET v.status = :status WHERE v.id = :id")
    int updateStatus(@Param("id") UUID id, @Param("status") String status);

    /**
     * Bản khung đánh giá đang hiệu lực của TOÀN HỆ, dùng cho luyện tập.
     *
     * <p>Vì sao không đi qua {@code assessment_policies} như đường thi: policy được chọn theo
     * lớp học sinh đang thuộc (ưu tiên lớp &gt; khối &gt; trường). Ba hệ quả không muốn ở luyện
     * tập -- trường chưa cấu hình policy thì học sinh không vào được phiên; hai học sinh cùng
     * trường khác lớp luyện dưới hai thang khác nhau; và một học sinh thuộc nhiều lớp active
     * thì policy nào thắng là chuyện của thứ tự chứ không phải của ý định.
     *
     * <p>{@code frameworks} không gắn trường hay ngôn ngữ nên đây là khái niệm toàn hệ -- tra
     * một câu là ra, không có nhập nhằng phạm vi.
     *
     * <p>{@code :code} cho phép chỉ định thẳng qua config
     * ({@code app.practice.framework-version-code}); để null thì tự chọn bản đang hoạt động.
     * Có đường này để trường nhiều khung không phải phụ thuộc vào cờ {@code is_active}.
     */
    @Query(value = """
        SELECT fv.id
        FROM framework_versions fv
        JOIN frameworks f ON f.id = fv.framework_id AND f.is_active = true
        WHERE fv.status = 'PUBLISHED'
          AND fv.effective_from <= CURRENT_TIMESTAMP
          AND (fv.effective_to IS NULL OR fv.effective_to >= CURRENT_TIMESTAMP)
          AND (CAST(:code AS varchar) IS NULL OR fv.code = CAST(:code AS varchar))
        ORDER BY fv.version DESC
        LIMIT 1
        """, nativeQuery = true)
    Optional<UUID> findActiveVersionId(@Param("code") String code);
}
