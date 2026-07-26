package com.sep.vox.application.query.dto;

/**
 * Thẻ số màn phúc khảo.
 *
 * @param processing APPROVED + GRADING (trạng thái COMPARING đã bị bỏ)
 * @param withdrawn  đơn học sinh tự rút — tách khỏi {@code rejected} vì đó là quyết
 *                   định của học sinh, không phải của trường
 */
public record AppealStatsInfo(
    int pending,
    int processing,
    int published,
    int rejected,
    int withdrawn
) {
}
