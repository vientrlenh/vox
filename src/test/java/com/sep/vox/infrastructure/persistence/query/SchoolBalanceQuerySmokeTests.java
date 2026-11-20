package com.sep.vox.infrastructure.persistence.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.grpc.test.autoconfigure.AutoConfigureTestGrpcTransport;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.school.SchoolBalanceEntry;
import com.sep.vox.domain.model.school.SchoolBalanceEntryType;
import com.sep.vox.domain.model.school.SchoolDebtEvent;
import com.sep.vox.domain.repository.SchoolBalanceEntryRepository;
import com.sep.vox.domain.repository.SchoolDebtEventRepository;
import com.sep.vox.support.IntegrationTest;

/**
 * Hai repository này dựng câu bằng {@code @Query} viết tay, và Spring Data chỉ phân tích chúng lúc
 * khởi động context -- nhưng một câu chạy được vẫn có thể trả về SAI thứ tự hoặc sai bộ lọc. Lớp này
 * chạy từng câu trên Postgres THẬT với dữ liệu thật để chốt ba thứ không phép kiểm nào khác bắt được:
 * bộ lọc theo loại bút toán, khoảng nửa mở {@code [from, to)}, và khoá sắp xếp phụ theo id.
 *
 * <p>Không kiểm nghiệp vụ -- đó là việc của test use case.
 *
 * <p>{@code @SpringBootTest} chứ KHÔNG phải {@code @DataJpaTest}, dù lớp này chỉ đụng tới hai
 * repository. Đã đo cả hai: chạy riêng một mình thì slice nhanh hơn (8,4s so với 11,5s), nhưng chạy
 * cùng bộ thì ngược hẳn -- 8 test ở đây mất 0,06s khi dùng chung context đã ấm với chục lớp
 * ...SmokeTests anh em, và 2,1s khi {@code @DataJpaTest} bắt Spring dựng thêm một context THỨ HAI chỉ
 * cho riêng nó. Trong CI không ai chạy lẻ một lớp, nên con số thứ hai mới là con số thật.
 */
@IntegrationTest
@AutoConfigureTestGrpcTransport
@Transactional
class SchoolBalanceQuerySmokeTests {

    @Autowired
    private SchoolBalanceEntryRepository schoolBalanceEntryRepository;

    @Autowired
    private SchoolDebtEventRepository schoolDebtEventRepository;

    private final UUID schoolId = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-08-29T10:00:00Z");
    private final Instant epoch = Instant.EPOCH;

    @Test
    void should_page_an_empty_ledger_without_filter() {
        PageResult<SchoolBalanceEntry> page = schoolBalanceEntryRepository.findBySchoolId(schoolId, null, epoch, now, 1, 20);

        assertThat(page.content()).isEmpty();
        assertThat(page.page()).isEqualTo(1);
        assertThat(page.totalElements()).isZero();
    }

    @Test
    void should_page_an_empty_ledger_with_an_entry_type_filter() {
        PageResult<SchoolBalanceEntry> page = schoolBalanceEntryRepository.findBySchoolId(
            schoolId, SchoolBalanceEntryType.OVERAGE_CHARGE, epoch, now, 1, 20);

        assertThat(page.content()).isEmpty();
    }

