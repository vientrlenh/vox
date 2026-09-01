package com.sep.vox.infrastructure.persistence.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.SchoolAiSpendEntryJpaEntity;

public interface SpringDataSchoolAiSpendEntryRepository
    extends JpaRepository<SchoolAiSpendEntryJpaEntity, UUID> {
}
