package com.sep.vox.application.query.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Một dòng trên danh sách "trường cần chú ý".
 *
 * @param planName         tên gói của kỳ LIÊN QUAN tới nhóm: kỳ đang phủ nếu trường còn gói, kỳ gần
 *                         nhất nếu không. {@code null} khi gói đã bị xoá khỏi danh mục
 * @param relevantEndDate  ngày hết hạn của chính kỳ đó — với nhóm sắp hết hạn thì đây là con số
 *                         người vận hành đang đếm ngược
 * @param suspendedReason  lý do đình chỉ gần nhất; chỉ có nghĩa ở nhóm {@link SchoolRiskBucket#SUSPENDED},
 *                         và là thông tin đáng giá nhất của nhóm đó
 * @param balanceVnd       số dư ví tự nạp; âm nghĩa là trường đang bị chặn mở ca thi. Không bao giờ
 *                         null — trường chưa từng nạp coi như 0, cùng quy ước với
 *                         {@code SchoolBalance.emptyFor}
 */
public record SchoolAtRiskDto(
    UUID schoolId,
    String schoolName,
    String schoolCode,
    String planName,
    Instant relevantEndDate,
    String suspendedReason,
    BigDecimal balanceVnd
) {
}
