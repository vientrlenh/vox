package com.sep.vox.application.port.input.usecase.dashboard;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.ViewSchoolAiCostQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.SchoolAiCostQueryRepository;
import com.sep.vox.application.response.input.dashboard.SchoolAiCostTimeseriesResponse;
import com.sep.vox.domain.common.DecimalText;

/**
 * Chi phí AI của trường theo thời gian, đọc từ sổ {@code school_ai_spend_entries}.
 *
 * <p>Thay cho {@code schoolTokenUsageTimeseries} — trường GraphQL đó chưa bao giờ tồn tại trong
 * schema, và repository duy nhất có thể phục vụ nó đọc một bảng không mã nào còn ghi vào. Xem V10.
 */
@Service
public class ViewSchoolAiCostTimeseriesUseCase
        implements IUseCase<ViewSchoolAiCostQuery, SchoolAiCostTimeseriesResponse> {

    /** Bỏ trống mốc đầu = lùi 30 ngày, cùng mặc định mà thẻ trên trang tổng quan vẫn hiển thị. */
    private static final int DEFAULT_WINDOW_DAYS = 30;

    private final UserContextPort userContextPort;
    private final SchoolAiCostQueryRepository schoolAiCostQueryRepository;

    public ViewSchoolAiCostTimeseriesUseCase(UserContextPort userContextPort,
            SchoolAiCostQueryRepository schoolAiCostQueryRepository) {
        this.userContextPort = userContextPort;
        this.schoolAiCostQueryRepository = schoolAiCostQueryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public SchoolAiCostTimeseriesResponse execute(ViewSchoolAiCostQuery input) {
        var schoolId = userContextPort.getCurrentSchoolId();
        var to = input.to() == null ? Instant.now() : input.to();
        var from = input.from() == null ? to.minus(DEFAULT_WINDOW_DAYS, ChronoUnit.DAYS) : input.from();

        var buckets = schoolAiCostQueryRepository.findBucketedCost(
            schoolId, from, to, input.granularity().sqlUnit());

        // Cộng ở Java trên tập ĐÃ gom nhóm (tối đa vài chục dòng), không phải một câu SUM thứ hai:
        // hai câu riêng có thể lệch nhau nếu bút toán được ghi xen vào giữa, và lúc đó tổng ở đầu thẻ
        // không bằng tổng các cột ngay bên dưới nó.
        var total = buckets.stream()
            .map(bucket -> bucket.costVnd())
            .reduce(BigDecimal.ZERO, (left, right) -> left.add(right));

        var recordedFrom = schoolAiCostQueryRepository.findFirstRecordedAt(schoolId);

        return new SchoolAiCostTimeseriesResponse(
            input.granularity().name(),
            DecimalText.of(total),
            buckets.stream()
                .map(bucket -> new SchoolAiCostTimeseriesResponse.AiCostPointResponse(
                    bucket.bucket().toString(),
                    bucket.quotaType(),
                    DecimalText.of(bucket.costVnd())))
                .toList(),
            recordedFrom == null ? null : recordedFrom.toString()
        );
    }
}
