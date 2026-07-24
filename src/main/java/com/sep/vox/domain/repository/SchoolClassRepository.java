package com.sep.vox.domain.repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.school.SchoolClass;
import com.sep.vox.domain.model.school.SchoolClassStatus;

public interface SchoolClassRepository {
    Optional<SchoolClass> findById(UUID id);
    PageResult<SchoolClass> findBySchoolId(UUID schoolId, int pageNumber, int size);
    PageResult<SchoolClass> findBySchoolId(UUID schoolId, String search, SchoolClassStatus status,
            UUID languageId, UUID schoolGradeId, int pageNumber, int size);
    PageResult<SchoolClass> findByUserId(UUID schoolId, UUID userId, SchoolClassStatus status, int pageNumber, int size);
    Optional<SchoolClass> findBySchoolIdAndCode(UUID schoolId, String code);
    Optional<SchoolClass> findBySchoolIdAndName(UUID schoolId, String name);
    List<SchoolClass> findBySchoolIdAndCodeIn(UUID schoolId, Collection<String> codes);
    List<SchoolClass> findBySchoolIdAndNameIn(UUID schoolId, Collection<String> names);
    SchoolClass save(SchoolClass schoolClass);
    int updateMutableFields(UUID id, UUID schoolId, String name, boolean nameProvided,
            String description, boolean descriptionProvided, SchoolClassStatus status, boolean statusProvided,
            OffsetDateTime updatedAt, UUID updatedBy);
    void deleteById(UUID id);
    List<SchoolClass> findBySchoolIdIn(Collection<UUID> schoolIds, int pageNumber, int size);

    List<SchoolClass>findAllById(List<UUID> schoolIds);

    boolean existsBySchoolGradeId(UUID schoolGradeId);

    boolean existsBySchoolIdAndStatus(UUID schoolId, String status);

    /** Xóa mềm (ARCHIVED) tất cả lớp thuộc một năm học, bỏ qua lớp đã ARCHIVED. Trả về số dòng bị ảnh hưởng. */
    int archiveByGradeId(UUID schoolGradeId, OffsetDateTime updatedAt, UUID updatedBy);
}
