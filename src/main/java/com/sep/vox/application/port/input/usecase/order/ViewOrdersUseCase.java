package com.sep.vox.application.port.input.usecase.order;

import com.sep.vox.domain.model.order.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.ViewOrdersQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.OrderDto;
import com.sep.vox.domain.repository.OrderRepository;

/**
 * The system-wide order list for the System Admin.
 *
 * <p>A PENDING order is DIAGNOSTIC information on the admin side, not garbage to filter out: a pile of
 * pending orders at the same time is a sign that the gateway is not calling the callback back, and one
 * unusually long-pending order means a school is locked out of placing a new order (see
 * CreateSubscriptionOrderUseCase).
 */
@Service
public class ViewOrdersUseCase implements IUseCase<ViewOrdersQuery, PageResult<OrderDto>> {

    private final OrderRepository orderRepository;

    public ViewOrdersUseCase(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<OrderDto> execute(ViewOrdersQuery input) {
        PageResult<Order> page = orderRepository.findForAdmin(
            input.schoolId(),
            input.status(),
            input.type(),
            input.keyword(),
            input.page(),
            input.size()
        );

        return new PageResult<>(
            page.content().stream().map(OrderDto::toDto).toList(),
            page.page(),
            page.size(),
            page.totalElements(),
            page.totalPages()
        );
    }
}
