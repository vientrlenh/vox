package com.sep.vox.application.port.input.usecase.order;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.port.input.query.ViewOrdersQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.OrderDto;
import com.sep.vox.domain.model.order.OrderStatus;
import com.sep.vox.domain.model.order.OrderType;
import com.sep.vox.domain.repository.OrderRepository;

/**
 * Danh sách đơn hàng toàn hệ thống cho System Admin.
 *
 * <p>Đơn PENDING là thông tin CHẨN ĐOÁN ở phía admin chứ không phải rác cần lọc: một đống đơn treo
 * cùng lúc là dấu hiệu cổng không gọi callback về, còn một đơn treo lâu bất thường là trường đang bị
 * khóa không đặt được đơn mới (xem CreateSubscriptionOrderUseCase).
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
        var page = orderRepository.findForAdmin(
            input.schoolId(),
            statusOf(input.status()),
            typeOf(input.type()),
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

    // null/rỗng = không lọc theo tiêu chí này. Chuẩn hóa CHỮ HOA vì đằng sau là Enum.valueOf phân biệt
    // hoa thường: "pending" trên query string sẽ ném lỗi dù người dùng không sai gì.
    private static OrderStatus statusOf(String status) {
        var normalized = StringNormalization.normalizeCode(status);
        if (normalized == null || normalized.isEmpty()) {
            return null;
        }
        try {
            return OrderStatus.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Trạng thái đơn hàng không hợp lệ: " + status);
        }
    }

    private static OrderType typeOf(String type) {
        var normalized = StringNormalization.normalizeCode(type);
        if (normalized == null || normalized.isEmpty()) {
            return null;
        }
        try {
            return OrderType.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Loại đơn hàng không hợp lệ: " + type);
        }
    }
}
