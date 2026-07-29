package com.sep.vox.domain.repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.school.SchoolClassUser;

public interface SchoolClassUserRepository {
    Optional<SchoolClassUser> findByUserIdAndSchoolClassId(UUID userId, UUID schoolClassId);
    List<SchoolClassUser> findByUserIdInAndSchoolClassIdIn(Collection<UUID> userIds, Collection<UUID> schoolClassIds);
    List<SchoolClassUser> findByUserId(UUID userId);
    List<SchoolClassUser> findByUserIdIn(Collection<UUID> userIds);
    PageResult<SchoolClassUser> findBySchoolClassId(UUID schoolClassId, int pageNumber, int size);
    /** Như trên nhưng lọc thêm theo mã vai trò và họ tên/email. `pageNumber` là 1-based. */
    PageResult<SchoolClassUser> findBySchoolClassId(UUID schoolClassId, String roleCode, String search,
            int pageNumber, int size);
    /** Số thành viên đang active của từng lớp. Lớp không có thành viên sẽ không xuất hiện trong map. */
    Map<UUID, Integer> countActiveBySchoolClassIdIn(Collection<UUID> schoolClassIds);
    boolean existsBySchoolClassId(UUID schoolClassId);
    SchoolClassUser save(SchoolClassUser schoolClassUser);
    List<SchoolClassUser> saveAll(Collection<SchoolClassUser> schoolClassUsers);

    /** Vô hiệu hóa (deactivate) mọi thành viên đang active của các lớp thuộc một năm học. Trả về số dòng bị ảnh hưởng. */
    int deactivateByGradeId(UUID schoolGradeId, OffsetDateTime leftAt);

    /** Vô hiệu hóa (deactivate) mọi thành viên đang active của một lớp học. Trả về số dòng bị ảnh hưởng. */
    int deactivateBySchoolClassId(UUID schoolClassId, OffsetDateTime leftAt);
}
