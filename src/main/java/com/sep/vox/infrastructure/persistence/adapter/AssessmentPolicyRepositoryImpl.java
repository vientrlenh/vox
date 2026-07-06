package com.sep.vox.infrastructure.persistence.adapter;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicy;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.infrastructure.persistence.mapper.AssessmentPolicyMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataAssessmentPolicyRepository;

@Repository
public class AssessmentPolicyRepositoryImpl implements AssessmentPolicyRepository {

    private final SpringDataAssessmentPolicyRepository springDataAssessmentPolicyRepository;

    public AssessmentPolicyRepositoryImpl(SpringDataAssessmentPolicyRepository springDataAssessmentPolicyRepository) {
        this.springDataAssessmentPolicyRepository = springDataAssessmentPolicyRepository;
    }

    @Override
    public Optional<AssessmentPolicy> findById(UUID id) {
        return springDataAssessmentPolicyRepository.findById(id).map(AssessmentPolicyMapper::toDomain);
    }

    @Override
    public AssessmentPolicy save(AssessmentPolicy policy) {
        var entity = AssessmentPolicyMapper.toJpa(policy);
        var saved = springDataAssessmentPolicyRepository.save(entity);
        return AssessmentPolicyMapper.toDomain(saved);
    }

    @Override
    public List<AssessmentPolicy> saveAll(List<AssessmentPolicy> policies) {
        var entities = policies.stream().map(AssessmentPolicyMapper::toJpa).toList();
        return springDataAssessmentPolicyRepository.saveAll(entities)
                .stream().map(AssessmentPolicyMapper::toDomain).toList();
    }

    @Override
    public PageResult<AssessmentPolicy> findAllSystemWide(String status, UUID languageId, int page, int size) {
        var pageable = PageRequest.of(page - 1, size);
        var result = springDataAssessmentPolicyRepository.findBySchoolIdIsNullAndStatus(status, languageId, pageable);
        return new PageResult<>(
                result.getContent().stream().map(AssessmentPolicyMapper::toDomain).toList(),
                page,
                size,
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Override
    public PageResult<AssessmentPolicy> findAllBySchoolId(UUID schoolId, String status, UUID languageId, int page, int size) {
        var pageable = PageRequest.of(page - 1, size);
        var result = springDataAssessmentPolicyRepository.findBySchoolIdAndStatus(schoolId, status, languageId, pageable);
        return new PageResult<>(
                result.getContent().stream().map(AssessmentPolicyMapper::toDomain).toList(),
                page,
                size,
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Override
    public Optional<AssessmentPolicy> findActivePolicy(UUID schoolId, UUID languageId, UUID classId, UUID gradeId,
            UUID gradeLevelId, OffsetDateTime atTime) {
        return springDataAssessmentPolicyRepository.findCandidatePolicies(schoolId, languageId, classId, gradeId, gradeLevelId, atTime, PageRequest.ofSize(1))
                .stream()
                .findFirst()
                .map(AssessmentPolicyMapper::toDomain);
    }

    @Override
    public boolean existsByFrameworkVersionId(UUID frameworkVersionId) {
        return springDataAssessmentPolicyRepository.existsByFrameworkVersionId(frameworkVersionId);
    }

    @Override
    public boolean existsActiveForScope(UUID schoolId, UUID languageId, UUID frameworkVersionId,
            UUID schoolGradeLevelId, UUID schoolGradeId, UUID schoolClassId, UUID rubricVersionId) {
        return springDataAssessmentPolicyRepository.existsActiveForScope(schoolId, languageId, frameworkVersionId,
                schoolGradeLevelId, schoolGradeId, schoolClassId, rubricVersionId);
    }

    @Override
    public int findMaxVersionForScope(UUID schoolId, UUID languageId, UUID frameworkVersionId,
            UUID schoolGradeLevelId, UUID schoolGradeId, UUID schoolClassId) {
        return springDataAssessmentPolicyRepository.findMaxVersionForScope(schoolId, languageId, frameworkVersionId,
                schoolGradeLevelId, schoolGradeId, schoolClassId);
    }

    @Override
    public boolean existsPublishedByRubricVersionId(UUID rubricVersionId) {
        return springDataAssessmentPolicyRepository.existsByRubricVersionIdAndStatus(rubricVersionId, "PUBLISHED");
    }

    @Override
    public boolean existsNotPublishedByRubricVersionId(UUID rubricVersionId) {
        return springDataAssessmentPolicyRepository.existsByRubricVersionIdAndStatusNot(rubricVersionId, "PUBLISHED");
    }

    @Override
    public void deleteById(UUID id) {
        springDataAssessmentPolicyRepository.deleteById(id);
    }
}
