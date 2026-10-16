package com.sep.vox.application.port.input.usecase.order;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.ViewMySchoolOrdersQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.OrderDto;
import com.sep.vox.domain.repository.OrderRepository;
import com.sep.vox.domain.shared.PageResult;


@Service
public class ViewMySchoolOrdersUseCase implements IUseCase<ViewMySchoolOrdersQuery, PageResult<OrderDto>> {

    private final OrderRepository orderRepository;
    private final UserContextPort userContextPort;

    public ViewMySchoolOrdersUseCase(OrderRepository orderRepository, UserContextPort userContextPort) {
        this.orderRepository = orderRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<OrderDto> execute(ViewMySchoolOrdersQuery input) {
        var schoolId = userContextPort.getCurrentSchoolId();
        var page = orderRepository.findBySchoolId(schoolId, input.page(), input.size());

        return new PageResult<>(
            page.content().stream().map(OrderDto::toDto).toList(),
            page.page(),
            page.size(),
            page.totalElements(),
            page.totalPages()
        );
    }
}
