package com.sep.vox.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.school.SchoolUserRole;
import com.sep.vox.domain.shared.PageResult;

public interface SchoolUserRepository {

    Optional<SchoolUser> findByUserId(UUID userId);

    List<SchoolUser> findByUserIdIn(Collection<UUID> userIds);

    SchoolUser save(SchoolUser schoolUser);

    List<SchoolUser> findBySchoolIdIn(Collection<UUID> schoolIds, int pageNumber, int size);

    PageResult<SchoolUser> findBySchoolId(UUID schoolId, String search, SchoolUserRole role, String status,
        UUID excludeClassId, boolean excludeAdminRoles, int pageNumber, int size);
    
    PageResult<SchoolUser> searchBySchoolId(UUID schoolId, UUID excludeUserId, String search, SchoolUserRole role,
        String status, int pageNumber, int size);
    
    Optional<SchoolUser> findBySchoolIdAndUserId(UUID schoolId, UUID userId);

    Optional<UUID> findSchoolIdByUserId(UUID userId);

    boolean existsBySchoolIdAndUserId(UUID schoolId, UUID userId);

    List<SchoolUser> findBySchoolIdWithRole(UUID schoolId, SchoolUserRole role);

    long countByRole(SchoolUserRole role);

    /** Vai trò trong trường của MỘT người; người không thuộc trường nào thì không có vai trò nào. */
    default boolean hasRole(UUID userId, SchoolUserRole role) {
        return findByUserId(userId)
            .filter(schoolUser -> schoolUser.getRole() == role)
            .isPresent();
    }

    /** Một query cho cả tập người dùng, thay vì hỏi vai trò từng người. */
    default Set<UUID> findUserIdsByRole(Collection<UUID> userIds, SchoolUserRole role) {
        return findByUserIdIn(userIds).stream()
            .filter(schoolUser -> schoolUser.getRole() == role)
            .map(SchoolUser::getUserId)
            .collect(Collectors.toSet());
    }
}
