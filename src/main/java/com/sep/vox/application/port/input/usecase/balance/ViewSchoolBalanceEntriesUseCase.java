package com.sep.vox.application.port.input.usecase.balance;

import com.sep.vox.domain.model.school.SchoolBalanceEntry;
import java.time.Instant;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.balance.ViewSchoolBalanceEntriesQuery;
import com.sep.vox.application.port.input.service.SchoolScopedReadGuard;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.SchoolBalanceEntryDto;
import com.sep.vox.domain.model.school.SchoolBalanceEntryType;
import com.sep.vox.domain.repository.SchoolBalanceEntryRepository;

/** Statement of a school's balance, newest first. */
@Service
@RequiredArgsConstructor
public class ViewSchoolBalanceEntriesUseCase
        implements IUseCase<ViewSchoolBalanceEntriesQuery, PageResult<SchoolBalanceEntryDto>> {

    private final SchoolBalanceEntryRepository schoolBalanceEntryRepository;
    private final SchoolScopedReadGuard schoolScopedReadGuard;

    @Override
    @Transactional(readOnly = true)
    public PageResult<SchoolBalanceEntryDto> execute(ViewSchoolBalanceEntriesQuery input) {
        schoolScopedReadGuard.requireCanRead(input.schoolId());

        PageResult<SchoolBalanceEntry> page = schoolBalanceEntryRepository.findBySchoolId(
            input.schoolId(),
            parseEntryType(input.entryType()),
            // The repository needs two non-null bounds (see its Javadoc). "No bound on either end" is turned into the
            // whole history range here, instead of pushing a null parameter down into the JPQL.
            input.from() == null ? Instant.EPOCH : input.from(),
            input.to() == null ? Instant.now() : input.to(),
            input.page(),
            input.size()
        );

        return new PageResult<>(
            page.content().stream().map(SchoolBalanceEntryDto::toDto).toList(),
            page.page(),
            page.size(),
            page.totalElements(),
            page.totalPages()
        );
    }

    /**
     * GraphQL already rejects unknown values at the schema layer (enum SchoolBalanceEntryType), so only null or a
     * valid name reaches this point. IllegalArgumentException is still caught so that other callers -- tests, or a
     * REST adapter added later -- do not blow up with a hard-to-read error from deep inside the enum.
     */
    private static SchoolBalanceEntryType parseEntryType(String entryType) {
        if (entryType == null || entryType.isBlank()) {
            return null;
        }
        try {
            return SchoolBalanceEntryType.valueOf(entryType);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid balance entry type: " + entryType);
        }
    }
}
