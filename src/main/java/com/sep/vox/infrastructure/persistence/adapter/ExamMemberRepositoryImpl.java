package com.sep.vox.infrastructure.persistence.adapter;

import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.infrastructure.persistence.repository.SpringDataExamMemberRepository;

@Repository
public class ExamMemberRepositoryImpl implements ExamMemberRepository {

    private final SpringDataExamMemberRepository springDataExamMemberRepository;

    public ExamMemberRepositoryImpl(SpringDataExamMemberRepository springDataExamMemberRepository) {
        this.springDataExamMemberRepository = springDataExamMemberRepository;
    }

    @Override
    public boolean existsByExamIdAndUserIdAndRole(UUID examId, UUID userId, ExamMemberRole role) {
        return springDataExamMemberRepository.existsByExamIdAndUserIdAndRole(examId, userId, role.name());
    }
    
}
