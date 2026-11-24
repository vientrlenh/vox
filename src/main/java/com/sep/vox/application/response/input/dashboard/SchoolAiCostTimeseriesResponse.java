package com.sep.vox.application.response.input.dashboard;

import java.time.Instant;
import java.util.List;

import com.sep.vox.application.port.input.query.dashboard.ViewSchoolAiCostQuery.AiCostGranularity;

/**
 * AI cost of the school over time.
 *
 * <p>Money is a String -- the source is {@code numeric(18,6)}, the same reason as written at the top of school-balance.graphqls.
 *
 * <p>{@code recordedFrom} is the moment the ledger started to have data (V10 does not backfill). It is returned so that the
 * UI can tell "the school spent nothing in this range" apart from "this range lies before the system started
 * recording" -- two very different things that draw the same flat line at 0.
 */
public record SchoolAiCostTimeseriesResponse(
    AiCostGranularity granularity,
    String totalCostVnd,
    List<AiCostPointResponse> points,
    Instant recordedFrom
) {

    /** One point on the time axis, split by wallet kind. */
    public record AiCostPointResponse(
        /** Start of the bucket in Vietnam time. */
        Instant bucket,
        String quotaType,
        String costVnd
    ) {
    }
}
