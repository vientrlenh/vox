package com.sep.vox.domain.repository;

import java.time.Instant;
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
    /** Same as above but also filters by role code and full name/email. `pageNumber` is 1-based. */
    PageResult<SchoolClassUser> findBySchoolClassId(UUID schoolClassId, String roleCode, String search,
            int pageNumber, int size);
    /** Number of active members per class. A class with no members does not appear in the map. */
    Map<UUID, Integer> countActiveBySchoolClassIdIn(Collection<UUID> schoolClassIds);
    boolean existsBySchoolClassId(UUID schoolClassId);
    SchoolClassUser save(SchoolClassUser schoolClassUser);
    List<SchoolClassUser> saveAll(Collection<SchoolClassUser> schoolClassUsers);

    /** Deactivates every active member of the classes of a school year. Returns the number of affected rows. */
    int deactivateByGradeId(UUID schoolGradeId, Instant leftAt);

    /** Deactivates every active member of a class. Returns the number of affected rows. */
    int deactivateBySchoolClassId(UUID schoolClassId, Instant leftAt);

    /** The grade (school_grade_id) of the class the student most recently belongs to as an active member. */
    Optional<UUID> findCurrentSchoolGradeId(UUID studentId);
}
