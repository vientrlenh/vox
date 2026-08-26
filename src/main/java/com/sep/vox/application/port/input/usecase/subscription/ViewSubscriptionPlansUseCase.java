package com.sep.vox.application.port.input.usecase.subscription;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.ViewSubscriptionPlansQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.SubscriptionPlanQueryRepository;
import com.sep.vox.application.response.input.subscription.ViewSubscriptionPlansResponse;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.SubscriptionPlanDto;
import com.sep.vox.domain.model.subscription.SubscriptionPlanStatus;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;

@Service
public class ViewSubscriptionPlansUseCase implements IUseCase<ViewSubscriptionPlansQuery, PageResult<ViewSubscriptionPlansResponse>> {

    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final SubscriptionPlanQueryRepository subscriptionPlanQueryRepository;
    private final UserContextPort userContextPort;

    public ViewSubscriptionPlansUseCase(
            SubscriptionPlanRepository subscriptionPlanRepository,
            SubscriptionPlanQueryRepository subscriptionPlanQueryRepository,
            UserContextPort userContextPort) {
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.subscriptionPlanQueryRepository = subscriptionPlanQueryRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ViewSubscriptionPlansResponse> execute(ViewSubscriptionPlansQuery input) {
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
            ? subscriptionPlanRepository.findAll(input.page(), input.size())
            : subscriptionPlanRepository.findByStatus(SubscriptionPlanStatus.ACTIVE, input.page(), input.size());

        // Tính một lần cho cả trang, và tính TOÀN CỤC chứ không phải trong phạm vi trang -- xem
        // SubscriptionPlanQueryRepository#findMostPopularPlanId. Rỗng nghĩa là chưa trường nào đăng
        // ký, khi đó không gói nào được gắn nhãn.
        var mostPopularPlanId = subscriptionPlanQueryRepository.findMostPopularPlanId().orElse(null);

        var content = page.content().stream()
            .map(SubscriptionPlanDto::toDto)
            .map(plan -> new ViewSubscriptionPlansResponse(plan, plan.id().equals(mostPopularPlanId)))
            .toList();

        return new PageResult<>(content, page.page(), page.size(), page.totalElements(), page.totalPages());
    }
}
