package com.sep.vox.application.query.dto;

public interface SubAttributeWeaknessInfo {

    String getCriterionCode();

    String getSubAttribute();

    int getOccurrenceCount();

    String getSeverity();

    boolean getPracticeable();

    /** Phần trăm đổi nhịp so với cửa sổ trước; null khi mẫu quá nhỏ hoặc lỗi vừa mới xuất hiện. */
    Double getTrendPercent();
}
