package com.sep.vox.application.port.input.usecase.subscription;

import com.sep.vox.domain.model.subscription.SchoolSubscription;
import com.sep.vox.domain.model.subscription.SubscriptionPlan;
import java.time.Instant;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.service.SubscriptionPlanResolver;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.subscription.SchoolSubscriptionRenewalPreviewResponse;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;

/**
 * Cho trường xem trước gói nào sẽ được dùng khi gia hạn, và kỳ mới bắt đầu lúc nào.
 *
 * <p>Cần thiết vì gói đang dùng có thể đã bị lưu trữ và được gán gói thay thế -- lúc đó gia hạn tự
 * chuyển sang gói mới. Trường xác nhận bằng cách gửi lại renewalPlan.id làm acceptedPlanId khi đặt
 * đơn (RenewSchoolSubscriptionUseCase), thay vì bị âm thầm đổi gói.
 *
 * <p>KHÔNG nhận tham số nào: trường lấy từ token, và kỳ để gia hạn là kỳ gần nhất của chính trường
 * đó. Nhận subscriptionId từ ngoài thì {@code hasRole('SCHOOL_ADMIN')} không đủ để bảo vệ -- nó trả
 * lời "có phải school admin không", không trả lời "có phải kỳ CỦA TRƯỜNG NÀY không" -- nên lại phải
 * dựng một lớp kiểm quyền sở hữu trong use case. Không nhận id thì không có gì để kiểm.
 *
 * <p>Cũng vì thế mà không có nhánh System Admin: đây là thao tác của nhà trường. Admin muốn xem tình
 * trạng gói của một trường thì dùng màn quản trị gói đăng ký, không đi qua đường này.
 */
@Service
@RequiredArgsConstructor
public class PreviewSchoolSubscriptionRenewalUseCase
        implements IUseCase<Void, SchoolSubscriptionRenewalPreviewResponse> {

    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final SubscriptionPlanResolver subscriptionPlanResolver;
    private final UserContextPort userContextPort;

    @Override
    @Transactional(readOnly = true)
    public SchoolSubscriptionRenewalPreviewResponse execute(Void input) {
        UUID schoolId = userContextPort.getCurrentSchoolId();

        // Kỳ GẦN NHẤT, không lọc trạng thái: gia hạn muộn (gói hết hạn tuần trước) là đường phổ biến
        // nhất, mà ở đó không còn kỳ nào ACTIVE để hỏi.
        SchoolSubscription subscription = schoolSubscriptionRepository.findMostRecentBySchoolId(schoolId)
            .orElseThrow(() -> new NotFoundException(
                "Trường chưa từng đăng ký gói nào, hãy chọn gói từ danh sách thay vì gia hạn."));

        SubscriptionPlan currentPlan = subscriptionPlanRepository.findById(subscription.getSubscriptionPlanId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy gói"));
        // Ném lỗi nếu gói đã lưu trữ mà chưa có gói thay thế -- đúng ý: trường cần biết ngay là chưa
        // gia hạn được, thay vì thấy một màn xem trước đẹp đẽ rồi mới hỏng ở bước đặt đơn.
        SubscriptionPlan renewalPlan = subscriptionPlanResolver.resolveActivePlan(currentPlan);

        Instant now = Instant.now();
        Instant startsAt = schoolSubscriptionRepository.findUnfinishedBySchoolId(schoolId, now).stream()
            .findFirst()
            .map(s -> s.getEndDate())
            .orElse(now);

        return SchoolSubscriptionRenewalPreviewResponse.toResponse(currentPlan, renewalPlan, startsAt);
    }
}
