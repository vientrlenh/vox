package com.sep.vox.application.response.input.dashboard;

import java.util.List;

/**
 * Chi phí AI của trường theo mốc thời gian.
 *
 * <p>Tiền là String — nguồn là {@code numeric(18,6)}, cùng lý do đã ghi ở đầu school-balance.graphqls.
 *
 * <p>{@code recordedFrom} là mốc sổ bắt đầu có dữ liệu (V10 không backfill). Trả ra để giao diện
 * phân biệt được "khoảng này trường không tiêu đồng nào" với "khoảng này nằm trước ngày hệ thống bắt
 * đầu ghi sổ" — hai chuyện khác hẳn nhau mà cùng vẽ ra một đường phẳng ở 0.
 */
public record SchoolAiCostTimeseriesResponse(
    String granularity,
    String totalCostVnd,
    List<AiCostPointResponse> points,
    String recordedFrom
) {

    /** Một mốc trên trục thời gian, tách theo loại ví. */
    public record AiCostPointResponse(
        /** ISO-8601, đầu mốc theo giờ Việt Nam. */
        String bucket,
        String quotaType,
        String costVnd
    ) {
    }
}
