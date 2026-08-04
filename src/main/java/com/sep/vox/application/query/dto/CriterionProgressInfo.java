package com.sep.vox.application.query.dto;

public interface CriterionProgressInfo {

    String getCriterionCode();

    String getObservedDate();

    double getLatentLevel();

    /** EXAM hay PRACTICE. Trước đây mapper gán cứng "EXAM" cho MỌI dòng, kể cả dòng đến từ
     * nhánh practice_criterion_score -- nhãn nguồn sai, mà đây lại đúng là thứ cần phân biệt:
     * đường luyện phẳng hơn đường thi một cách có hệ thống (độ khó bám theo bậc học sinh). */
    String getSource();
}
