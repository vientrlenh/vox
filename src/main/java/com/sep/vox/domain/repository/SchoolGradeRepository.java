package com.sep.vox.domain.repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageRequest;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.school.SchoolGrade;
import com.sep.vox.domain.model.school.SchoolGradeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SchoolGradeRepository {
    Optional<SchoolGrade> findById(UUID id);
    Optional<SchoolGrade> findBySchoolIdAndCode(UUID schoolId, String code);
    SchoolGrade save(SchoolGrade grade);

    boolean existsBySchoolIdAndCode(UUID schoolId, String code);

    PageResult<SchoolGrade> findAllBySchoolId(UUID schoolId, PageRequest pageRequest);

    void deleteByIdAndSchoolId(UUID id, UUID schoolId);

    Optional<SchoolGrade> findByIdForDelete(UUID id,UUID schoolId);

    boolean existsBySchoolIdAndStatus(UUID schoolId, String status);
    int updateSchoolGradeAtomic(UUID id, String name, String description, LocalDate startDate, LocalDate endDate, OffsetDateTime now, UUID updatedBy);

}
