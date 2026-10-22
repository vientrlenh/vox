package com.sep.vox.application.port.input.query;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.application.shared.AppPageRequest;

/**
 * @param entryType null = không lọc theo loại bút toán
 * @param from bao gồm, {@code to} KHÔNG bao gồm. Use case tự điền mặc định khi controller nhận null,
 *     nên tới repository thì cả hai luôn khác null -- xem SchoolBalanceEntryRepository.findBySchoolId.
 * @param pageRequest trang đếm TỪ 1
 */
public record ViewSchoolBalanceEntriesQuery(
    UUID schoolId,
    String entryType,
    Instant from,
    Instant to,
    AppPageRequest pageRequest
) {
}
