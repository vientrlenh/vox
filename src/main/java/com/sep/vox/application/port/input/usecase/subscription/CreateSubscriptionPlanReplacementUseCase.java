package com.sep.vox.application.port.input.usecase.subscription;

import com.sep.vox.domain.model.subscription.SubscriptionPlan;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.subscription.CreateSubscriptionPlanReplacementCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.model.subscription.SubscriptionPlanStatus;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;

/**
 * Creates a new plan (DRAFT) and IMMEDIATELY archives the plan currently on sale, pointing its replacedByPlanId at the
 * new plan -- merging the two steps "create a plan" + "stop selling, choose a replacement plan" into one, for the
 * "Create replacement plan" flow on the plan management page.
 *
 * <p>Does NOT check SubscriptionPlanReplacementValidator here: the new plan is still DRAFT, and the admin may still edit
 * the quota/duration before publishing. The terms (equal period/price, quota/duration not lower) are checked again by
 * PublishSubscriptionPlanUseCase at the moment this plan really takes on the replacement role -- see
 * SubscriptionPlanReplacementValidator.requireCompatibleTerms.
 */
@Service
@RequiredArgsConstructor
public class CreateSubscriptionPlanReplacementUseCase
        implements IUseCase<CreateSubscriptionPlanReplacementCommand, UUID> {

    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final CreateSubscriptionPlanUseCase createSubscriptionPlanUseCase;

    @Override
    @Transactional
    public UUID execute(CreateSubscriptionPlanReplacementCommand input) {
        SubscriptionPlan replacedPlan = subscriptionPlanRepository.findById(input.replacedPlanId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy gói"));

        // Only for plans ON SALE: an ARCHIVED plan that needs its replacement fixed goes through
        // UpdateSubscriptionPlanReplacementUseCase, and a DRAFT plan has no users yet so it needs no replacement.
        if (replacedPlan.getStatus() != SubscriptionPlanStatus.ACTIVE) {
            throw new IllegalArgumentException("Chỉ tạo được gói thay thế cho gói đang bán.");
        }

        UUID newPlanId = createSubscriptionPlanUseCase.execute(input.newPlan());

        replacedPlan.setReplacedByPlanId(newPlanId);
        replacedPlan.setStatus(SubscriptionPlanStatus.ARCHIVED);
        subscriptionPlanRepository.save(replacedPlan);

        return newPlanId;
    }
}
