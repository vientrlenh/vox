package com.sep.vox.infrastructure.persistence.adapter;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.common.PageRequest;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.school.SchoolClass;
import com.sep.vox.domain.model.school.SchoolClassStatus;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.infrastructure.persistence.mapper.SchoolClassMapper;
import com.sep.vox.infrastructure.persistence.mapper.SchoolMapper;
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
    public void deleteById(UUID id) {
        springDataSchoolClassRepository.deleteById(id);
    }

    @Override
    public PageResult<SchoolClass> findBySchoolId(UUID schoolId, PageRequest pageRequest) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findBySchoolId'");
    }

    @Override
    public Optional<SchoolClass> findBySchoolIdAndCode(UUID schoolId, String code) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findBySchoolIdAndCode'");
    }

    @Override
    public List<SchoolClass> findBySchoolIdAndCodeIn(UUID schoolId, Collection<String> codes) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findBySchoolIdAndCodeIn'");
    }

    @Override
    public List<SchoolClass> findBySchoolIdAndName(UUID schoolId, String name) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findBySchoolIdAndName'");
    }

    @Override
    public List<SchoolClass> findBySchoolIdAndLanguageIdAndTargetSchoolLevelVersionId(UUID schoolId, UUID languageId,
            UUID levelId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findBySchoolIdAndLanguageIdAndTargetSchoolLevelVersionId'");
    }

    @Override
    public int updateMutableFields(UUID id, UUID schoolId, UUID languageId, String name, String description,
            UUID targetSchoolLevelVersionId, SchoolClassStatus status, OffsetDateTime updatedAt, UUID updatedBy) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'updateMutableFields'");
    }

    @Override
    public List<SchoolClass> findBySchoolIdIn(Collection<UUID> schoolIds, int page, int size) {
        var fromRow = (page - 1) * size + 1;
        var toRow = page * size;
        return springDataSchoolClassRepository.findBySchoolIdIn(schoolIds, fromRow, toRow)
            .stream()
            .map(SchoolClassMapper::toDomain)
            .toList();
    }
    
}
