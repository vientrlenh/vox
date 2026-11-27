package com.sep.vox.application.port.input.usecase.balance;

import com.sep.vox.domain.model.school.SchoolDebtEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.balance.ViewSchoolDebtEventsQuery;
import com.sep.vox.application.port.input.service.SchoolScopedReadGuard;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.SchoolDebtEventDto;
import com.sep.vox.domain.repository.SchoolDebtEventRepository;

/**
 * The audit ledger of "what caused the AI quota debt" of a school.
 *
 * <p>Lives in the balance package rather than school: debt is the flip side of the balance itself -- a negative
 * balance IS the debt -- so it belongs with the balance statement, not with classes and exam rooms.
 */
@Service
@RequiredArgsConstructor
public class ViewSchoolDebtEventsUseCase
        implements IUseCase<ViewSchoolDebtEventsQuery, PageResult<SchoolDebtEventDto>> {

    private final SchoolDebtEventRepository schoolDebtEventRepository;
    private final SchoolScopedReadGuard schoolScopedReadGuard;

    @Override
    @Transactional(readOnly = true)
    public PageResult<SchoolDebtEventDto> execute(ViewSchoolDebtEventsQuery input) {
        schoolScopedReadGuard.requireCanRead(input.schoolId());

        // page goes into the repository 1-BASED; the adapter is the ONLY place that subtracts 1 before handing it to
        // Spring Data -- see PagingConventionTests.
        PageResult<SchoolDebtEvent> page = schoolDebtEventRepository.findBySchoolId(input.schoolId(), input.page(), input.size());

        return new PageResult<>(
            page.content().stream().map(SchoolDebtEventDto::toDto).toList(),
            page.page(),
            page.size(),
            page.totalElements(),
            page.totalPages()
        );
    }
}
