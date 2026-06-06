package com.sep.vox.infrastructure.persistence.repository;


import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.SchoolRoomJpaEntity;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataSchoolRoomRepository extends JpaRepository<SchoolRoomJpaEntity, UUID> {
    // Thêm dòng này để Spring Data tự động query kiểm tra mã code
    boolean existsBySchoolIdAndCode(UUID schoolId, String code);

    Page<SchoolRoomJpaEntity> findBySchoolId(UUID schoolId, Pageable pageable);


    // CHỈ CẦN DÙNG LOCK ĐỂ CHẶN NGƯỜI KHÁC SỬA CÙNG LÚC
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM SchoolRoomJpaEntity r WHERE r.id = :id")
    Optional<SchoolRoomJpaEntity> findByIdForUpdate(@Param("id") UUID id);
}
