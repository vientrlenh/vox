package com.sep.vox.domain.repository;

import java.time.LocalDate;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.school.SchoolGrade;

public interface SchoolGradeRepository {
    Optional<SchoolGrade> findById(UUID id);
    Optional<SchoolGrade> findBySchoolIdAndCode(UUID schoolId, String code);
    Optional<SchoolGrade> findBySchoolIdAndName(UUID schoolId, String name);
    SchoolGrade save(SchoolGrade grade);

    List<SchoolGrade> findAllById(List<UUID> gradeIds);

    // gradeLevelId nay trỏ tới catalog toàn cục, nên mã lớp chỉ duy nhất trong phạm vi
    // (schoolId, gradeLevelId) -- phải truyền kèm schoolId, khác bản cũ chỉ cần levelId.
    boolean existsBySchoolIdAndGradeLevelIdAndCode(UUID schoolId, UUID gradeLevelId, String code);
    Optional<SchoolGrade> findBySchoolIdAndGradeLevelIdAndCode(UUID schoolId, UUID gradeLevelId, String code);
    List<SchoolGrade> findBySchoolIdAndCodeIn(UUID schoolId, Collection<String> codes);
    List<SchoolGrade> findBySchoolIdAndNameIn(UUID schoolId, Collection<String> names);

    // Quét mọi trường: dùng để chặn xóa một khối trong catalog dùng chung.
    boolean existsByGradeLevelId(UUID gradeLevelId);

    boolean existsByGradeLevelIdAndStatusNot(UUID gradeLevelId, String status);

    PageResult<SchoolGrade> findAllBySchoolId(UUID schoolId, UUID gradeLevelId, String status, int pageNumber, int size);

    boolean existsBySchoolIdAndStatus(UUID schoolId, String status);
    int updateSchoolGradeAtomic(UUID id, String name, String description, LocalDate startDate, LocalDate endDate, Instant now, UUID updatedBy);
    void deleteById(UUID schoolGradeId);
    List<SchoolGrade> findByIdIn(Collection<UUID> ids);
}
