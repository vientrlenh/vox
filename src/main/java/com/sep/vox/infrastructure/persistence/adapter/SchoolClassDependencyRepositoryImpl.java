package com.sep.vox.infrastructure.persistence.adapter;

import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.repository.SchoolClassDependencyRepository;
import com.sep.vox.domain.repository.SchoolClassUserRepository;
import com.sep.vox.infrastructure.persistence.repository.SpringDataRubricApplicabilityRepository;

@Repository
public class SchoolClassDependencyRepositoryImpl implements SchoolClassDependencyRepository {

    private final SchoolClassUserRepository schoolClassUserRepository;
    private final SpringDataRubricApplicabilityRepository springDataRubricApplicabilityRepository;

    public SchoolClassDependencyRepositoryImpl(
            SchoolClassUserRepository schoolClassUserRepository,
            SpringDataRubricApplicabilityRepository springDataRubricApplicabilityRepository) {
        this.schoolClassUserRepository = schoolClassUserRepository;
        this.springDataRubricApplicabilityRepository = springDataRubricApplicabilityRepository;
    }

    @Override
    public boolean existsDependencyBySchoolClassId(UUID schoolClassId) {
        return schoolClassUserRepository.existsBySchoolClassId(schoolClassId)
            || springDataRubricApplicabilityRepository.existsBySchoolClassId(schoolClassId);
    }
}
