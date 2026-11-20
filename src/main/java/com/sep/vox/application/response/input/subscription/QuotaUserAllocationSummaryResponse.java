package com.sep.vox.application.response.input.subscription;

import java.util.List;

import com.sep.vox.domain.dto.SchoolSubscriptionQuotaRecordDto;
import com.sep.vox.domain.dto.SchoolSubscriptionQuotaUserAllocationDto;

/**
 * The school administrator's screen for distributing personal quota: what the school wallet holds, and how much the school has
 * distributed to each person.
 *
 * <p>ALWAYS returns BOTH because the allocation list alone cannot be read: "teacher A has 200,000 left"
 * only means something when you know how much the school's ASSESSMENT wallet has left -- the total distributed to individuals is NOT
 * forced to equal the wallet capacity (see DistributeQuotaToUsersService), so a mismatch between the two numbers is
 * normal and the mismatch itself is what the user needs to look at.
 *
 * @param pool        the school-level wallet -- ASSESSMENT when distributing to teachers, PRACTICE when distributing to students
 * @param allocations only people who HAVE been given a separate quota; not being in here means
 *                    not being blocked per person, only the school's wallet applies
 */
public record QuotaUserAllocationSummaryResponse(
    SchoolSubscriptionQuotaRecordDto pool,
    List<SchoolSubscriptionQuotaUserAllocationDto> allocations
) { 
}
