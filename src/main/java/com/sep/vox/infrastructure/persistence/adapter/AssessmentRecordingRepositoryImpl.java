package com.sep.vox.infrastructure.persistence.adapter;

import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentRecordingJpaEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.assessment.AssessmentRecording;
import com.sep.vox.domain.model.assessment.AssessmentRequiredStreamType;
import com.sep.vox.domain.repository.AssessmentRecordingRepository;
import com.sep.vox.infrastructure.persistence.mapper.AssessmentRecordingMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataAssessmentRecordingRepository;

@Repository
@RequiredArgsConstructor
public class AssessmentRecordingRepositoryImpl implements AssessmentRecordingRepository {

    private final SpringDataAssessmentRecordingRepository springDataAssessmentRecordingRepository;

    @Override
    public List<AssessmentRecording> findByAssessmentSessionId(UUID assessmentSessionId) {
        return springDataAssessmentRecordingRepository.findByAssessmentSessionId(assessmentSessionId)
            .stream()
            .map(AssessmentRecordingMapper::toDomain)
            .toList();
    }

    @Override
    public Optional<AssessmentRecording> findByAssessmentSessionIdAndStreamTypeAndSource(
        UUID assessmentSessionId, AssessmentRequiredStreamType streamType, String source
    ) {
        return springDataAssessmentRecordingRepository
            .findByAssessmentSessionIdAndStreamTypeAndSource(assessmentSessionId, streamType.name(), source)
            .map(AssessmentRecordingMapper::toDomain);
    }

    @Override
    public AssessmentRecording save(AssessmentRecording recording) {
        AssessmentRecordingJpaEntity saved = springDataAssessmentRecordingRepository.save(AssessmentRecordingMapper.toJpa(recording));
        return AssessmentRecordingMapper.toDomain(saved);
    }

    @Override
    public void deleteByAssessmentSessionId(UUID assessmentSessionId) {
        springDataAssessmentRecordingRepository.deleteByAssessmentSessionId(assessmentSessionId);
    }
}
