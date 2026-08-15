package com.sep.vox.domain.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.school.School;

public interface SchoolRepository {
    Optional<School> findById(UUID id);
    Optional<School> findByCode(String code);

    /**
     * Trả về DANH SÁCH chứ không phải Optional: tên miền KHÔNG còn là danh tính của trường, một
     * trường nhiều cơ sở dùng chung 1 tên miền và mỗi cơ sở là một School riêng. Đổi lại Optional
     * là ngày nào đó nổ NonUniqueResultException ở chỗ không liên quan.
     */
    List<School> findByDomain(String domain);
    PageResult<School> findAll(int pageNumber, int size);
    School save(School school);
    boolean existsById(UUID id);

    boolean existsByContactEmailAndIdNot(String email, UUID id);
    boolean existsByContactPhoneAndIdNot(String phone, UUID id);

    void deleteById(UUID id);

    boolean existsByIdAndIsActiveTrue(UUID schoolId);

    int updateSchoolAtomic(UUID id, String name, String description, String phone,
                           String email, String domain, String address, Integer studentCount,
                           Instant now, UUID updatedBy);
    List<School> findByIdIn(Collection<UUID> ids);
    boolean existsByCode(String code);

    long countAll();
    long countByIsActiveTrue();
}
