package com.sep.vox.infrastructure.persistence.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.grpc.test.autoconfigure.AutoConfigureTestGrpcTransport;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.config.ContainerTestConfig;
import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.school.SchoolDebtEvent;
import com.sep.vox.domain.model.school.SchoolDebtEventType;
import com.sep.vox.domain.repository.SchoolDebtEventRepository;

/**
 * {@code chk_school_debt_events_shape_matches_event_type} là chốt chặn giữ cho hai nửa của bản sửa
 * V4 không trôi khỏi nhau: thêm cột nguồn mà quên truyền id xuống chỗ phát sự kiện thì INSERT phải
 * hỏng ngay, chứ không được âm thầm ghi thêm một dòng mất dấu vết nữa.
 *
 * <p>Ràng buộc sống ở tầng DB nên không phép kiểm nào ở tầng Java chạm tới được -- phải đẩy dòng
 * thật xuống Postgres thật mới biết nó canh đúng không. Và nó phải được khai CẢ ở @Table của
 * SchoolDebtEventJpaEntity: profile test chạy ddl-auto=create-drop, nên schema mà test đối mặt do
 * Hibernate dựng từ annotation, không phải do Flyway dựng từ migration.
 *
 * <p>LƯU Ý về nơi ngoại lệ bật ra: {@code save()} chứ KHÔNG phải {@code flush()}. Id của bảng này là
 * {@code @Generated(event = INSERT)} với {@code DEFAULT uuidv7()}, nên Hibernate buộc phải chạy
 * INSERT ngay lúc persist để đọc lại giá trị vừa sinh -- không có bước "xếp hàng chờ flush" nào. Bọc
 * {@code flush()} thì mọi test ở đây xanh giả: ngoại lệ đã bay ra từ dòng trước đó.
 *
 * <p>Xem SchoolBalanceQuerySmokeTests về lý do dùng {@code @SpringBootTest} thay vì slice.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestGrpcTransport
@Transactional
class SchoolDebtEventShapeTests extends ContainerTestConfig {

    private static final String SHAPE_CONSTRAINT = "chk_school_debt_events_shape_matches_event_type";

    @Autowired
    private SchoolDebtEventRepository schoolDebtEventRepository;

    private final UUID schoolId = UUID.randomUUID();
    private final UUID subscriptionId = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-08-29T10:00:00Z");

    @Test
    void should_accept_a_cleared_row_with_no_quota_bucket_and_no_trigger() {
        // Hết nợ là sự kiện cấp TRƯỜNG: số dư là MỘT con số dùng chung cho cả hai ví hạn mức, nên
        // không ví nào "vừa hết nợ". Ba cột đó phải để trống được, nếu không thì chỗ phát sự kiện
        // buộc phải bịa ra một giá trị và làm bẩn chính quyển sổ dùng để đối soát.
        var saved = schoolDebtEventRepository.save(new SchoolDebtEvent(
            schoolId, subscriptionId, SchoolDebtEventType.CLEARED,
            null, null, null, null, null, null, BigDecimal.ZERO, now));

        assertThat(saved.getQuotaType()).isNull();
        assertThat(saved.getTriggerExamSessionId()).isNull();
        assertThat(saved.getTriggerPracticeSessionId()).isNull();
        assertThat(saved.getOverageVnd()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void should_accept_a_debt_row_triggered_by_a_practice_session() {
        // Chính là hình dạng mà trước V4 không ghi nổi: nợ sinh từ phiên LUYỆN NÓI.
        var saved = schoolDebtEventRepository.save(new SchoolDebtEvent(
            schoolId, subscriptionId, SchoolDebtEventType.CAP_EXCEEDED,
            QuotaType.PRACTICE, null, UUID.randomUUID(), new BigDecimal("18402.60"),
            new BigDecimal("2400000"), new BigDecimal("2906310"), new BigDecimal("506310"), now));

        assertThat(saved.getTriggerPracticeSessionId()).isNotNull();
        assertThat(saved.getTriggerExamSessionId()).isNull();
    }

    @Test
    void should_reject_a_cleared_row_that_claims_a_quota_bucket() {
        assertThatThrownBy(() -> schoolDebtEventRepository.save(new SchoolDebtEvent(
            schoolId, subscriptionId, SchoolDebtEventType.CLEARED,
            QuotaType.EXAM, null, null, null,
            new BigDecimal("8000000"), new BigDecimal("8000000"), BigDecimal.ZERO, now)))
            .hasMessageContaining(SHAPE_CONSTRAINT);
    }

    @Test
    void should_reject_a_debt_row_with_no_trigger_session_at_all() {
        // Đây CHÍNH LÀ dòng mà bản trước V4 vẫn ghi được: CAP_EXCEEDED do luyện nói gây ra, nhưng
        // trigger_exam_session_id = NULL vì practiceSessionId không được truyền xuống. Sổ biết
        // trường vượt trần mà không biết vì khoản nào.
        assertThatThrownBy(() -> schoolDebtEventRepository.save(new SchoolDebtEvent(
            schoolId, subscriptionId, SchoolDebtEventType.CAP_EXCEEDED,
            QuotaType.PRACTICE, null, null, new BigDecimal("18402.60"),
            new BigDecimal("2400000"), new BigDecimal("2906310"), new BigDecimal("506310"), now)))
            .hasMessageContaining(SHAPE_CONSTRAINT);
    }

    @Test
    void should_reject_a_debt_row_naming_both_trigger_sessions() {
        assertThatThrownBy(() -> schoolDebtEventRepository.save(new SchoolDebtEvent(
            schoolId, subscriptionId, SchoolDebtEventType.LOCKED,
            QuotaType.EXAM, UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("96320.40"),
            new BigDecimal("8000000"), new BigDecimal("8045180"), new BigDecimal("45180"), now)))
            .hasMessageContaining(SHAPE_CONSTRAINT);
    }

    @Test
    void should_reject_a_cleared_row_that_still_carries_a_debt() {
        // overage_vnd = 0 là ĐỊNH NGHĨA của hết nợ, không phải một giá trị mặc định tiện tay.
        assertThatThrownBy(() -> schoolDebtEventRepository.save(new SchoolDebtEvent(
            schoolId, subscriptionId, SchoolDebtEventType.CLEARED,
            null, null, null, null, null, null, new BigDecimal("45180"), now)))
            .hasMessageContaining(SHAPE_CONSTRAINT);
    }
}
