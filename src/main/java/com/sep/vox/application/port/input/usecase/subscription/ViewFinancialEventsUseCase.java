package com.sep.vox.application.port.input.usecase.subscription;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.query.ViewFinancialEventsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.FinancialEventQueryRepository;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.FinancialEventDto;

@Service
public class ViewFinancialEventsUseCase implements IUseCase<ViewFinancialEventsQuery, PageResult<FinancialEventDto>> {

    private final FinancialEventQueryRepository financialEventQueryRepository;
    private final UserContextPort userContextPort;

    public ViewFinancialEventsUseCase(FinancialEventQueryRepository financialEventQueryRepository, UserContextPort userContextPort) {
        this.financialEventQueryRepository = financialEventQueryRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<FinancialEventDto> execute(ViewFinancialEventsQuery input) {
        if (!userContextPort.isSystemAdmin() && !input.schoolId().equals(userContextPort.getCurrentSchoolId())) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        var page = financialEventQueryRepository.findAllBySchoolId(input.schoolId(), PageRequest.of(input.page(), input.size()));

        return new PageResult<>(page.getContent(), input.page(), input.size(), page.getTotalElements(), page.getTotalPages());
    }
}
