package com.sep.vox.infrastructure.persistence.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.TopicSuggestionJpaEntity;

/**
 * Chỉ còn findById/save kế thừa từ JpaRepository.
 *
 * <p>GỠ 2026-08-07: {@code countWeeklyKeywordRequests} -- truy vấn đếm số lượt gõ từ khoá trong
 * tuần lịch, nuôi hạn mức 3 lượt/tuần đã bỏ ở {@code TopicSuggestionService.generateFromKeyword}.
 * Không còn ai gọi. Bảng {@code topic_suggestions} giữ nguyên, nay thuần là nhật ký.
 */
public interface SpringDataTopicSuggestionRepository
        extends JpaRepository<TopicSuggestionJpaEntity, UUID> {

}
