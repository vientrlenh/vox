package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.school.SchoolGrade;
import com.sep.vox.domain.repository.SchoolGradeRepository;
import com.sep.vox.infrastructure.persistence.mapper.SchoolGradeMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataSchoolGradeRepository;

@Repository
public class SchoolGradeRepositoryImpl implements SchoolGradeRepository {

    private final SpringDataSchoolGradeRepository springDataSchoolGradeRepository;

    public SchoolGradeRepositoryImpl(SpringDataSchoolGradeRepository springDataSchoolGradeRepository) {
        this.springDataSchoolGradeRepository = springDataSchoolGradeRepository;
    }

    @Override
    public Optional<SchoolGrade> findById(UUID id) {
        return springDataSchoolGradeRepository.findById(id)
            .map(SchoolGradeMapper::toDomain);
    }

    @Override
    public Optional<SchoolGrade> findBySchoolIdAndCode(UUID schoolId, String code) {
        return springDataSchoolGradeRepository.findBySchoolIdAndCode(schoolId, code)
            .map(SchoolGradeMapper::toDomain);
    }

    @Override
    public List<SchoolGrade> findBySchoolIdAndCodeIn(UUID schoolId, Collection<String> codes) {
        if (schoolId == null || codes == null || codes.isEmpty()) {
            return List.of();
        }
        return springDataSchoolGradeRepository.findBySchoolIdAndCodeIn(schoolId, codes)
            .stream()
            .map(SchoolGradeMapper::toDomain)
            .toList();
    }

    @Override
    public SchoolGrade save(SchoolGrade grade) {
        var entity = SchoolGradeMapper.toJpa(grade);
        var saved = springDataSchoolGradeRepository.save(entity);
        return SchoolGradeMapper.toDomain(saved);
    }
    
}
