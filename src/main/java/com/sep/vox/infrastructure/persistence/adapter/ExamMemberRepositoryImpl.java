package com.sep.vox.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.exam.ExamMember;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.infrastructure.persistence.mapper.ExamMemberMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataExamMemberRepository;

@Repository
public class ExamMemberRepositoryImpl implements ExamMemberRepository {

    private final SpringDataExamMemberRepository springDataExamMemberRepository;

    public ExamMemberRepositoryImpl(SpringDataExamMemberRepository springDataExamMemberRepository) {
        this.springDataExamMemberRepository = springDataExamMemberRepository;
    }

    @Override
    public ExamMember save(ExamMember examMember) {
        var saved = springDataExamMemberRepository.save(ExamMemberMapper.toJpa(examMember));
        return ExamMemberMapper.toDomain(saved);
    }

    @Override
    public Optional<ExamMember> findById(UUID id) {
        return springDataExamMemberRepository.findById(id)
            .map(ExamMemberMapper::toDomain);
    }

    @Override
    public List<ExamMember> findByExamId(UUID examId) {
        return springDataExamMemberRepository.findByExamId(examId).stream()
            .map(ExamMemberMapper::toDomain)
            .toList();
    }

    @Override
    public Optional<ExamMember> findByExamIdAndUserId(UUID examId, UUID userId) {
        return springDataExamMemberRepository.findByExamIdAndUserId(examId, userId)
            .map(ExamMemberMapper::toDomain);
    }

    @Override
    public boolean existsByExamIdAndUserIdAndRole(UUID examId, UUID userId, ExamMemberRole role) {
        return springDataExamMemberRepository.existsByExamIdAndUserIdAndRole(examId, userId, role.name());
    }

    @Override
    public boolean existsByUserIdAndRoleAndSchoolId(UUID userId, ExamMemberRole role, UUID schoolId) {
        return springDataExamMemberRepository.existsByUserIdAndRoleAndSchoolId(userId, role.name(), schoolId);
    }

    @Override
    public boolean existsByExamIdAndRoleExcludingUserId(UUID examId, ExamMemberRole role, UUID excludeUserId) {
        return springDataExamMemberRepository.existsByExamIdAndRoleExcludingUserId(examId, role.name(), excludeUserId);
    }

    @Override
    public boolean existsByRoleAndSchoolIdExcludingUserId(ExamMemberRole role, UUID schoolId, UUID excludeUserId) {
        return springDataExamMemberRepository.existsByRoleAndSchoolIdExcludingUserId(role.name(), schoolId, excludeUserId);
    }

    @Override
    public void deleteById(UUID id) {
        springDataExamMemberRepository.deleteById(id);
    }
}
