package com.sep.vox.application.port.input.usecase.dashboard;

import com.sep.vox.application.query.dto.AiCostBucketDto;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.dashboard.ViewSchoolAiCostQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.SchoolAiCostQueryRepository;
import com.sep.vox.application.response.input.dashboard.SchoolAiCostTimeseriesResponse;
import com.sep.vox.domain.common.DecimalText;

/**
 * The AI cost of a school over time, read from the {@code school_ai_spend_entries} ledger.
 *
 * <p>Replaces {@code schoolTokenUsageTimeseries} -- that GraphQL field never existed in the schema, and the only
 * repository that could serve it read a table that no code writes to any more. See V10.
 */
@Service
@RequiredArgsConstructor
public class ViewSchoolAiCostTimeseriesUseCase
        implements IUseCase<ViewSchoolAiCostQuery, SchoolAiCostTimeseriesResponse> {

    /** An empty start bound means 30 days back, the same default the overview page card shows. */
    private static final int DEFAULT_WINDOW_DAYS = 30;

    private final UserContextPort userContextPort;
    private final SchoolAiCostQueryRepository schoolAiCostQueryRepository;

    @Override
    @Transactional(readOnly = true)
    public SchoolAiCostTimeseriesResponse execute(ViewSchoolAiCostQuery input) {
        UUID schoolId = userContextPort.getCurrentSchoolId();
        Instant to = input.to() == null ? Instant.now() : input.to();
        Instant from = input.from() == null ? to.minus(DEFAULT_WINDOW_DAYS, ChronoUnit.DAYS) : input.from();

        List<AiCostBucketDto> buckets = schoolAiCostQueryRepository.findBucketedCost(
            schoolId, from, to, input.granularity().sqlUnit());

        // Sum in Java over the ALREADY grouped set (a few dozen rows at most), not with a second SUM query:
        // two separate queries can disagree if an entry is written in between, and then the total at the top
        // of the card would not equal the sum of the columns right below it.
        BigDecimal total = buckets.stream()
            .map(bucket -> bucket.costVnd())
            .reduce(BigDecimal.ZERO, (left, right) -> left.add(right));

        Instant recordedFrom = schoolAiCostQueryRepository.findFirstRecordedAt(schoolId);

        return new SchoolAiCostTimeseriesResponse(
            input.granularity(),
            DecimalText.of(total),
            toPoints(buckets),
            recordedFrom
        );
    }

    private List<SchoolAiCostTimeseriesResponse.AiCostPointResponse> toPoints(List<AiCostBucketDto> buckets) {
        return buckets.stream()
            .map(bucket -> new SchoolAiCostTimeseriesResponse.AiCostPointResponse(
                bucket.bucket(),
                bucket.quotaType(),
                DecimalText.of(bucket.costVnd())))
            .toList();
    }
}
