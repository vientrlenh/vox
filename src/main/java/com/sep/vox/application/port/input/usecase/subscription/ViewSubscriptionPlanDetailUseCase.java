package com.sep.vox.application.port.input.usecase.subscription;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.query.ViewSubscriptionPlanDetailQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.SubscriptionPlanDto;
import com.sep.vox.domain.model.subscription.SubscriptionPlanStatus;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;

@Service
public class ViewSubscriptionPlanDetailUseCase implements IUseCase<ViewSubscriptionPlanDetailQuery, SubscriptionPlanDto> {

    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final UserContextPort userContextPort;

    public ViewSubscriptionPlanDetailUseCase(
            SubscriptionPlanRepository subscriptionPlanRepository,
            UserContextPort userContextPort) {
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionPlanDto execute(ViewSubscriptionPlanDetailQuery input) {
        var plan = subscriptionPlanRepository.findById(input.id())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy gói"));

        // Chỉ System Admin mới quản lý gói nên được xem mọi trạng thái (DRAFT/ARCHIVED để biết
        // gói nào chưa xuất bản/đã ngừng bán). Các role khác chỉ được xem gói đang bán — ẩn hẳn
        // sự tồn tại của gói DRAFT/ARCHIVED thay vì trả lỗi quyền, đồng nhất với ViewPlansUseCase.
        if (!userContextPort.isSystemAdmin() && plan.getStatus() != SubscriptionPlanStatus.ACTIVE) {
            throw new NotFoundException("Không tìm thấy gói");
        }

        return SubscriptionPlanDto.toDto(plan);
    }
}
