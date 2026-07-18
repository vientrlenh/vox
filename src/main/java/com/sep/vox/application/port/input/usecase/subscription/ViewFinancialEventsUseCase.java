package com.sep.vox.application.port.input.usecase.subscription;

import java.util.Comparator;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.query.ViewFinancialEventsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.FinancialEventDto;
import com.sep.vox.domain.mapper.FinancialEventDtoMapper;
import com.sep.vox.domain.model.subscription.FinancialEvent;
import com.sep.vox.domain.repository.FinancialEventRepository;

@Service
public class ViewFinancialEventsUseCase implements IUseCase<ViewFinancialEventsQuery, PageResult<FinancialEventDto>> {

    private final FinancialEventRepository financialEventRepository;
    private final UserContextPort userContextPort;

    public ViewFinancialEventsUseCase(FinancialEventRepository financialEventRepository, UserContextPort userContextPort) {
        this.financialEventRepository = financialEventRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<FinancialEventDto> execute(ViewFinancialEventsQuery input) {
        if (!userContextPort.isSystemAdmin() && !input.schoolId().equals(userContextPort.getCurrentSchoolId())) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        // TODO: add a DB LIMIT/OFFSET query if this ledger grows large enough to matter.
        var all = financialEventRepository.findAllBySchoolId(input.schoolId()).stream()
            .sorted(Comparator.comparing(FinancialEvent::getOccurredAt).reversed())
            .toList();

        var fromIndex = Math.min(input.page() * input.size(), all.size());
        var toIndex = Math.min(fromIndex + input.size(), all.size());
        var pageContent = FinancialEventDtoMapper.toDtoList(all.subList(fromIndex, toIndex));

        return new PageResult<>(pageContent, input.page(), input.size(), all.size(), (int) Math.ceil((double) all.size() / input.size()));
    }
}
