package com.sep.vox.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.framework.FrameworkResultBand;
import com.sep.vox.domain.model.personalization.LearnerProfile;
import com.sep.vox.domain.repository.LearnerProfileRepository;
import com.sep.vox.infrastructure.persistence.mapper.FrameworkResultBandMapper;
import com.sep.vox.infrastructure.persistence.mapper.personalization.LearnerProfileMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataLearnerProfileRepository;

@Repository
@RequiredArgsConstructor
public class LearnerProfileRepositoryImpl
        implements LearnerProfileRepository {

    private final SpringDataLearnerProfileRepository profileRepository;

    @Override
    public Optional<LearnerProfile> findCurrent(UUID studentId) {
        return profileRepository
            .findByStudentId(studentId)
            .map(LearnerProfileMapper::toDomain);
    }

    @Override
    public Optional<LearnerProfile> findCurrentForUpdate(UUID studentId) {
        return profileRepository
            .findWithLockByStudentId(studentId)
            .map(LearnerProfileMapper::toDomain);
    }

    @Override
    public LearnerProfile save(LearnerProfile profile) {
        return LearnerProfileMapper.toDomain(
            profileRepository.save(LearnerProfileMapper.toJpa(profile))
        );
    }

    @Override
    public List<Integer> findFrameworkBandCount(UUID frameworkVersionId) {
        return profileRepository.findFrameworkBandCount(frameworkVersionId);
    }

    @Override
    public List<FrameworkResultBand> findFrameworkBandLadder(UUID frameworkVersionId) {
        return profileRepository.findFrameworkBandLadder(frameworkVersionId).stream()
            .map(FrameworkResultBandMapper::toDomain)
            .toList();
    }
}
