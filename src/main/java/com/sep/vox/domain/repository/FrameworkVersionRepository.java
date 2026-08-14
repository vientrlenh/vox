package com.sep.vox.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;

public interface FrameworkVersionRepository {
    Optional<FrameworkVersion> findById(UUID id);
    Optional<FrameworkVersion> findByCode(String code);
    Optional<FrameworkVersion> findByName(String name);
    List<FrameworkVersion> findByIdIn(Collection<UUID> ids);
    List<FrameworkVersion> findByCodeIn(Collection<String> codes);
    List<FrameworkVersion> findByNameIn(Collection<String> names);
    Optional<FrameworkVersion> findByIdForUpdate(UUID id);
    PageResult<FrameworkVersion> findByFrameworkId(UUID frameworkId, int pageNumber, int size);
    List<FrameworkVersion> findByFrameworkIdAndStatus(UUID frameworkId, FrameworkVersionStatus status);
    PageResult<FrameworkVersion> findByFrameworkIdAndStatus(UUID frameworkId, FrameworkVersionStatus status, int pageNumber, int size);
    Optional<FrameworkVersion> findByFrameworkIdAndVersion(UUID frameworkId, int version);
    FrameworkVersion save(FrameworkVersion version);
    int updateStatus(UUID id, FrameworkVersionStatus status);
    void deleteById(UUID id);
    boolean existsByFrameworkId(UUID frameworkId);

    /**
     * Bản khung đánh giá đang hiệu lực của toàn hệ -- nguồn framework cho LUYỆN TẬP.
     *
     * <p>Khác đường thi (tra qua assessment policy theo lớp học sinh): luyện tập không phụ
     * thuộc policy, xem V13 và {@code SpringDataFrameworkVersionRepository.findActiveVersionId}.
     *
     * @param code chỉ định thẳng một mã version; null = tự chọn bản đang hoạt động
     */
    Optional<UUID> findActiveVersionId(String code);

    /**
     * Mọi khung đánh giá còn hiệu lực, mỗi khung kèm bản ĐÃ BAN HÀNH mới nhất -- nguồn cho ô
     * chọn khung trước khi chọn bậc ở màn luyện tập.
     *
     * <p>Cùng bộ điều kiện hiệu lực với {@link #findActiveVersionId}, chỉ khác là không thu về
     * một khung duy nhất.
     */
    List<ActiveFramework> findActiveFrameworks();

    /** Một khung còn hiệu lực kèm bản mới nhất của nó. */
    record ActiveFramework(
        UUID frameworkId,
        String frameworkCode,
        String frameworkName,
        String frameworkDescription,
        UUID versionId,
        String versionCode,
        int versionNumber
    ) {
    }
}
