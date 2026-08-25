package com.sep.vox.application.port.input.usecase.subscription;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.ArchivePlanCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.SubscriptionPlanDto;
import com.sep.vox.domain.mapper.SubscriptionPlanDtoMapper;
import com.sep.vox.domain.model.subscription.PlanStatus;
import com.sep.vox.domain.repository.PlanQuotaRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;

@Service
public class ArchivePlanUseCase implements IUseCase<ArchivePlanCommand, SubscriptionPlanDto> {

    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final PlanQuotaRepository planQuotaRepository;
    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final UserContextPort userContextPort;

    public ArchivePlanUseCase(
            SubscriptionPlanRepository subscriptionPlanRepository,
            PlanQuotaRepository planQuotaRepository,
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            UserContextPort userContextPort) {
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.planQuotaRepository = planQuotaRepository;
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public SubscriptionPlanDto execute(ArchivePlanCommand input) {
        if (!userContextPort.isSystemAdmin()) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        var plan = subscriptionPlanRepository.findById(input.planId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy gói"));

        if (plan.getStatus() == PlanStatus.DRAFT) {
            throw new IllegalStateException("Gói đang ở trạng thái nháp thì phải xóa cứng, không lưu trữ.");
        }
        if (plan.getStatus() == PlanStatus.ARCHIVED) {
            throw new IllegalStateException("Gói đã được lưu trữ trước đó.");
        }

        // existsActiveByPlanId đã có sẵn từ trước nhưng chưa từng được gọi -- gap thật: archive vô
        // điều kiện dù đang có trường ACTIVE dùng gói này sẽ khiến trường đó kẹt cứng, không gia hạn
        // được cho tới khi có ai đó tạo gói thay thế (SubscriptionPlanResolver.resolveActivePlan ném
        // lỗi lúc gia hạn). Bắt buộc chọn gói thay thế NGAY LÚC archive nếu đang có trường dùng.
        if (input.replacedByPlanId() == null && schoolSubscriptionRepository.existsActiveByPlanId(plan.getId())) {
            throw new IllegalArgumentException(
                "Gói đang có trường sử dụng, phải chọn gói thay thế hoặc đợi tới khi không còn trường nào dùng gói này trước khi lưu trữ.");
        }

        if (input.replacedByPlanId() != null) {
            if (input.replacedByPlanId().equals(plan.getId())) {
                throw new IllegalArgumentException("Gói thay thế không được trùng với chính gói đang lưu trữ");
            }
            var replacement = subscriptionPlanRepository.findById(input.replacedByPlanId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy gói thay thế"));
            if (replacement.getStatus() != PlanStatus.ACTIVE) {
                throw new IllegalArgumentException("Gói thay thế phải đang ở trạng thái hoạt động");
            }
            // Không bắt buộc cùng giá gói cũ: trường bị ép đổi gói khi gia hạn (xem
            // SubscriptionPlanResolver.resolveActivePlan) vẫn được bảo vệ ở PreviewRenewalUseCase +
            // CreatePaymentLinkForRenewalUseCase -- 2 nơi đó bắt trường xem trước giá gói thay thế và
            // xác nhận đúng acceptedPlanId trước khi bị thu tiền, nên không cần chặn cứng giá ở đây.
            plan.setReplacedByPlanId(replacement.getId());
        }

        plan.setStatus(PlanStatus.ARCHIVED);
        var savedPlan = subscriptionPlanRepository.save(plan);

        return SubscriptionPlanDtoMapper.toDto(savedPlan, planQuotaRepository.findAllByPlanId(savedPlan.getId()));
    }
}
