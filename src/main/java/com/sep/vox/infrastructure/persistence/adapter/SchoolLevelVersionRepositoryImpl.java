package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.languagelevel.SchoolLevelVersion;
import com.sep.vox.domain.repository.SchoolLevelVersionRepository;
import com.sep.vox.infrastructure.persistence.mapper.SchoolLevelVersionMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataSchoolLevelVersionRepository;

@Repository
public class SchoolLevelVersionRepositoryImpl implements SchoolLevelVersionRepository {

    private final SpringDataSchoolLevelVersionRepository springDataSchoolLevelVersionRepository;

    public SchoolLevelVersionRepositoryImpl(SpringDataSchoolLevelVersionRepository springDataSchoolLevelVersionRepository) {
        this.springDataSchoolLevelVersionRepository = springDataSchoolLevelVersionRepository;
    }

    @Override
    public Optional<SchoolLevelVersion> findById(UUID id) {
        return springDataSchoolLevelVersionRepository.findById(id)
            .map(SchoolLevelVersionMapper::toDomain);
    }

    @Override
    public Optional<SchoolLevelVersion> findBySchoolLevelIdAndVersion(UUID schoolLevelId, int version) {
        return springDataSchoolLevelVersionRepository.findBySchoolLevelIdAndVersion(schoolLevelId, version)
            .map(SchoolLevelVersionMapper::toDomain);
    }

    @Override
    public SchoolLevelVersion save(SchoolLevelVersion version) {
        var entity = SchoolLevelVersionMapper.toJpa(version);
        var saved = springDataSchoolLevelVersionRepository.save(entity);
        return SchoolLevelVersionMapper.toDomain(saved);
    }
    
}
