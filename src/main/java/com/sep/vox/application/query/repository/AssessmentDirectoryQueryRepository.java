package com.sep.vox.application.query.repository;

import java.util.Collection;
import java.util.UUID;

import com.sep.vox.application.query.dto.AssessmentDirectoryGradeInfo;
import com.sep.vox.application.query.dto.AssessmentDirectoryUserInfo;
import com.sep.vox.domain.common.PageResult;

/**
 * The read model of the candidate / proctor source directory for an assessment.
 *
 * <p>Kept apart from the write repositories because this is a flat projection (with users already
 * joined) -- the GraphQL surface of the directory must not nest the `User`/`SchoolUser` types (those types
 * have field resolvers gated by SCHOOL_ADMIN).
 *
 * <p>`page` is **1-based**, matching every other paged repository in this repo.
 */
public interface AssessmentDirectoryQueryRepository {

    /** The school years of a school, filtered by code/name. */
    PageResult<AssessmentDirectoryGradeInfo> findGradesBySchoolId(UUID schoolId, String search, int page, int size);

    /**
     * The users of a school by role code (STUDENT / TEACHER).
     *
     * <p>{@code excludeUserIds} removes people inside the SQL so that {@code content} and
     * {@code totalElements}/{@code totalPages} agree -- filtering after paging would give a short
     * (even empty) page with a count that still includes the excluded people.
     */
    PageResult<AssessmentDirectoryUserInfo> findUsersBySchoolId(UUID schoolId, String roleCode, String search,
        Collection<UUID> excludeUserIds, int page, int size);

    /**
     * The active users in a set of classes, by role code. An empty set of classes returns an empty
     * page -- it must not be read as "the whole school".
     */
    PageResult<AssessmentDirectoryUserInfo> findUsersByClassIds(Collection<UUID> schoolClassIds, String roleCode,
        String search, Collection<UUID> excludeUserIds, int page, int size);
}
