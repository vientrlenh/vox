package com.sep.vox.domain.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.school.SchoolQuotaPolicy;

/**
 * Quota distribution ceiling per SCHOOL and per quota type.
 *
 * <p>No row = the whole balance may be distributed. So the read path never hands an empty Optional outside:
 * it builds the default in place, just as the wallet balance builds a 0-dong version for a school that never topped up.
 */
public interface SchoolQuotaPolicyRepository {

    /** Never null -- a school that set nothing gets the default 1.0, NOT written to the DB. */
    SchoolQuotaPolicy findBySchoolIdAndQuotaType(UUID schoolId, QuotaType quotaType);

    List<SchoolQuotaPolicy> findBySchoolId(UUID schoolId);

    /** Creates the row if missing, overwrites the ratio if it exists. */
    SchoolQuotaPolicy upsertRatio(UUID schoolId, QuotaType quotaType, BigDecimal distributableRatio);
}
