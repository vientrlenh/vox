package com.sep.vox.application.port.input.usecase.balance;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.ViewSchoolBalanceSummaryQuery;
import com.sep.vox.application.port.input.service.SchoolScopedReadGuard;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.response.input.balance.SchoolBalanceSummaryResponse;
import com.sep.vox.domain.model.school.SchoolBalanceEntryType;
import com.sep.vox.domain.repository.SchoolBalanceEntryRepository;

/**
 * Cộng dồn sổ cái ví theo một khoảng thời gian.
 *
 * <p>Bốn lượt cộng dồn riêng chứ không một câu GROUP BY: mỗi lượt dùng lại đúng
 * {@code sumAmountBySchoolIdAndEntryTypeInRange} đã có sẵn (đã COALESCE về 0, đã theo khoảng nửa mở),
 * và cả bốn đều chạy trên {@code idx_school_balance_entries_school_occurred}. Thêm một câu SQL mới
 * chỉ để gộp bốn phép cộng vào một lượt quét là đổi một thứ đang đúng lấy một thứ phải kiểm lại.
 */
@Service
public class ViewSchoolBalanceSummaryUseCase
        implements IUseCase<ViewSchoolBalanceSummaryQuery, SchoolBalanceSummaryResponse> {

    private final SchoolBalanceEntryRepository schoolBalanceEntryRepository;
    private final SchoolScopedReadGuard schoolScopedReadGuard;

    public ViewSchoolBalanceSummaryUseCase(
            SchoolBalanceEntryRepository schoolBalanceEntryRepository,
            SchoolScopedReadGuard schoolScopedReadGuard) {
        this.schoolBalanceEntryRepository = schoolBalanceEntryRepository;
        this.schoolScopedReadGuard = schoolScopedReadGuard;
    }

    @Override
    @Transactional(readOnly = true)
    public SchoolBalanceSummaryResponse execute(ViewSchoolBalanceSummaryQuery input) {
        schoolScopedReadGuard.requireCanRead(input.schoolId());

        var from = input.from() == null ? Instant.EPOCH : input.from();
        var to = input.to() == null ? Instant.now() : input.to();

        return SchoolBalanceSummaryResponse.of(
            sum(input, SchoolBalanceEntryType.TOP_UP, from, to),
            sum(input, SchoolBalanceEntryType.REFUND, from, to),
            sum(input, SchoolBalanceEntryType.OVERAGE_CHARGE, from, to),
            sum(input, SchoolBalanceEntryType.ADJUSTMENT, from, to)
        );
    }

    private BigDecimal sum(
            ViewSchoolBalanceSummaryQuery input, SchoolBalanceEntryType entryType, Instant from, Instant to) {
        return schoolBalanceEntryRepository
            .sumAmountBySchoolIdAndEntryTypeInRange(input.schoolId(), entryType, from, to);
    }
}
