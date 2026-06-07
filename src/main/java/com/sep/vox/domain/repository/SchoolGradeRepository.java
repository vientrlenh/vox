package com.sep.vox.domain.repository;

import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageRequest;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.school.SchoolGrade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SchoolGradeRepository {
    Optional<SchoolGrade> findById(UUID id);
    Optional<SchoolGrade> findBySchoolIdAndCode(UUID schoolId, String code);
    SchoolGrade save(SchoolGrade grade);

    boolean existsBySchoolIdAndCode(UUID schoolId, String code);

    Optional<SchoolGrade> findByIdForUpdate(UUID schoolGradeId, UUID schoolId);

    PageResult<SchoolGrade> findAllBySchoolId(UUID schoolId, PageRequest pageRequest);

    void deleteById(UUID id);

    Optional<SchoolGrade> findByIdForDelete(UUID schoolGradeId);


}
