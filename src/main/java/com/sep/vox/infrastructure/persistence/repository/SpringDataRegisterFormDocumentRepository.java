package com.sep.vox.infrastructure.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.RegisterFormDocumentJpaEntity;

public interface SpringDataRegisterFormDocumentRepository extends JpaRepository<RegisterFormDocumentJpaEntity, UUID> {
    List<RegisterFormDocumentJpaEntity> findByRegisterFormId(UUID registerFormId);
    List<RegisterFormDocumentJpaEntity> findByRegisterFormIdIn(Collection<UUID> registerFormIds);
}
