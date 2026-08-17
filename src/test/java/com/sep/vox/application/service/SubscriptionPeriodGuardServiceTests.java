package com.sep.vox.application.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.exception.PlanLimitExceededException;
import com.sep.vox.application.port.input.service.SubscriptionPeriodGuardService;
import com.sep.vox.domain.model.subscription.SchoolSubscription;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;

/**
 * Khung mở/đóng bài phải nằm trong phiên thuê bao của trường. Điểm dễ sai nhất là biên trên:
 * {@code school_subscription.end_date} là kiểu {@code date} và INCLUSIVE (các native query dùng
 * {@code CURRENT_DATE BETWEEN start_date AND end_date}), nên cận trên thật sự là HẾT ngày end_date
 * chứ không phải 00:00 của ngày đó -- so sai một nhịp là chặn oan toàn bộ ngày cuối của gói.
 */
class SubscriptionPeriodGuardServiceTests {

    private SchoolSubscriptionRepository schoolSubscriptionRepository;
    private SubscriptionPeriodGuardService guard;

    private final UUID schoolId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        schoolSubscriptionRepository = mock(SchoolSubscriptionRepository.class);
        guard = new SubscriptionPeriodGuardService(schoolSubscriptionRepository);
        givenSubscription(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
    }

    @Test
    void should_pass_when_window_inside_subscription_period() {
        assertThatCode(() -> guard.requireWithinSubscriptionPeriod(
            schoolId, at("2026-06-01T07:00"), at("2026-06-01T09:00")))
            .doesNotThrowAnyException();
    }

    /** Biên dưới INCLUSIVE: 00:00 đúng ngày start_date vẫn phải qua. */
    @Test
    void should_pass_when_open_at_is_first_moment_of_start_date() {
        assertThatCode(() -> guard.requireWithinSubscriptionPeriod(
            schoolId, at("2026-01-01T00:00"), at("2026-01-01T09:00")))
            .doesNotThrowAnyException();
    }

    /** Biên trên INCLUSIVE: cả ngày end_date vẫn nằm trong hạn gói. */
    @Test
    void should_pass_when_close_at_is_last_moment_of_end_date() {
        assertThatCode(() -> guard.requireWithinSubscriptionPeriod(
            schoolId, at("2026-12-31T07:00"), at("2026-12-31T23:59")))
            .doesNotThrowAnyException();
    }

    @Test
    void should_reject_when_open_at_before_subscription_start() {
        assertThatThrownBy(() -> guard.requireWithinSubscriptionPeriod(
            schoolId, at("2025-12-31T23:00"), at("2026-01-02T09:00")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Thời gian mở bài")
            .hasMessageContaining("01/01/2026")
            .hasMessageContaining("31/12/2026");
    }

    @Test
    void should_reject_when_close_at_after_subscription_end() {
        assertThatThrownBy(() -> guard.requireWithinSubscriptionPeriod(
            schoolId, at("2026-06-01T07:00"), at("2027-01-05T09:00")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Thời gian đóng bài")
            .hasMessageContaining("31/12/2026");
    }

    /** Sang ngày kế tiếp end_date là ra ngoài hạn, dù chỉ hơn một phút. */
    @Test
    void should_reject_when_close_at_is_first_moment_after_end_date() {
        assertThatThrownBy(() -> guard.requireWithinSubscriptionPeriod(
            schoolId, at("2026-12-31T07:00"), at("2027-01-01T00:00")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Thời gian đóng bài");
    }

    /**
     * Cùng loại lỗi với các guard theo gói đã có (ExamTimeQuotaGuardService,
     * ClassTestTokenQuotaGuardService) để FE phân biệt được "lỗi do gói" với "lỗi do nhập liệu".
     */
    @Test
    void should_reject_when_school_has_no_active_subscription() {
        when(schoolSubscriptionRepository.findActiveBySchoolId(schoolId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> guard.requireWithinSubscriptionPeriod(
            schoolId, at("2026-06-01T07:00"), at("2026-06-01T09:00")))
            .isInstanceOf(PlanLimitExceededException.class)
            .hasMessageContaining("gói dịch vụ");
    }

    /**
     * Kỳ thi tập trung cũ có thể còn null một vế; guard không được biến thành nơi bắt buộc nhập
     * (việc đó thuộc về use case), chỉ soi những giá trị thật sự có.
     */
    @Test
    void should_ignore_null_dates() {
        assertThatCode(() -> guard.requireWithinSubscriptionPeriod(schoolId, null, null))
            .doesNotThrowAnyException();
        assertThatCode(() -> guard.requireWithinSubscriptionPeriod(schoolId, at("2026-06-01T07:00"), null))
            .doesNotThrowAnyException();
    }

    @Test
    void should_skip_when_school_id_is_null() {
        assertThatCode(() -> guard.requireWithinSubscriptionPeriod(
            null, at("2020-01-01T07:00"), at("2030-01-01T09:00")))
            .doesNotThrowAnyException();
    }

    /**
     * Gói có start_date/end_date null là dữ liệu hỏng chứ không phải "không giới hạn" -- nhưng chặn
     * ở đây thì mọi thao tác của trường đó đứng hình vì một lỗi dữ liệu không liên quan tới người
     * dùng cuối, nên bỏ qua ràng buộc thay vì ném.
     */
    @Test
    void should_skip_when_subscription_has_no_period() {
        givenSubscription(null, null);

        assertThatCode(() -> guard.requireWithinSubscriptionPeriod(
            schoolId, at("2020-01-01T07:00"), at("2030-01-01T09:00")))
            .doesNotThrowAnyException();
    }

    private void givenSubscription(LocalDate startDate, LocalDate endDate) {
        var subscription = new SchoolSubscription();
        subscription.setId(UUID.randomUUID());
        subscription.setSchoolId(schoolId);
        subscription.setStartDate(startDate);
        subscription.setEndDate(endDate);
        when(schoolSubscriptionRepository.findActiveBySchoolId(schoolId)).thenReturn(Optional.of(subscription));
    }

    /** Giờ địa phương VN -- cùng zone mà guard dùng để quy đổi start_date/end_date. */
    private Instant at(String localDateTime) {
        return LocalDateTime.parse(localDateTime).atZone(DateMapper.DEFAULT_INPUT_ZONE).toInstant();
    }
}
