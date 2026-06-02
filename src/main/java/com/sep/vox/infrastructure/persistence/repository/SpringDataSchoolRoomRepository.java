package com.sep.vox.infrastructure.persistence.repository;


import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.SchoolRoomJpaEntity;

public interface SpringDataSchoolRoomRepository extends JpaRepository<SchoolRoomJpaEntity, UUID> {
    // Thêm dòng này để Spring Data tự động query kiểm tra mã code
    boolean existsBySchoolIdAndCode(UUID schoolId, String code);

    Page<SchoolRoomJpaEntity> findBySchoolId(UUID schoolId, Pageable pageable);
}
