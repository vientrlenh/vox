package com.sep.vox.infrastructure.persistence.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.SchoolBalanceJpaEntity;

import jakarta.persistence.LockModeType;

public interface SpringDataSchoolBalanceRepository extends JpaRepository<SchoolBalanceJpaEntity, UUID> {

    Optional<SchoolBalanceJpaEntity> findBySchoolId(UUID schoolId);

    // PESSIMISTIC_WRITE: chặn mọi transaction khác đụng vào số dư của trường này cho tới khi
    // transaction hiện tại commit. Cần thiết vì bút toán đi kèm phải ghi balance_after_vnd -- đọc
    // không khóa thì hai lần nạp/trừ song song sẽ ghi ra hai entry có cùng balanceAfter.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<SchoolBalanceJpaEntity> findWithLockBySchoolId(UUID schoolId);

    /**
     * Bảo đảm trường đã CÓ dòng số dư, để {@link #findWithLockBySchoolId} có cái mà khóa.
     *
     * <p>SELECT ... FOR UPDATE không khóa được một dòng chưa tồn tại, nên "đọc, không thấy thì dựng
     * mới" là một cửa sổ tranh chấp thật: hai lần chấm bài song song của một trường CHƯA TỪNG NẠP đều
     * không thấy gì và cùng INSERT, rồi một trong hai chết vì uq_school_balances_school. Đó lại đúng
     * là trường hợp phổ biến nhất -- phần lớn trường không nạp bao giờ, và lần đầu tiên chạm vào ví
     * chính là lần đầu tiêu vượt hạn mức.
     *
     * <p>ON CONFLICT DO NOTHING biến cửa sổ đó thành một phép chờ: transaction đến sau chặn tại đây
     * cho tới khi transaction trước commit/rollback, rồi không làm gì và đi tiếp -- lần khóa ngay sau
     * đó đọc được đúng dòng đã commit.
     *
     * <p>id để Postgres tự sinh (DEFAULT uuidv7()) và version = 0 để @Version của entity bắt nhịp
     * đúng ở lần save kế tiếp.
     */
    @Modifying
    @Query(value = """
        INSERT INTO school_balances (school_id, balance_vnd, created_at, updated_at, version)
        VALUES (:schoolId, 0, :now, :now, 0)
        ON CONFLICT (school_id) DO NOTHING
        """, nativeQuery = true)
    int insertIfAbsent(@Param("schoolId") UUID schoolId, @Param("now") Instant now);
}
