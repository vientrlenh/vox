package com.sep.vox.infrastructure.persistence.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import com.sep.vox.infrastructure.persistence.entity.SchoolBalanceJpaEntity;

import jakarta.persistence.LockModeType;

public interface SpringDataSchoolBalanceRepository extends JpaRepository<SchoolBalanceJpaEntity, UUID> {

    Optional<SchoolBalanceJpaEntity> findBySchoolId(UUID schoolId);

    // PESSIMISTIC_WRITE: chặn mọi transaction khác đụng vào số dư của trường này cho tới khi
    // transaction hiện tại commit. Cần thiết vì bút toán đi kèm phải ghi balance_after_vnd -- đọc
    // không khóa thì hai lần nạp/trừ song song sẽ ghi ra hai entry có cùng balanceAfter.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<SchoolBalanceJpaEntity> findWithLockBySchoolId(UUID schoolId);
}
