package com.sep.vox.infrastructure.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.SchoolDirectoryJpaEntity;

public interface SpringDataSchoolDirectoryRepository extends JpaRepository<SchoolDirectoryJpaEntity, UUID> {
    boolean existsByCode(String code);
    List<SchoolDirectoryJpaEntity> findByCodeIn(Collection<String> codes);
    Slice<SchoolDirectoryJpaEntity> findAllByOrderByIdAsc(Pageable pageable);
    Slice<SchoolDirectoryJpaEntity> findByIdGreaterThanOrderByIdAsc(UUID cursor, Pageable pageable);
    Page<SchoolDirectoryJpaEntity> findAllByOrderByIdDesc(Pageable pageable);
}
