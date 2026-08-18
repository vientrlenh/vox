package com.sep.vox.application.port.input.usecase.subscription;

import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.ViewPlansQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.SubscriptionPlanQueryRepository;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.SubscriptionPlanDto;
import com.sep.vox.domain.model.subscription.PlanStatus;

@Service
public class ViewPlansUseCase implements IUseCase<ViewPlansQuery, PageResult<SubscriptionPlanDto>> {

    private final SubscriptionPlanQueryRepository subscriptionPlanQueryRepository;
    private final UserContextPort userContextPort;

    public ViewPlansUseCase(
            SubscriptionPlanQueryRepository subscriptionPlanQueryRepository,
            UserContextPort userContextPort) {
        this.subscriptionPlanQueryRepository = subscriptionPlanQueryRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<SubscriptionPlanDto> execute(ViewPlansQuery input) {
        var pageRequest = PageRequest.of(input.page(), input.size());
        // System admin quản lý gói nên cần thấy cả ARCHIVED (để biết gói nào đã ngừng bán, xem
        // replacedByPlanId...). Trường chỉ dùng danh sách này để đăng ký/gia hạn nên chỉ thấy
        // ACTIVE — gói archived không mua được thì không cần hiện ra.
        //
        // Query này còn phục vụ khách vãng lai chưa đăng nhập (mục "Gói AI dành cho nhà trường"
        // ở landing page công khai -- /graphql permitAll ở tầng HTTP đúng để cho phép việc này).
        // isSystemAdmin() throw AuthenticationCredentialsNotFoundException khi không có JWT, nên
        // phải bắt riêng và coi "chưa đăng nhập" như "không phải system admin" thay vì để lỗi văng
        // ra ngoài -- nếu không thì mọi khách vãng lai đều nhận lỗi và trang không hiện được gói
        // nào dù đã có gói ACTIVE.
        boolean isSystemAdmin;
        try {
            isSystemAdmin = userContextPort.isSystemAdmin();
        } catch (AuthenticationCredentialsNotFoundException notAuthenticated) {
            isSystemAdmin = false;
        }

        var page = isSystemAdmin
            ? subscriptionPlanQueryRepository.findAll(pageRequest)
            : subscriptionPlanQueryRepository.findAllByStatus(PlanStatus.ACTIVE, pageRequest);

        return new PageResult<>(page.getContent(), input.page(), input.size(), page.getTotalElements(), page.getTotalPages());
    }
}
