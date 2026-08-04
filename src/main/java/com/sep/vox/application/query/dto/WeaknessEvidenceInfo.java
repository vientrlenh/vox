package com.sep.vox.application.query.dto;

/**
 * Một mẩu bằng chứng cho một nhãn điểm yếu -- chính đoạn học sinh đã nói.
 *
 * Không phải dữ liệu mới: {@code weakness_observation.evidence_span} đã được ghi ngay lúc suy
 * ra nhãn, chỉ là chưa từng có ai đọc nó ra màn hình.
 */
public interface WeaknessEvidenceInfo {

    String getCriterionCode();

    String getSubAttribute();

    String getEvidenceSpan();

    /** Số lần bằng chứng này lặp lại trong cửa sổ. */
    Integer getTimes();
}
