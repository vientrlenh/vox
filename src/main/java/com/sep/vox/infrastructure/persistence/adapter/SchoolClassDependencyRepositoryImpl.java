package com.sep.vox.infrastructure.persistence.adapter;

import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.repository.SchoolClassDependencyRepository;
import com.sep.vox.domain.repository.SchoolClassUserRepository;

@Repository
public class SchoolClassDependencyRepositoryImpl implements SchoolClassDependencyRepository {

    private final SchoolClassUserRepository schoolClassUserRepository;

    public SchoolClassDependencyRepositoryImpl(
            SchoolClassUserRepository schoolClassUserRepository) {
        this.schoolClassUserRepository = schoolClassUserRepository;
    }

    @Override
    public boolean existsDependencyBySchoolClassId(UUID schoolClassId) {
        return schoolClassUserRepository.existsBySchoolClassId(schoolClassId);
    }
}
