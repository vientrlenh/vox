package com.sep.vox.application.port.input.usecase.order;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.ViewMySchoolOrdersQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.OrderDto;
import com.sep.vox.domain.repository.OrderRepository;

/**
 * Lịch sử đơn hàng của chính trường đang đăng nhập.
 *
 * <p>KHÔNG lọc bỏ đơn PENDING. Đây không phải lựa chọn hiển thị mà là ràng buộc:
 * CreateSubscriptionOrderUseCase từ chối đơn đăng ký mới khi trường còn một đơn treo, với thông báo
 * "hãy hoàn tất hoặc hủy đơn đó trước" -- giấu đơn treo đi thì trường được bảo phải hủy một thứ họ
 * không nhìn thấy. Đơn PENDING cũng là dòng DUY NHẤT còn hành động được (trả tiếp hoặc hủy); mọi
 * trạng thái khác chỉ để đọc.
 *
 * <p>Không kèm order_items: đơn nạp thêm vốn không có dòng nào, còn đơn gói đã có description chốt
 * sẵn lúc tạo ("Đăng ký {tên gói}") nên danh sách tự mô tả được mà không cần join.
 */
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
