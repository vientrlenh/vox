package com.sep.vox.application.query.dto;

import java.util.UUID;

/**
 * Hai cột duy nhất mà {@code InterestVectorService.recomputeDimensionInterest} cần từ
 * {@code practice_topic}: id để khớp với sự kiện quan tâm, và chiều để cộng điểm.
 *
 * <p>Tồn tại để thay cho việc nạp {@code PracticeTopicJpaEntity} đầy đủ -- entity đó có
 * {@code description} kiểu TEXT cùng 9 cột khác, không cột nào được dùng ở đường này.
 */
public interface TopicDimensionInfo {

    UUID getId();

    String getInterestDimension();
}
