package com.sep.vox.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.schoolclass.SchoolClass;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.infrastructure.persistence.mapper.SchoolClassMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataSchoolClassRepository;

@Repository
public class SchoolClassRepositoryImpl implements SchoolClassRepository {

    private final SpringDataSchoolClassRepository springDataSchoolClassRepository;

    public SchoolClassRepositoryImpl(SpringDataSchoolClassRepository springDataSchoolClassRepository) {
        this.springDataSchoolClassRepository = springDataSchoolClassRepository;
    }

    @Override
    public Optional<SchoolClass> findById(UUID id) {
        return springDataSchoolClassRepository.findById(id)
            .map(SchoolClassMapper::toDomain);
    }

    @Override
    public SchoolClass save(SchoolClass schoolClass) {
        var entity = SchoolClassMapper.toJpa(schoolClass);
        var saved = springDataSchoolClassRepository.save(entity);
        return SchoolClassMapper.toDomain(saved);
    }

    @Override
    public Optional<SchoolClass> findBySchoolIdAndCode(UUID schoolId, String code) {
        return springDataSchoolClassRepository.findBySchoolIdAndCode(schoolId, code)
            .map(SchoolClassMapper::toDomain);
    }

    @Override
    public List<SchoolClass> findBySchoolIdAndName(UUID schoolId, String name) {
       return springDataSchoolClassRepository.findBySchoolIdAndName(schoolId, name)
            .stream()
            .map(SchoolClassMapper::toDomain)
            .toList();
    }

    @Override
    public List<SchoolClass> findBySchoolIdAndLanguageIdAndLevelId(UUID schoolId, UUID languageId, UUID levelId) {
        return springDataSchoolClassRepository.findBySchoolIdAndLanguageIdAndLevelId(schoolId, languageId, levelId)
            .stream()
            .map(SchoolClassMapper::toDomain)
            .toList();
    }
    
}
