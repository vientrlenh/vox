package com.sep.vox.application.query.dto;

import java.util.UUID;

public interface CriterionFrameworkInfo {

    /** Khoá gom nhóm -- từ V13 là framework_criteria.id, trước đó là rubric_criterions.id. */
    UUID getCriterionId();

    /** Mã tiêu chí dùng làm criterionKey gửi xuống Python. */
    String getCriterionCode();


    /** Thang chấm của tiêu chí. Luyện tập luôn 0. */
    double getMinScore();

    /** Luyện tập luôn 100 -- Azure trả HundredMark nên đây là thang gốc. */
    double getMaxScore();

    String getFrameworkCode();

    String getFrameworkName();

    String getFrameworkDescription();

    String getTargetBandId();

    String getTargetBandCode();

    String getTargetBandLabel();

    String getBandCode();

    String getBandLabel();

    int getBandOrder();

    String getDescriptor();
}
