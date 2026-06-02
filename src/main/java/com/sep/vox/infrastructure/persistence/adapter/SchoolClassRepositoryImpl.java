package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.common.PageRequest;
import com.sep.vox.domain.common.PageResult;
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
    public PageResult<SchoolClass> findBySchoolId(UUID schoolId, PageRequest pageRequest) {
        var pageable = org.springframework.data.domain.PageRequest.of(pageRequest.page() - 1, pageRequest.size());
        var page = springDataSchoolClassRepository.findBySchoolId(schoolId, pageable);
        return new PageResult<>(
            page.getContent().stream()
                .map(SchoolClassMapper::toDomain)
                .toList(),
            page.getNumber() + 1,
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages()
        );
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
    public List<SchoolClass> findBySchoolIdAndCodeIn(UUID schoolId, Collection<String> codes) {
        return springDataSchoolClassRepository.findBySchoolIdAndCodeIn(schoolId, codes)
            .stream()
            .map(SchoolClassMapper::toDomain)
            .toList();
    }

    @Override
    public List<SchoolClass> findBySchoolIdAndName(UUID schoolId, String name) {
       return springDataSchoolClassRepository.findBySchoolIdAndName(schoolId, name)
            .stream()
            .map(SchoolClassMapper::toDomain)
            .toList();
    }

    @Override
    public List<SchoolClass> findBySchoolIdAndLanguageIdAndTargetSchoolLevelVersionId(UUID schoolId, UUID languageId, UUID levelId) {
        return springDataSchoolClassRepository.findBySchoolIdAndLanguageIdAndTargetSchoolLevelVersionId(schoolId, languageId, levelId)
            .stream()
            .map(SchoolClassMapper::toDomain)
            .toList();
    }

    @Override
    public void deleteById(UUID id) {
        springDataSchoolClassRepository.deleteById(id);
    }
    
}
