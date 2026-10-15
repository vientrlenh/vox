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
    public AssessmentMember save(AssessmentMember examMember) {
        var saved = springDataAssessmentMemberRepository.save(AssessmentMemberMapper.toJpa(examMember));
        return AssessmentMemberMapper.toDomain(saved);
    }

    @Override
    public Optional<AssessmentMember> findById(UUID id) {
        return springDataAssessmentMemberRepository.findById(id)
            .map(AssessmentMemberMapper::toDomain);
    }

    @Override
    public List<AssessmentMember> findByExamId(UUID examId) {
        return springDataAssessmentMemberRepository.findByExamId(examId).stream()
            .map(AssessmentMemberMapper::toDomain)
            .toList();
    }

    @Override
    public List<AssessmentMember> findByExamIdIn(Collection<UUID> examIds) {
        if (examIds.isEmpty()) {
            return List.of();
        }
        return springDataAssessmentMemberRepository.findByExamIdIn(examIds).stream()
            .map(AssessmentMemberMapper::toDomain)
            .toList();
    }

    @Override
    public Optional<AssessmentMember> findByExamIdAndUserId(UUID examId, UUID userId) {
        return springDataAssessmentMemberRepository.findByExamIdAndUserId(examId, userId)
            .map(AssessmentMemberMapper::toDomain);
    }

    @Override
    public boolean existsByExamIdAndUserIdAndRole(UUID examId, UUID userId, AssessmentMemberRole role) {
        return springDataAssessmentMemberRepository.existsByExamIdAndUserIdAndRole(examId, userId, role.name());
    }

    @Override
    public boolean existsByUserIdAndRoleAndSchoolId(UUID userId, AssessmentMemberRole role, UUID schoolId) {
        return springDataAssessmentMemberRepository.existsByUserIdAndRoleAndSchoolId(userId, role.name(), schoolId);
    }

    @Override
    public boolean existsByExamIdAndRoleExcludingUserId(UUID examId, AssessmentMemberRole role, UUID excludeUserId) {
        return springDataAssessmentMemberRepository.existsByExamIdAndRoleExcludingUserId(examId, role.name(), excludeUserId);
    }

    @Override
    public boolean existsByRoleAndSchoolIdExcludingUserId(AssessmentMemberRole role, UUID schoolId, UUID excludeUserId) {
        return springDataAssessmentMemberRepository.existsByRoleAndSchoolIdExcludingUserId(role.name(), schoolId, excludeUserId);
    }

    @Override
    public boolean canAttachBlueprint(UUID examId, UUID userId) {
        return springDataAssessmentMemberRepository.canAttachBlueprint(examId, userId);
    }

    @Override
    public boolean canApproveBlueprintVersion(UUID examId, UUID userId) {
        return springDataAssessmentMemberRepository.canApproveBlueprintVersion(examId, userId);
    }

    @Override
    public void deleteById(UUID id) {
        springDataAssessmentMemberRepository.deleteById(id);
    }

    @Override
    public void deleteByExamId(UUID examId) {
        springDataAssessmentMemberRepository.deleteByExamId(examId);
    }
}
