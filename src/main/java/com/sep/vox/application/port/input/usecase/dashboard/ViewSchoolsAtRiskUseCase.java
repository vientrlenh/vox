package com.sep.vox.application.port.input.usecase.dashboard;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.ViewSchoolsAtRiskQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.repository.PlatformBusinessHealthQueryRepository;
import com.sep.vox.application.query.repository.SchoolsAtRiskQueryRepository;
import com.sep.vox.application.response.input.dashboard.SchoolsAtRiskResponse;
import com.sep.vox.domain.common.BusinessConstant;

/**
 * Trang "trường cần chú ý" — chỗ đáp của bốn dòng trên thẻ cùng tên ở trang tổng quan hệ thống.
 *
 * <p>MỘT trang, bốn bộ lọc, không phải bốn trang: bốn nhóm chỉ khác nhau ở vị từ trên cùng một bảng.
 *
 * <p>Số đếm lấy từ CHÍNH {@link PlatformBusinessHealthQueryRepository} mà trang tổng quan dùng, chứ
 * không đếm lại bằng câu khác. Đây là điểm dễ hỏng nhất của màn hình: thẻ ghi 5, người dùng bấm vào,
 * và một phép lọc khác sẽ cho ra 9 mà không ai biết số nào đúng.
 */
@Service
public class ViewSchoolsAtRiskUseCase implements IUseCase<ViewSchoolsAtRiskQuery, SchoolsAtRiskResponse> {

    private final PlatformBusinessHealthQueryRepository platformBusinessHealthQueryRepository;
    private final SchoolsAtRiskQueryRepository schoolsAtRiskQueryRepository;

    public ViewSchoolsAtRiskUseCase(
            PlatformBusinessHealthQueryRepository platformBusinessHealthQueryRepository,
            SchoolsAtRiskQueryRepository schoolsAtRiskQueryRepository) {
        this.platformBusinessHealthQueryRepository = platformBusinessHealthQueryRepository;
        this.schoolsAtRiskQueryRepository = schoolsAtRiskQueryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public SchoolsAtRiskResponse execute(ViewSchoolsAtRiskQuery input) {
        // MỘT mốc "bây giờ" cho cả phép đếm lẫn danh sách. Gọi Instant.now() hai lần thì một kỳ hết
        // hạn đúng giữa hai lời gọi sẽ khiến thẻ và bảng ngay bên dưới nó lệch nhau một trường, và
        // không có cách nào tái hiện được.
        var now = Instant.now();
        var expiringThrough = now.plus(BusinessConstant.DASHBOARD_EXPIRING_SOON_DAYS, ChronoUnit.DAYS);

        var health = platformBusinessHealthQueryRepository.countSchoolSubscriptionHealth(now, expiringThrough);
        var counts = new SchoolsAtRiskResponse.BucketCounts(
            health.expiringSoonSchools(),
            health.lapsedSchools(),
            health.suspendedSchools(),
            platformBusinessHealthQueryRepository.countSchoolsInDebt()
        );

        var schools = schoolsAtRiskQueryRepository.findByBucket(
            input.bucket(), now, expiringThrough, input.keyword(), input.page(), input.size());

        return new SchoolsAtRiskResponse(input.bucket(), counts, schools);
    }
}
