package com.sep.vox.application.port.input.service;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.PlanLimitExceededException;
import com.sep.vox.domain.common.ZoneConstant;
import com.sep.vox.domain.model.subscription.SchoolSubscription;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;

/**
 * Khung mở/đóng bài của kỳ thi và bài kiểm tra trên lớp phải nằm trong phiên thuê bao trường đã mua.
 *
 * <p>Không có ràng buộc này thì trường đặt được lịch thi ngoài hạn gói, và bài đó chỉ vỡ lúc chạy
 * thật (ClassTestTokenQuotaGuardService / StartClassTestSessionUseCase) -- tức là sau khi giáo viên
 * đã soạn đề và xếp học sinh xong.
 *
 * <p>Cố ý so trực tiếp với {@code start_date}/{@code end_date} thay vì tin vào
 * {@code status = ACTIVE}: việc hạ ACTIVE → EXPIRED do {@code SubscriptionExpiryJob} chạy mỗi giờ,
 * nên ngay sau khi gói hết hạn vẫn còn một khoảng repository trả về gói cũ.
 */
@Service
public class SubscriptionPeriodGuardService {

    // Instant.toString() in ra UTC kèm hậu tố Z, nên hạn gói bắt đầu 01/01 hiện thành
    // "2025-12-31T17:00:00Z" -- lệch một ngày so với chính con số trên màn quản lý thuê bao, đúng
    // vào lúc người đọc đang cố đối chiếu hai chỗ với nhau.
    private static final DateTimeFormatter DISPLAY_DATE =
        DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneConstant.BUSINESS_ZONE);

    private final SchoolSubscriptionRepository schoolSubscriptionRepository;

    public SubscriptionPeriodGuardService(SchoolSubscriptionRepository schoolSubscriptionRepository) {
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
    }

    /**
     * @param openAt  bỏ qua nếu null -- việc bắt buộc nhập thuộc về use case, không phải guard này
     * @param closeAt bỏ qua nếu null, cùng lý do
     */
    public void requireWithinSubscriptionPeriod(UUID schoolId, Instant openAt, Instant closeAt) {
        if (schoolId == null || (openAt == null && closeAt == null)) {
            return;
        }

        var subscription = schoolSubscriptionRepository.findActiveBySchoolId(schoolId)
            .orElseThrow(() -> new PlanLimitExceededException(
                "Trường chưa có gói dịch vụ đang hoạt động, không thể đặt thời gian cho bài kiểm tra"));

        var startDate = subscription.getStartDate();
        var endDate = subscription.getEndDate();
        // Gói thiếu ngày là dữ liệu hỏng, không phải "không giới hạn" -- nhưng chặn ở đây sẽ làm cả
        // trường đứng hình vì một lỗi dữ liệu người dùng cuối không sửa được.
        if (startDate == null || endDate == null) {
            return;
        }

        // end_date là mốc EXCLUSIVE, đúng bằng thời điểm kỳ kết thúc -- khớp "end_date >
        // CURRENT_TIMESTAMP" ở các native query và "endDate > :at" ở findInForceBySchoolId.
        //
        // KHÔNG cộng thêm một ngày nữa. Bản cũ cộng vì end_date từng là LocalDate, tức mốc theo NGÀY
        // nên cận trên thật sự là hết ngày hôm đó. V2 đổi cột sang timestamptz và model sang Instant,
        // nên end_date giờ đã là chính xác lúc kỳ hết hạn; cộng thêm 24 giờ là mở cửa cho lịch thi
        // nằm NGOÀI hạn gói -- bài xếp lịch trót lọt rồi chết lúc chạy thật ở
        // ClassTestTokenQuotaGuardService, đúng cái lỗi muộn mà guard này sinh ra để chặn.
        if (openAt != null && isOutside(openAt, startDate, endDate)) {
            throw new IllegalStateException(outsidePeriodMessage("Thời gian mở bài", subscription));
        }
        if (closeAt != null && isOutside(closeAt, startDate, endDate)) {
            throw new IllegalStateException(outsidePeriodMessage("Thời gian đóng bài", subscription));
        }
    }

    private boolean isOutside(Instant value, Instant lowerBound, Instant upperBoundExclusive) {
        return value.isBefore(lowerBound) || !value.isBefore(upperBoundExclusive);
    }

    /**
     * Hiển thị theo ngày giờ VN thay vì {@code Instant.toString()} (UTC, hậu tố Z) -- người dùng đối
     * chiếu với hạn gói đang hiện trên màn quản lý thuê bao, vốn cũng là ngày địa phương.
     */
    private String outsidePeriodMessage(String fieldLabel, SchoolSubscription subscription) {
        return "%s phải nằm trong hạn gói dịch vụ của trường (từ %s đến %s)".formatted(
            fieldLabel,
            DISPLAY_DATE.format(subscription.getStartDate()),
            DISPLAY_DATE.format(subscription.getEndDate()));
    }

}
