package com.sep.vox.application.port.input.usecase.order;

import com.sep.vox.domain.model.order.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.query.order.ViewOrderDetailsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.OrderDto;
import com.sep.vox.domain.repository.OrderRepository;

@Service
@RequiredArgsConstructor
public class ViewOrderDetailsUseCase implements IUseCase<ViewOrderDetailsQuery, OrderDto> {

    private final OrderRepository orderRepository;
    private final UserContextPort userContextPort;

    @Override
    public OrderDto execute(ViewOrderDetailsQuery input) {
        Order order = orderRepository.findById(input.id())
            .orElseThrow(() -> new ResourceNotFoundException("The requested order was not found"));
        if (!userContextPort.isSystemAdmin() && !order.getSchoolId().equals(userContextPort.getCurrentSchoolId())) {
            throw new ForbiddenException("You are not allowed to access this order");
        }
        return OrderDto.toDto(order);
    }
    
}
