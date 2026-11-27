package com.sep.vox.application.query.repository;

import java.time.Instant;
import java.util.List;

import com.sep.vox.application.query.dto.SessionCostDto;

/**
 * Reads the REAL AI cost grouped by session, for QuotaPricingCalibrationService.
 *
 * <p>Deliberately kept apart from AiUsageRecordRepository even though both read one table: that port serves the WRITE path
 * (recording usage, adding money, stamping as charged) so it speaks the language of the aggregate, while this is a
 * REPORT -- it combines many sessions, corresponds to no entity and has no write-back path. Kept together, the report shape
 * would have to live in domain.repository, and every new statistics screen would add another record to it until nobody could tell
 * which is a business model.
 *
 * <p>See SchoolAiCostQueryRepository / QuestionBankStatsQueryRepository for the same mold.
 */
public interface SessionCostQueryRepository {

    /**
     * The total cost_usd for EACH session with {@code occurred_at >= since}.
     *
     * <p>Not filtered by charged_at: calibration needs the REAL cost incurred to derive a per-second price,
     * and whether it has been charged is an accounting matter unrelated to measuring cost price.
     */
    List<SessionCostDto> sumCostUsdGroupedBySessionSince(Instant since);
}
