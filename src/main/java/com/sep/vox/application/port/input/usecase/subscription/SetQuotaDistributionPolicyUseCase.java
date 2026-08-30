package com.sep.vox.application.port.input.usecase.subscription;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.command.SetQuotaDistributionPolicyCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.repository.SchoolQuotaPolicyRepository;

/**
 * Đặt trần phân phối hạn mức của trường cho MỘT loại hạn mức.
 *
 * <p>Hạ trần xuống dưới mức ĐÃ CHIA là hợp lệ và cố ý không bị từ chối: phần đã chia là chuyện đã
 * rồi, còn cấm hạ trần thì quản trị viên mắc kẹt không siết lại được chính sách. Giao diện hiện phần
 * vượt để họ biết cần thu bớt của ai, và mọi lần chia TIẾP theo đều bị chặn tới khi tổng về dưới trần.
 */
@Service
public class SetQuotaDistributionPolicyUseCase implements IUseCase<SetQuotaDistributionPolicyCommand, BigDecimal> {

    private final SchoolQuotaPolicyRepository schoolQuotaPolicyRepository;
    private final UserContextPort userContextPort;

    public SetQuotaDistributionPolicyUseCase(
            SchoolQuotaPolicyRepository schoolQuotaPolicyRepository,
            UserContextPort userContextPort) {
        this.schoolQuotaPolicyRepository = schoolQuotaPolicyRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public BigDecimal execute(SetQuotaDistributionPolicyCommand input) {
        if (!userContextPort.isSystemAdmin() && !input.schoolId().equals(userContextPort.getCurrentSchoolId())) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        var ratio = input.distributableRatio();
        // DB có CHECK giữ khoảng này, nhưng chặn ở đây để lỗi ra thành câu tiếng Việt đọc được thay
        // vì một DataIntegrityViolationException.
        if (ratio == null || ratio.compareTo(BigDecimal.ZERO) < 0 || ratio.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Trần phân phối phải nằm trong khoảng 0% tới 100%");
        }

        var quotaType = parseQuotaType(input.quotaType());
        return schoolQuotaPolicyRepository
            .upsertRatio(input.schoolId(), quotaType, ratio)
            .getDistributableRatio();
    }

    private static QuotaType parseQuotaType(String quotaType) {
        try {
            return QuotaType.valueOf(quotaType);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Loại hạn mức không hợp lệ: " + quotaType);
        }
    }
}
