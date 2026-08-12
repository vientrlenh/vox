package com.sep.vox.application.query.dto;

import java.util.UUID;

/**
 * Ba cột mà phép chống trùng theo TÊN của {@code TopicSuggestionService} cần, và chỉ ba cột đó.
 *
 * <p>Tồn tại để thay việc nạp {@code PracticeTopicJpaEntity} đầy đủ: phép so là
 * {@code normalize(name)} bằng nhau hoặc {@code tokenSimilarity(name) >= 0.90}, nên 8 cột còn lại
 * của entity -- kể cả {@code description} kiểu TEXT -- không bao giờ được đọc tới.
 */
public interface TopicNameCardInfo {

    UUID getId();

    String getName();

    String getInterestDimension();
}
