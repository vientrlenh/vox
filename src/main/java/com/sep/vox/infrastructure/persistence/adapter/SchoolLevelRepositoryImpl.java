package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.languagelevel.SchoolLevel;
import com.sep.vox.domain.repository.SchoolLevelRepository;
import com.sep.vox.infrastructure.persistence.mapper.SchoolLevelMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataSchoolLevelRepository;

@Repository
public class SchoolLevelRepositoryImpl implements SchoolLevelRepository {

    private final SpringDataSchoolLevelRepository springDataSchoolLevelRepository;

    public SchoolLevelRepositoryImpl(SpringDataSchoolLevelRepository springDataSchoolLevelRepository) {
        this.springDataSchoolLevelRepository = springDataSchoolLevelRepository;
    }

    @Override
    public Optional<SchoolLevel> findById(UUID id) {
        return springDataSchoolLevelRepository.findById(id)
            .map(SchoolLevelMapper::toDomain);
    }

    @Override
    public SchoolLevel save(SchoolLevel schoolLevel) {
        var entity = SchoolLevelMapper.toJpa(schoolLevel);
        var saved = springDataSchoolLevelRepository.save(entity);
        return SchoolLevelMapper.toDomain(saved);
    }
    
}
