package com.sep.vox.infrastructure.persistence.adapter;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.SchoolClassDependencyRepository;
import com.sep.vox.domain.repository.SchoolClassUserRepository;

@Repository
@RequiredArgsConstructor
public class SchoolClassDependencyRepositoryImpl implements SchoolClassDependencyRepository {

    private final SchoolClassUserRepository schoolClassUserRepository;
    private final AssessmentPolicyRepository assessmentPolicyRepository;

    @Override
    public boolean existsDependencyBySchoolClassId(UUID schoolClassId) {
        return schoolClassUserRepository.existsBySchoolClassId(schoolClassId)
                || assessmentPolicyRepository.existsBySchoolClassId(schoolClassId);
    }
}
