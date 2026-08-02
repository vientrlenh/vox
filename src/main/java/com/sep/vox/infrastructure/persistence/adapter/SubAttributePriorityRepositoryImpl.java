package com.sep.vox.infrastructure.persistence.adapter;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.model.personalization.SubAttributePriority;
import com.sep.vox.domain.repository.personalization.SubAttributePriorityRepository;
import com.sep.vox.infrastructure.persistence.entity.SubAttributePriorityJpaEntity;
import com.sep.vox.infrastructure.persistence.repository.SpringDataSubAttributePriorityRepository;

@Repository
public class SubAttributePriorityRepositoryImpl implements SubAttributePriorityRepository {

    private final SpringDataSubAttributePriorityRepository repository;

    public SubAttributePriorityRepositoryImpl(
            SpringDataSubAttributePriorityRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void replaceForStudents(
            List<UUID> studentIds,
            List<SubAttributePriority> priorities) {
        if (studentIds.isEmpty()) {
            return;
        }
        repository.deleteByStudentIdIn(studentIds);
        repository.saveAll(priorities.stream()
            .map(item -> new SubAttributePriorityJpaEntity(
                item.id(),
                item.studentId(),
                item.frameworkCriterionId(),
                item.subAttribute(),
                item.frequency(),
                item.recentFrequency(),
                item.priority(),
                item.practiceable(),
                item.computedAt()
            ))
            .toList());
    }

    @Override
    public List<PracticeablePriority> findPracticeablePrioritiesOrderedDesc(UUID studentId) {
        return repository.findPracticeablePrioritiesOrderedDesc(studentId).stream()
            .map(row -> new PracticeablePriority(row.getCriterionCode(), row.getSubAttribute()))
            .toList();
    }
}
