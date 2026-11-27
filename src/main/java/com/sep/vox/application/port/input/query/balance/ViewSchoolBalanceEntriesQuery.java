package com.sep.vox.application.port.input.query.balance;

import java.time.Instant;
import java.util.UUID;

/**
 * @param entryType null = do not filter by entry type
 * @param from inclusive, {@code to} EXCLUSIVE. The use case fills in defaults itself when the controller receives null,
 *     so by the time it reaches the repository both are always non-null -- see SchoolBalanceEntryRepository.findBySchoolId.
 * @param page counted FROM 1
 */
public record ViewSchoolBalanceEntriesQuery(
    UUID schoolId,
    String entryType,
    Instant from,
    Instant to,
    int page,
    int size
) {
}
