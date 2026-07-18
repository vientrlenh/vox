package com.sep.vox.application.port.input.usecase.subscription;

import java.util.Comparator;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.query.ViewInvoicesQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.InvoiceDto;
import com.sep.vox.domain.mapper.InvoiceDtoMapper;
import com.sep.vox.domain.model.subscription.Invoice;
import com.sep.vox.domain.repository.InvoiceRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;

@Service
public class ViewInvoicesUseCase implements IUseCase<ViewInvoicesQuery, PageResult<InvoiceDto>> {

    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final InvoiceRepository invoiceRepository;
    private final UserContextPort userContextPort;

    public ViewInvoicesUseCase(
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            InvoiceRepository invoiceRepository,
            UserContextPort userContextPort) {
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.invoiceRepository = invoiceRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<InvoiceDto> execute(ViewInvoicesQuery input) {
        if (!userContextPort.isSystemAdmin() && !input.schoolId().equals(userContextPort.getCurrentSchoolId())) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        var all = schoolSubscriptionRepository.findAllBySchoolId(input.schoolId()).stream()
            .flatMap(subscription -> invoiceRepository.findAllBySubscriptionId(subscription.getId()).stream())
            .sorted(Comparator.comparing(Invoice::getIssueDate).reversed())
            .toList();

        var fromIndex = Math.min(input.page() * input.size(), all.size());
        var toIndex = Math.min(fromIndex + input.size(), all.size());
        var pageContent = InvoiceDtoMapper.toDtoList(all.subList(fromIndex, toIndex));

        return new PageResult<>(pageContent, input.page(), input.size(), all.size(), (int) Math.ceil((double) all.size() / input.size()));
    }
}
