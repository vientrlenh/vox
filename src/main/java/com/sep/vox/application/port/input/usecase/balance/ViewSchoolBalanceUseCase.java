package com.sep.vox.application.port.input.usecase.balance;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.service.SchoolScopedReadGuard;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.dto.SchoolBalanceDto;
import com.sep.vox.domain.repository.SchoolBalanceRepository;

/**
 * The balance of a school.
 *
 * <p>Never returns null: a school that has never touched the balance gets a zero-VND instance built on the spot
 * -- see {@link SchoolBalanceDto#emptyFor}.
 */
@Service
@RequiredArgsConstructor
public class ViewSchoolBalanceUseCase implements IUseCase<UUID, SchoolBalanceDto> {

    private final SchoolBalanceRepository schoolBalanceRepository;
    private final SchoolScopedReadGuard schoolScopedReadGuard;

    @Override
    @Transactional(readOnly = true)
    public SchoolBalanceDto execute(UUID schoolId) {
        schoolScopedReadGuard.requireCanRead(schoolId);

        // findBySchoolId, NOT findBySchoolIdForUpdateOrCreate: the latter locks the row and CREATES it if missing, so
        // using it on a read path would spawn an empty balance row for every school just because somebody opened the
        // page -- and do so inside a readOnly transaction.
        return schoolBalanceRepository.findBySchoolId(schoolId)
            .map(SchoolBalanceDto::toDto)
            .orElseGet(() -> SchoolBalanceDto.emptyFor(schoolId));
    }
}
