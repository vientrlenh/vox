package com.sep.vox.application.port.input.usecase.subscription;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.query.ViewInvoicesQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.InvoiceQueryRepository;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.InvoiceDto;

@Service
public class ViewInvoicesUseCase implements IUseCase<ViewInvoicesQuery, PageResult<InvoiceDto>> {

    private final InvoiceQueryRepository invoiceQueryRepository;
    private final UserContextPort userContextPort;

    public ViewInvoicesUseCase(InvoiceQueryRepository invoiceQueryRepository, UserContextPort userContextPort) {
        this.invoiceQueryRepository = invoiceQueryRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<InvoiceDto> execute(ViewInvoicesQuery input) {
        if (!userContextPort.isSystemAdmin() && !input.schoolId().equals(userContextPort.getCurrentSchoolId())) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        var page = invoiceQueryRepository.findAllBySchoolId(input.schoolId(), PageRequest.of(input.page(), input.size()));

        return new PageResult<>(page.getContent(), input.page(), input.size(), page.getTotalElements(), page.getTotalPages());
    }
}
