package com.sep.vox.application.port.input.usecase.balance;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.ViewSchoolDebtEventsQuery;
import com.sep.vox.application.port.input.service.SchoolScopedReadGuard;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.SchoolDebtEventDto;
import com.sep.vox.domain.repository.SchoolDebtEventRepository;

/**
 * Sổ audit "nguyên nhân nợ hạn mức AI" của một trường.
 *
 * <p>Ở package balance chứ không phải school: nợ là mặt trái của chính cái ví -- số dư âm CHÍNH LÀ
 * khoản nợ -- nên nó thuộc về cùng một nhóm với sao kê ví, không phải với lớp học và phòng thi.
 */
@Service
public class ViewSchoolDebtEventsUseCase
        implements IUseCase<ViewSchoolDebtEventsQuery, PageResult<SchoolDebtEventDto>> {

    private final SchoolDebtEventRepository schoolDebtEventRepository;
    private final SchoolScopedReadGuard schoolScopedReadGuard;

    public ViewSchoolDebtEventsUseCase(
            SchoolDebtEventRepository schoolDebtEventRepository,
            SchoolScopedReadGuard schoolScopedReadGuard) {
        this.schoolDebtEventRepository = schoolDebtEventRepository;
        this.schoolScopedReadGuard = schoolScopedReadGuard;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<SchoolDebtEventDto> execute(ViewSchoolDebtEventsQuery input) {
        schoolScopedReadGuard.requireCanRead(input.schoolId());

        // page đi vào repository theo lối 1-BASED; adapter là chỗ DUY NHẤT trừ 1 trước khi giao cho
        // Spring Data -- xem PagingConventionTests.
        var page = schoolDebtEventRepository.findBySchoolId(input.schoolId(), input.page(), input.size());

        return new PageResult<>(
            page.content().stream().map(SchoolDebtEventDto::toDto).toList(),
            page.page(),
            page.size(),
            page.totalElements(),
            page.totalPages()
        );
    }
}
