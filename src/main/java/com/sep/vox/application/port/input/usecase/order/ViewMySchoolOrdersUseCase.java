package com.sep.vox.application.port.input.usecase.order;

import com.sep.vox.domain.model.order.Order;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.order.ViewMySchoolOrdersQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.OrderDto;
import com.sep.vox.domain.repository.OrderRepository;

/**
 * The order history of the school that is logged in.
 *
 * <p>Does NOT filter out PENDING orders. This is not a display choice but a constraint:
 * CreateSubscriptionOrderUseCase rejects a new subscription order while the school still has an open one, with
 * the message "complete or cancel that order first" -- hiding the open order would tell the school to cancel
 * something it cannot see. A PENDING order is also the ONLY row that can still be acted on (pay again or
 * cancel); every other status is read-only.
 *
 * <p>No order_items are attached: a top-up order has no item rows, and a plan order already has its description
 * fixed at creation time (the Vietnamese "Subscribe {plan name}"), so the list describes itself without a join.
 */
@Service
@RequiredArgsConstructor
public class ViewMySchoolOrdersUseCase implements IUseCase<ViewMySchoolOrdersQuery, PageResult<OrderDto>> {

    private final OrderRepository orderRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional(readOnly = true)
    public PageResult<OrderDto> execute(ViewMySchoolOrdersQuery input) {
        UUID schoolId = userContextPort.getCurrentSchoolId();
        PageResult<Order> page = orderRepository.findBySchoolId(schoolId, input.page(), input.size());

        return new PageResult<>(
            page.content().stream().map(OrderDto::toDto).toList(),
            page.page(),
            page.size(),
            page.totalElements(),
            page.totalPages()
        );
    }
}
