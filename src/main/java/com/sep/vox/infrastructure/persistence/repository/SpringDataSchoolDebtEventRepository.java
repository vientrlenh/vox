package com.sep.vox.infrastructure.persistence.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.SchoolDebtEventJpaEntity;

public interface SpringDataSchoolDebtEventRepository extends JpaRepository<SchoolDebtEventJpaEntity, UUID> {

    // Khoá sắp xếp phụ theo id KHÔNG phải cho đẹp: một ca thi có thể sinh nhiều sự kiện trong cùng
    // một Instant, và chỉ ORDER BY occurred_at thì thứ tự giữa chúng là không xác định -- Postgres
    // được phép trả khác nhau giữa hai lần chạy. Với phân trang, hậu quả là một dòng hiện hai lần ở
    // hai trang còn một dòng khác biến mất. Id là uuidv7 nên tăng theo thời gian: vừa ổn định, vừa
    // không phá thứ tự thời gian.
    Page<SchoolDebtEventJpaEntity> findBySchoolIdOrderByOccurredAtDescIdDesc(UUID schoolId, Pageable pageable);
}
