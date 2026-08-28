package com.sep.vox.application.port.input.usecase.order;

import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.query.ViewOrderDetailsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.OrderDto;
import com.sep.vox.domain.repository.OrderRepository;

@Service
public class ViewOrderDetailsUseCase implements IUseCase<ViewOrderDetailsQuery, OrderDto> {

    private final OrderRepository orderRepository;
    private final UserContextPort userContextPort;

    public ViewOrderDetailsUseCase(OrderRepository orderRepository, UserContextPort userContextPort) {
        this.orderRepository = orderRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    public OrderDto execute(ViewOrderDetailsQuery input) {
        var order = orderRepository.findById(input.id())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy thông tin của đơn hàng yêu cầu"));
        if (!userContextPort.isSystemAdmin() && !order.getSchoolId().equals(userContextPort.getCurrentSchoolId())) {
            throw new ForbiddenException("Bạn không có quyền truy cập vào đơn hàng này");
        }
        return OrderDto.toDto(order);
    }
    
}
