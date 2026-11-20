package com.sep.vox.infrastructure.persistence.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.grpc.test.autoconfigure.AutoConfigureTestGrpcTransport;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.config.ContainerTestConfig;
import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.school.SchoolQuotaPolicy;
import com.sep.vox.domain.repository.SchoolQuotaPolicyRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Trần phân phối quyết định trường được chia ra bao nhiêu, nên hai tính chất phải chắc: mặc định
 * KHÔNG đổi hành vi cũ, và tỷ lệ ngoài khoảng 0..1 không ghi xuống được.
 *
 * <p>Phép nhân ra "phần được chia" cũng nằm ở đây: nó là con số backend dùng để từ chối, và làm tròn
 * sai chiều một vài phần triệu đồng là đủ để một lần chia đúng luật bị chặn.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestGrpcTransport
@Transactional
class SchoolQuotaPolicyTests extends ContainerTestConfig {

    @Autowired
    private SchoolQuotaPolicyRepository schoolQuotaPolicyRepository;

    @PersistenceContext
    private EntityManager em;

    private final UUID schoolId = UUID.randomUUID();

    @Test
    void should_default_to_fully_distributable_when_the_school_never_set_anything() {
        // Không có dòng nào trong DB. Trường chưa đụng tới màn cấu hình phải thấy hành vi y hệt
        // trước V5 -- chia được toàn bộ ví.
        var policy = schoolQuotaPolicyRepository.findBySchoolIdAndQuotaType(schoolId, QuotaType.EXAM);

        assertThat(policy.getDistributableRatio()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(policy.distributableAmountOf(new BigDecimal("10000")))
            .isEqualByComparingTo(new BigDecimal("10000"));
    }

    @Test
    void should_take_the_configured_share_of_the_pool() {
        var policy = new SchoolQuotaPolicy(schoolId, QuotaType.EXAM, new BigDecimal("0.8000"), null, null);

        // Ví 10.000đ, trần 80% -> chia được 8.000đ. Đúng ví dụ đã thống nhất.
        assertThat(policy.distributableAmountOf(new BigDecimal("10000")))
            .isEqualByComparingTo(new BigDecimal("8000"));
    }

    @Test
    void should_round_the_distributable_share_down_not_up() {
        // Làm tròn LÊN sẽ cho chia nhiều hơn trần vài phần triệu đồng -- tức phá đúng cái ràng buộc
        // mà chính sách này sinh ra để đặt.
        var policy = new SchoolQuotaPolicy(schoolId, QuotaType.EXAM, new BigDecimal("0.3333"), null, null);

        var distributable = policy.distributableAmountOf(new BigDecimal("10000.0000005"));

        assertThat(distributable).isLessThanOrEqualTo(new BigDecimal("10000.0000005").multiply(new BigDecimal("0.3333")));
        assertThat(distributable.scale()).isEqualTo(6);
    }

    @Test
    void should_keep_one_policy_per_school_and_quota_type() {
        schoolQuotaPolicyRepository.upsertRatio(schoolId, QuotaType.EXAM, new BigDecimal("0.8000"));
        schoolQuotaPolicyRepository.upsertRatio(schoolId, QuotaType.EXAM, new BigDecimal("0.5000"));
        em.flush();

        // Lần đặt thứ hai GHI ĐÈ chứ không thêm dòng -- nếu không, hai dòng cùng (trường, loại) sẽ
        // cho ra trần khác nhau tuỳ dòng nào đọc trúng trước.
        assertThat(schoolQuotaPolicyRepository.findBySchoolId(schoolId)).hasSize(1);
        assertThat(schoolQuotaPolicyRepository.findBySchoolIdAndQuotaType(schoolId, QuotaType.EXAM)
            .getDistributableRatio()).isEqualByComparingTo(new BigDecimal("0.5000"));
    }

    @Test
    void should_hold_a_separate_policy_for_each_quota_type() {
        // Cả lý do tách theo loại: giữ lại 30% ví thi mà chia hết ví luyện nói.
        schoolQuotaPolicyRepository.upsertRatio(schoolId, QuotaType.EXAM, new BigDecimal("0.7000"));
        schoolQuotaPolicyRepository.upsertRatio(schoolId, QuotaType.PRACTICE, BigDecimal.ONE);
        em.flush();

        assertThat(schoolQuotaPolicyRepository.findBySchoolIdAndQuotaType(schoolId, QuotaType.EXAM)
            .getDistributableRatio()).isEqualByComparingTo(new BigDecimal("0.7000"));
        assertThat(schoolQuotaPolicyRepository.findBySchoolIdAndQuotaType(schoolId, QuotaType.PRACTICE)
            .getDistributableRatio()).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    void should_reject_a_ratio_above_one_at_the_database() {
        // Tỷ lệ > 1 cho chia vượt ví -- đúng thứ ràng buộc này sinh ra để cấm. Chốt ở tầng DB nên
        // một đường ghi mới quên kiểm tra vẫn không lọt được.
        //
        // Ngoại lệ bật ra ở upsertRatio(), KHÔNG phải flush(): id là @Generated(event = INSERT) với
        // DEFAULT uuidv7(), nên Hibernate phải chạy INSERT ... RETURNING id ngay lúc lưu để đọc lại
        // giá trị vừa sinh. Bọc flush() thì test xanh vì lý do sai -- xem SchoolDebtEventShapeTests.
        assertThatThrownBy(() ->
            schoolQuotaPolicyRepository.upsertRatio(schoolId, QuotaType.EXAM, new BigDecimal("1.5000")))
            .hasMessageContaining("chk_school_quota_policies_ratio_in_range");
    }

    @Test
    void should_reject_a_negative_ratio_at_the_database() {
        assertThatThrownBy(() ->
            schoolQuotaPolicyRepository.upsertRatio(schoolId, QuotaType.PRACTICE, new BigDecimal("-0.1000")))
            .hasMessageContaining("chk_school_quota_policies_ratio_in_range");
    }
}
