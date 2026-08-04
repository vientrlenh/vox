package com.sep.vox.application.port.input.usecase.subscription;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.CancelSubscriptionCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.SchoolSubscriptionDto;
import com.sep.vox.domain.mapper.SchoolSubscriptionDtoMapper;
import com.sep.vox.domain.model.subscription.FinancialEvent;
import com.sep.vox.domain.model.subscription.FinancialEventType;
import com.sep.vox.domain.model.subscription.PaymentMethod;
import com.sep.vox.domain.model.subscription.SubscriptionStatus;
import com.sep.vox.domain.repository.FinancialEventRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;

// "Hủy" kiểu Claude: chỉ đánh dấu sẽ không gia hạn nữa (cancelledAt), KHÔNG cắt quyền dùng ngay —
// trường đã trả tiền cho cả kỳ nên vẫn dùng bình thường (status giữ ACTIVE) tới hết endDate. Không
// có gì để hoàn tiền vì không mất ngày nào đã trả. SubscriptionExpiryJob sẽ tự chuyển sang EXPIRED
// khi endDate tới, dù có bị hủy hay không.
@Service
public class CancelSubscriptionUseCase implements IUseCase<CancelSubscriptionCommand, SchoolSubscriptionDto> {

    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final FinancialEventRepository financialEventRepository;
    private final UserContextPort userContextPort;

    public CancelSubscriptionUseCase(
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            FinancialEventRepository financialEventRepository,
            UserContextPort userContextPort) {
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.financialEventRepository = financialEventRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public SchoolSubscriptionDto execute(CancelSubscriptionCommand input) {
        if (!userContextPort.isSystemAdmin() && !input.schoolId().equals(userContextPort.getCurrentSchoolId())) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        var subscription = schoolSubscriptionRepository.findById(input.subscriptionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy gói đăng ký"));
        if (!subscription.getSchoolId().equals(input.schoolId())) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new IllegalStateException("Gói đăng ký không ở trạng thái đang hoạt động");
        }
        if (subscription.getCancelledAt() != null) {
            throw new IllegalStateException("Gói đăng ký đã được hủy trước đó");
        }

        var now = Instant.now();
        subscription.setCancelledAt(now);
        var saved = schoolSubscriptionRepository.save(subscription);

        financialEventRepository.save(new FinancialEvent(
            input.schoolId(),
            saved.getId(),
            FinancialEventType.SUB_CANCELLED,
            BigDecimal.ZERO,
            "VND",
            PaymentMethod.MANUAL,
            userContextPort.getCurrentAuthenticatedUserId(),
            null,
            now
        ));

        return SchoolSubscriptionDtoMapper.toDto(saved);
    }
}
