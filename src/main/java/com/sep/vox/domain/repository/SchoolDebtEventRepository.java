package com.sep.vox.domain.repository;

import java.util.UUID;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.school.SchoolDebtEvent;

/**
 * Audit ledger of "the cause of AI quota debt" -- append-only, written only when the debt state CHANGES. Mirror of
 * {@link SchoolBalanceEntryRepository}: the same wallet, one side tells where the money went, the other tells why
 * the school was locked.
 */
public interface SchoolDebtEventRepository {
    SchoolDebtEvent save(SchoolDebtEvent event);

    /**
     * Log of the school, newest first. {@code page} counts FROM 1 per the project-wide convention.
     *
     * <p>Replaces the earlier unpaginated {@code List} version: this ledger is sparse for most schools
     * but not for a school that keeps exceeding its quota, and its only read path is a paginated
     * UI page.
     */
    PageResult<SchoolDebtEvent> findBySchoolId(UUID schoolId, int page, int size);
}
