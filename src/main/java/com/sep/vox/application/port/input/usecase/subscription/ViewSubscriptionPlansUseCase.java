package com.sep.vox.application.port.input.usecase.subscription;

import com.sep.vox.domain.model.subscription.SubscriptionPlan;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.subscription.ViewSubscriptionPlansQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.SubscriptionPlanQueryRepository;
import com.sep.vox.application.response.input.subscription.ViewSubscriptionPlansResponse;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.SubscriptionPlanDto;
import com.sep.vox.domain.model.subscription.SubscriptionPlanStatus;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;

@Service
@RequiredArgsConstructor
public class ViewSubscriptionPlansUseCase implements IUseCase<ViewSubscriptionPlansQuery, PageResult<ViewSubscriptionPlansResponse>> {

    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final SubscriptionPlanQueryRepository subscriptionPlanQueryRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional(readOnly = true)
    public PageResult<ViewSubscriptionPlansResponse> execute(ViewSubscriptionPlansQuery input) {
        PageResult<SubscriptionPlan> page = isSystemAdmin()
            ? subscriptionPlanRepository.findAll(input.page(), input.size())
            : subscriptionPlanRepository.findByStatus(SubscriptionPlanStatus.ACTIVE, input.page(), input.size());

        // Computed once for the whole page, and computed GLOBALLY rather than within the page -- see
        // SubscriptionPlanQueryRepository#findMostPopularPlanId. Empty means no school has subscribed yet, in
        // which case no plan gets the label.
        UUID mostPopularPlanId = subscriptionPlanQueryRepository.findMostPopularPlanId().orElse(null);

        List<ViewSubscriptionPlansResponse> content = page.content().stream()
            .map(SubscriptionPlanDto::toDto)
            .map(plan -> new ViewSubscriptionPlansResponse(plan, plan.id().equals(mostPopularPlanId)))
            .toList();

        return new PageResult<>(content, page.page(), page.size(), page.totalElements(), page.totalPages());
    }

    // A system admin manages the plans, so they also need to see ARCHIVED ones (to know which plans
    // were discontinued, see replacedByPlanId...). A school only uses this list to register or renew,
    // so it sees ACTIVE plans only: an archived plan cannot be bought and need not be shown.
    //
    // This query also serves anonymous visitors (the "AI plans for schools" section of the public
    // landing page; /graphql is permitAll at the HTTP layer precisely to allow this).
    // isSystemAdmin() throws when there is no authenticated user, so "not signed in" is checked first
    // and treated as "not a system admin" instead of letting an error escape -- otherwise every
    // anonymous visitor would get an error and the page could not show any plan even though ACTIVE
    // plans exist.
    private boolean isSystemAdmin() {
        return userContextPort.findCurrentAuthenticatedUserId().isPresent()
            && userContextPort.isSystemAdmin();
    }
}
