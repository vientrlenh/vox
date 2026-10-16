package com.sep.vox.infrastructure.persistence.adapter.assessment;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.assessment.AssessmentMember;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.repository.assessment.AssessmentMemberRepository;
import com.sep.vox.infrastructure.persistence.mapper.assessment.AssessmentMemberMapper;
import com.sep.vox.infrastructure.persistence.repository.assessment.SpringDataAssessmentMemberRepository;

@Repository
public class AssessmentMemberRepositoryImpl implements AssessmentMemberRepository {

    private final SpringDataAssessmentMemberRepository springDataAssessmentMemberRepository;

    public AssessmentMemberRepositoryImpl(SpringDataAssessmentMemberRepository springDataAssessmentMemberRepository) {
        this.springDataAssessmentMemberRepository = springDataAssessmentMemberRepository;
    }

    @Override
    public AssessmentMember save(AssessmentMember assessmentMember) {
        var saved = springDataAssessmentMemberRepository.save(AssessmentMemberMapper.toJpa(assessmentMember));
        return AssessmentMemberMapper.toDomain(saved);
    }

    @Override
    public Optional<AssessmentMember> findById(UUID id) {
        return springDataAssessmentMemberRepository.findById(id)
            .map(AssessmentMemberMapper::toDomain);
    }

    @Override
    public List<AssessmentMember> findByAssessmentId(UUID assessmentId) {
        return springDataAssessmentMemberRepository.findByAssessmentId(assessmentId).stream()
            .map(AssessmentMemberMapper::toDomain)
            .toList();
    }

    @Override
    public List<AssessmentMember> findByAssessmentIdIn(Collection<UUID> assessmentIds) {
        if (assessmentIds.isEmpty()) {
            return List.of();
        }
        return springDataAssessmentMemberRepository.findByAssessmentIdIn(assessmentIds).stream()
            .map(AssessmentMemberMapper::toDomain)
            .toList();
    }

    @Override
    public Optional<AssessmentMember> findByAssessmentIdAndUserId(UUID assessmentId, UUID userId) {
        return springDataAssessmentMemberRepository.findByAssessmentIdAndUserId(assessmentId, userId)
            .map(AssessmentMemberMapper::toDomain);
    }

    @Override
    public boolean existsByAssessmentIdAndUserIdAndRole(UUID assessmentId, UUID userId, AssessmentMemberRole role) {
        return springDataAssessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessmentId, userId, role.name());
    }

    @Override
    public boolean existsByUserIdAndRoleAndSchoolId(UUID userId, AssessmentMemberRole role, UUID schoolId) {
        return springDataAssessmentMemberRepository.existsByUserIdAndRoleAndSchoolId(userId, role.name(), schoolId);
    }

    @Override
    public boolean existsByAssessmentIdAndRoleExcludingUserId(UUID assessmentId, AssessmentMemberRole role, UUID excludeUserId) {
        return springDataAssessmentMemberRepository.existsByAssessmentIdAndRoleExcludingUserId(assessmentId, role.name(), excludeUserId);
    }

    @Override
    public boolean existsByRoleAndSchoolIdExcludingUserId(AssessmentMemberRole role, UUID schoolId, UUID excludeUserId) {
        return springDataAssessmentMemberRepository.existsByRoleAndSchoolIdExcludingUserId(role.name(), schoolId, excludeUserId);
    }

    @Override
    public boolean canAttachBlueprint(UUID assessmentId, UUID userId) {
        return springDataAssessmentMemberRepository.canAttachBlueprint(assessmentId, userId);
    }

    @Override
    public boolean canApproveBlueprintVersion(UUID assessmentId, UUID userId) {
        return springDataAssessmentMemberRepository.canApproveBlueprintVersion(assessmentId, userId);
    }

    @Override
    public void deleteById(UUID id) {
        springDataAssessmentMemberRepository.deleteById(id);
    }

    @Override
    public void deleteByAssessmentId(UUID assessmentId) {
        springDataAssessmentMemberRepository.deleteByAssessmentId(assessmentId);
    }
}
