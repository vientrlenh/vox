package com.sep.vox.application.port.input.query.subscription;

import java.util.UUID;

/**
 * @param search filter by the user's NAME, null/empty = no filter. Filtered on the server and not in the UI:
 *               since the list became paged, filtering within a page of 20 rows would miss exactly the person
 *               the administrator is looking for.
 * @param page   counted FROM 1 per the project-wide convention
 */
public record ViewQuotaUserAllocationsQuery(
    UUID schoolId,
    String search,
    int page,
    int size
) {
}
