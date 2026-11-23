package com.sep.vox.application.port.input.usecase.subscription;

import com.sep.vox.domain.model.subscription.SubscriptionPlan;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.ArchiveSubscriptionPlanCommand;
import com.sep.vox.application.port.input.service.SubscriptionPlanReplacementValidator;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.model.subscription.SubscriptionPlanStatus;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;

@Service
@RequiredArgsConstructor
public class ArchiveSubscriptionPlanUseCase implements IUseCase<ArchiveSubscriptionPlanCommand, UUID> {

    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final SubscriptionPlanReplacementValidator replacementValidator;

    @Override
    @Transactional
    public UUID execute(ArchiveSubscriptionPlanCommand input) {
        SubscriptionPlan plan = subscriptionPlanRepository.findById(input.subscriptionPlanId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy gói"));

        if (plan.getStatus() == SubscriptionPlanStatus.DRAFT) {
            throw new IllegalStateException("Gói đang ở trạng thái nháp thì phải xóa cứng, không lưu trữ.");
        }
        if (plan.getStatus() == SubscriptionPlanStatus.ARCHIVED) {
            throw new IllegalStateException("Gói đã được lưu trữ trước đó.");
        }

        // existsActiveByPlanId already existed but was never called -- a real gap: archiving unconditionally while an ACTIVE
        // school still uses this plan would leave that school stuck, unable to renew until someone creates a replacement plan
        // (SubscriptionPlanResolver.resolveActivePlan throws on renewal). Choosing a replacement plan is mandatory AT THE TIME OF
        // archiving if any school is using it.
        //
        // The second clause (existsByReplacedByPlanId) patches the same hole ONE STEP FURTHER AWAY: plan A already points to B as
        // its replacement, and now B is archived without naming a next plan. No school is ACTIVE on B, so the first clause lets
        // it through, but the schools on A would walk the whole chain and stop at an ARCHIVED plan with no way out -- still stuck,
        // just harder to see.
        if (input.replacedByPlanId() == null) {
            if (schoolSubscriptionRepository.existsActiveByPlanId(plan.getId())) {
                throw new IllegalArgumentException(
                    "Gói đang có trường sử dụng, phải chọn gói thay thế hoặc đợi tới khi không còn trường nào dùng gói này trước khi lưu trữ.");
            }
            if (subscriptionPlanRepository.existsByReplacedByPlanId(plan.getId())) {
                throw new IllegalArgumentException(
                    "Gói này đang là gói thay thế của một gói khác, phải chọn gói thay thế tiếp theo trước khi lưu trữ.");
            }
        }

        if (input.replacedByPlanId() != null) {
            SubscriptionPlan replacement = subscriptionPlanRepository.findById(input.replacedByPlanId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy gói thay thế"));
            // A school forced to change plan on renewal (SubscriptionPlanResolver.resolveActivePlan) has no chance to refuse -- see
            // SubscriptionPlanReplacementValidator for the full set of rules.
            replacementValidator.requireValidReplacement(replacement, plan);
            plan.setReplacedByPlanId(replacement.getId());
        }
        plan.setStatus(SubscriptionPlanStatus.ARCHIVED);
        SubscriptionPlan savedPlan = subscriptionPlanRepository.save(plan);

        return savedPlan.getId();
    }

}