    @Test
    void should_sum_to_zero_when_the_school_has_no_entry_at_all() {
        // COALESCE inside the SUM: without COALESCE this returns null, and the creditedVnd field in
        // the schema is String!, so null breaks the whole response in the most common case.
        BigDecimal sum = schoolBalanceEntryRepository.sumAmountBySchoolIdAndEntryTypeInRange(
            schoolId, SchoolBalanceEntryType.TOP_UP, epoch, now);

        assertThat(sum).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void should_keep_the_stored_scale_so_tiny_practice_charges_survive() {
        // A practice attempt may cost only a few hundredths of a dong. The column is numeric(18,6):
        // if somewhere along the way there is a rounding to whole dong, this amount vanishes
        // completely.
        BigDecimal tiny = new BigDecimal("-0.041230");
        schoolBalanceEntryRepository.save(overageCharge(tiny, now));

        PageResult<SchoolBalanceEntry> page = schoolBalanceEntryRepository.findBySchoolId(schoolId, null, epoch, now.plusSeconds(1), 1, 20);

        assertThat(page.content()).hasSize(1);
        assertThat(page.content().get(0).getAmountVnd()).isEqualByComparingTo(tiny);
    }

    @Test
    void should_filter_by_entry_type() {
        schoolBalanceEntryRepository.save(overageCharge(new BigDecimal("-100.500000"), now));
        schoolBalanceEntryRepository.save(overageCharge(new BigDecimal("-200.250000"), now.plusSeconds(60)));

        PageResult<SchoolBalanceEntry> overage = schoolBalanceEntryRepository.findBySchoolId(
            schoolId, SchoolBalanceEntryType.OVERAGE_CHARGE, epoch, now.plusSeconds(3600), 1, 20);
        PageResult<SchoolBalanceEntry> topUps = schoolBalanceEntryRepository.findBySchoolId(
            schoolId, SchoolBalanceEntryType.TOP_UP, epoch, now.plusSeconds(3600), 1, 20);

        assertThat(overage.content()).hasSize(2);
        assertThat(topUps.content()).isEmpty();
    }

    @Test
    void should_treat_the_upper_bound_as_exclusive() {
        // [from, to) -- an entry that falls EXACTLY on the `to` boundary must not be counted,
        // otherwise two adjacent periods both receive it and the total for the whole year is larger
        // than the real total.
        schoolBalanceEntryRepository.save(overageCharge(new BigDecimal("-10.000000"), now));

        PageResult<SchoolBalanceEntry> excluded = schoolBalanceEntryRepository.findBySchoolId(schoolId, null, epoch, now, 1, 20);
        PageResult<SchoolBalanceEntry> included = schoolBalanceEntryRepository.findBySchoolId(schoolId, null, epoch, now.plusMillis(1), 1, 20);

        assertThat(excluded.content()).isEmpty();
        assertThat(included.content()).hasSize(1);
    }

    @Test
    void should_order_entries_sharing_one_instant_deterministically() {
        // One assessment session produces several entries at the SAME Instant. Without a secondary key by
        // id, the order among them is undefined, and pagination would repeat a row on this page and
        // then drop a row on the other. The id is a uuidv7, so it increases with write time.
        Instant sameInstant = now.truncatedTo(ChronoUnit.MICROS);
        SchoolBalanceEntry first = schoolBalanceEntryRepository.save(overageCharge(new BigDecimal("-1.000000"), sameInstant));
        SchoolBalanceEntry second = schoolBalanceEntryRepository.save(overageCharge(new BigDecimal("-2.000000"), sameInstant));
        SchoolBalanceEntry third = schoolBalanceEntryRepository.save(overageCharge(new BigDecimal("-3.000000"), sameInstant));

        PageResult<SchoolBalanceEntry> pageOne = schoolBalanceEntryRepository.findBySchoolId(
            schoolId, null, epoch, sameInstant.plusMillis(1), 1, 2);
        PageResult<SchoolBalanceEntry> pageTwo = schoolBalanceEntryRepository.findBySchoolId(
            schoolId, null, epoch, sameInstant.plusMillis(1), 2, 2);

        List<UUID> seen = pageOne.content().stream().map(entry -> entry.getId()).toList();
        List<UUID> rest = pageTwo.content().stream().map(entry -> entry.getId()).toList();

        assertThat(pageOne.content()).hasSize(2);
        assertThat(pageTwo.content()).hasSize(1);
        // No row slips through both pages, and the three rows that were written each appear exactly
        // once.
        assertThat(seen).doesNotContainAnyElementsOf(rest);
        assertThat(seen).containsAll(java.util.List.of(third.getId(), second.getId()));
        assertThat(rest).containsExactly(first.getId());
    }

    @Test
    void should_page_an_empty_debt_log() {
        PageResult<SchoolDebtEvent> page = schoolDebtEventRepository.findBySchoolId(schoolId, 1, 20);

        assertThat(page.content()).isEmpty();
        assertThat(page.page()).isEqualTo(1);
        assertThat(page.totalElements()).isZero();
    }

    private SchoolBalanceEntry overageCharge(BigDecimal amountVnd, Instant occurredAt) {
        return new SchoolBalanceEntry(
            schoolId, null, SchoolBalanceEntryType.OVERAGE_CHARGE,
            amountVnd, amountVnd, null, UUID.randomUUID(), null, QuotaType.ASSESSMENT,
            new BigDecimal("0.001000"), new BigDecimal("26647.5000"), null, null, occurredAt);
    }
}
