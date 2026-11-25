package com.sep.vox.application.port.input.service;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.BusinessRuleViolationException;
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
@RequiredArgsConstructor
public class SubscriptionPeriodGuardService {

    // Instant.toString() prints UTC with a trailing Z, so a plan period starting 01/01 shows as
    // "2025-12-31T17:00:00Z" -- one day off from the number on the subscription management screen, exactly
    // when the reader is trying to compare the two places.
    private static final DateTimeFormatter DISPLAY_DATE =
        DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneConstant.BUSINESS_ZONE);

    private final SchoolSubscriptionRepository schoolSubscriptionRepository;

    /**
     * @param openAt  skipped when null -- making it mandatory belongs to the use case, not to this guard
     * @param closeAt skipped when null, same reason
     */
    public void requireWithinSubscriptionPeriod(UUID schoolId, Instant openAt, Instant closeAt) {
        if (schoolId == null || (openAt == null && closeAt == null)) {
            return;
        }

        SchoolSubscription subscription = schoolSubscriptionRepository.findActiveBySchoolId(schoolId)
            .orElseThrow(() -> new BusinessRuleViolationException(
                "Trường chưa có gói dịch vụ đang hoạt động, không thể đặt thời gian cho bài kiểm tra"));

        Instant startDate = subscription.getStartDate();
        Instant endDate = subscription.getEndDate();
        // A plan without dates is corrupt data, not "unlimited" -- but blocking here would freeze the whole school
        // over a data error the end user cannot fix.
        if (startDate == null || endDate == null) {
            return;
        }

        requireInsidePeriod(subscription, startDate, endDate, openAt, closeAt);
    }

    private void requireInsidePeriod(
            SchoolSubscription subscription, Instant startDate, Instant endDate, Instant openAt, Instant closeAt) {
        // end_date is an EXCLUSIVE bound, exactly the moment the period ends -- it matches "end_date >
        // CURRENT_TIMESTAMP" in the native queries and "endDate > :at" in findInForceBySchoolId.
        //
        // Do NOT add one more day. The old version added it because end_date used to be a LocalDate,
        // a DAY-level bound whose real upper limit was the end of that day. V2 changed the column to
        // timestamptz and the model to Instant, so end_date is now exactly the moment the period
        // expires; adding 24 hours would let a schedule fall OUTSIDE the plan period -- the schedule
        // would be accepted and then fail at run time in ClassTestTokenQuotaGuardService, the very
        // late failure this guard exists to prevent.
        if (openAt != null && isOutside(openAt, startDate, endDate)) {
            throw new IllegalArgumentException(outsidePeriodMessage("Thời gian mở bài", subscription));
        }
        if (closeAt != null && isOutside(closeAt, startDate, endDate)) {
            throw new IllegalArgumentException(outsidePeriodMessage("Thời gian đóng bài", subscription));
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
